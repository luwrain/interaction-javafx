/*
   Copyright 2012-2018 Michael Pozhidaev <michael.pozhidaev@gmail.com>
   Copyright 2015-2016 Roman Volovodov <gr.rPman@gmail.com>

   This file is part of LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.interaction.javafx;

import java.util.*;
import java.util.concurrent.*;

import javafx.stage.Screen;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;

import org.luwrain.core.*;
import org.luwrain.base.*;
import org.luwrain.browser.*;


public final class JavaFxInteraction implements Interaction
{
    static final String LOG_COMPONENT = "javafx";

    static private final int MIN_FONT_SIZE = 4;
    static private final String FRAME_TITLE = "LUWRAIN";

    private org.luwrain.interaction.KeyboardHandler keyboard;
    private boolean drawingInProgress = false;
    private int currentFontSize = 14;
    private String fontName = "Monospaced";
    private MainApp app = null;

    private final List<BrowserImpl> browsers = new Vector();
    private BrowserImpl currentBrowser = null;
    private boolean blockingPaint = false;

    @Override public boolean init(final InteractionParams params,final OperatingSystem os)
    {
	NullCheck.notNull(params, "params");
	if (params.fontName != null && !params.fontName.trim().isEmpty())
	    fontName = params.fontName;
	/*
	 * We have some sort of a hack here, since we are running a JavaFX
	 * application not from the main thread. The following line creates the
	 * thread, which javaFX will consider as its main thread, but the current function 
	 * will continue immediately once JavaFX calls MainApp.start()
	 * (see its last line). If there could be more elegant solution, we would
	 * be happy to use it, but now it's the best what we know about it, and
	 * it works.
	 */
	new Thread(new ThreadControl()).start();
	this.app = ThreadControl.waitAppStart();
	final FutureTask<Boolean> task = new FutureTask(()->{
		this.currentFontSize = params.initialFontSize;
		int wndWidth = params.wndWidth;
		int wndHeight = params.wndHeight;
		app.setInteractionFont(createFont(currentFontSize),createFont2(currentFontSize));
		app.setColors(
			      Utils.InteractionParamColorToFx(params.fontColor),
			      Utils.InteractionParamColorToFx(params.font2Color),
			      Utils.InteractionParamColorToFx(params.bkgColor),
			      Utils.InteractionParamColorToFx(params.splitterColor));
		app.setMargin(params.marginLeft,params.marginTop,params.marginRight,params.marginBottom);
		this.keyboard = os.getCustomKeyboardHandler("javafx");
		app.primary.addEventHandler(KeyEvent.KEY_PRESSED, (event)->keyboard.onKeyPressed(event));
		app.primary.addEventHandler(KeyEvent.KEY_RELEASED, (event)->keyboard.onKeyReleased(event));
		app.primary.addEventHandler(KeyEvent.KEY_TYPED, (event)->keyboard.onKeyTyped(event));
		if(wndWidth < 0 || wndHeight < 0)
		{
		    final Rectangle2D screenSize = Screen.getPrimary().getVisualBounds();
		    app.setUndecoratedSizeAndShow(screenSize.getWidth(),screenSize.getHeight());
		} else
		{
		    app.setSizeAndShow(wndWidth, wndHeight);
		}
		return new Boolean(app.initTable());
	    });
	Platform.runLater(task);
	final Boolean res;
	try
	{
	    res = task.get();
	}
	catch(InterruptedException | ExecutionException e)
	{
	    Log.error(LOG_COMPONENT, "the interaction procedure failed:" + e.getClass().getName() + ":" + e.getMessage());
	    return false;
	}
	if(res == null || !res.booleanValue())
	{
	    Log.error(LOG_COMPONENT, "the interaction procedure failed (there can be more detailed log message)");
	    return false;
	}
	return true;
    }

    @Override public void close()
    {
	// FIXME:
    }

    @Override public void startInputEventsAccepting(EventConsumer eventConsumer)
    {
	NullCheck.notNull(eventConsumer, "eventConsumer");
	keyboard.setEventConsumer(eventConsumer);
    }

    @Override public void stopInputEventsAccepting()
    {
	keyboard.setEventConsumer(null);
    }

    @Override public boolean setDesirableFontSize(int size)
    {
	final Object res = Utils.callInFxThreadSync(()->{
		final Font oldFont = app.getInteractionFont();
		final Font oldFont2 = app.getInteractionFont2();
		final Font probeFont = createFont(size);
		final Font probeFont2 = createFont2(size);
		app.setInteractionFont(probeFont,probeFont2);
		if (!app.initTable())
		{
		    app.setInteractionFont(oldFont, oldFont2);
		    return new Boolean(false);
		}
		currentFontSize = size;
		return new Boolean(true);
	    });
	if (res == null  || !(res instanceof Boolean))
	    return false;
	return ((Boolean)res).booleanValue();
    }

    @Override public int getFontSize()
    {
	return currentFontSize;
    }

    @Override public int getWidthInCharacters()
    {
	return app.getTableWidth();
    }

    @Override public int getHeightInCharacters()
    {
	return app.getTableHeight();
    }

    @Override public void startDrawSession()
    {
	drawingInProgress = true;
    }

    @Override public void clearRect(int left,int top,int right,int bottom)
    {
	app.clearRect(left, top, right, bottom);
    }

    @Override public void drawText(int x, int y, String text)
    {
	NullCheck.notNull(text, "text");
    	drawText(x, y, TextUtils.replaceIsoControlChars(text), false);
    }

    @Override public void drawText(int x, int y, String text, boolean withFont2)
    {
	NullCheck.notNull(text, "text");
	app.putString(x, y, TextUtils.replaceIsoControlChars(text), withFont2);
    }

    @Override public void endDrawSession()
    {
	drawingInProgress = false;
	if (!blockingPaint)
	    Utils.runInFxThreadAsync(()->app.paint());
    }

    @Override public void setHotPoint(final int x,final int y)
    {
	app.setHotPoint(x, y);
	if(!drawingInProgress) 
	    Utils.runInFxThreadAsync(()->app.paint());
    }

    @Override public void drawVerticalLine(int top, int bottom,
int x)
    {
	if(top > bottom)
	{
	    Log.warning(LOG_COMPONENT,"very odd vertical line: the top is greater than the bottom, "+top+">"+bottom);
	    app.drawVerticalLine(bottom, top, x);
	} else
	    app.drawVerticalLine(top, bottom, x);
    }

    @Override public void drawHorizontalLine(int left, int right,
int y)
    {
	if(left > right)
	{
	    Log.warning(LOG_COMPONENT,"very odd horizontal line: the left is greater than the right, "+left+">"+right);
	    app.drawHorizontalLine(right, left, y);
	} else
	    app.drawHorizontalLine(left, right, y);
    }

    private Font createFont(int desirableFontSize)
    {
	final Font res = Font.font(fontName,desirableFontSize);
	return res;
    }

    private Font createFont2(int desirableFontSize)
    {
	final Font res = Font.font(fontName, javafx.scene.text.FontWeight.BOLD, desirableFontSize);
	return res;
    }

    @Override public Browser createBrowser()
    {
	return new BrowserImpl(this);
    }

    // change current page to curPage, if it null, change previous current page to not visible 
    private void setCurrentBrowser(BrowserImpl newCurrentBrowser, boolean visibility)
    {
	if(currentBrowser != null)
	    currentBrowser.setVisibility(false);
	currentBrowser = newCurrentBrowser;
	if(currentBrowser != null)
	    currentBrowser.setVisibility(visibility);
    }

    void registerBrowser(BrowserImpl browser, WebView webView)
    {
	NullCheck.notNull(browser, "browser");
	NullCheck.notNull(webView, "webView");
	InvalidThreadException.checkThread("JavaFxInteraction.registerBrowser()");
	final boolean wasEmpty = browsers.isEmpty();
	browsers.add(browser);
	app.root.getChildren().add(webView);
	if(wasEmpty) 
	    setCurrentBrowser(browser, false);
    }

boolean closeBrowser(BrowserImpl browser)
    {
	NullCheck.notNull(browser, "browser");
	final int pos = browsers.indexOf(browser);
	if (pos < 0)
	    return false;
browsers.remove(this);
//FIXME:choosing another current browser
return true;
    }

    void  blockPaint()
    {
	this.blockingPaint = true;
    }

    void unblockPaint()
    {
	this.blockingPaint = false;
	app.primary.requestFocus();
    }
}
