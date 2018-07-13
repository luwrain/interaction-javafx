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
    //    private final ThreadControl threadControl = new ThreadControl
    final Thread threadfx = new Thread(new ThreadControl());

    final Vector<BrowserImpl> browsers = new Vector();
    private BrowserImpl currentBrowser = null;

    @Override public boolean init(final InteractionParams params,final OperatingSystem os)
    {
	NullCheck.notNull(params, "params");
	if (params.fontName != null && !params.fontName.trim().isEmpty())
	    fontName = params.fontName;
	threadfx.start();
	this.app = ThreadControl.waitAppStart();
	Callable<Boolean> task=new Callable<Boolean>()
	{
	    @Override public Boolean call() throws Exception
	    {
		currentFontSize = params.initialFontSize;
		int wndWidth = params.wndWidth;
		int wndHeight = params.wndHeight;
		app.setInteractionFont(createFont(currentFontSize),createFont2(currentFontSize));
		app.setColors(
				Utils.InteractionParamColorToFx(params.fontColor),
				Utils.InteractionParamColorToFx(params.font2Color),
				Utils.InteractionParamColorToFx(params.bkgColor),
				Utils.InteractionParamColorToFx(params.splitterColor));
		app.setMargin(params.marginLeft,params.marginTop,params.marginRight,params.marginBottom);
		//frame.primary.requestFocus();
		// FIXME: make better OS abstraction, but now we have only two OS types, windows like and other, like *nix
		keyboard = os.getCustomKeyboardHandler("javafx");
		app.primary.addEventHandler(KeyEvent.KEY_PRESSED, (event)->keyboard.onKeyPressed(event));
		app.primary.addEventHandler(KeyEvent.KEY_RELEASED, (event)->keyboard.onKeyReleased(event));
		app.primary.addEventHandler(KeyEvent.KEY_TYPED, (event)->keyboard.onKeyTyped(event));
		if(wndWidth<0||wndHeight<0)
		{
		    // undecorated full visible screen size
		    Rectangle2D screenSize=Screen.getPrimary().getVisualBounds();
		    app.setUndecoratedSizeAndShow(screenSize.getWidth(),screenSize.getHeight());
		} else
		{
		    app.setSizeAndShow(wndWidth,wndHeight);
		}
		return true;
	    }
	};
	FutureTask<Boolean> query=new FutureTask<Boolean>(task){};
	Platform.runLater(query);
	boolean res=false;
	try
	{
	    res=query.get();
	} catch(InterruptedException|ExecutionException e)
	{
	    e.printStackTrace();
	}
	if(!res) 
	    return false;
	if(!app.initTable())
	{
	    Log.fatal("javafx","error occurred on table initialization");
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
	final Font oldFont = app.getInteractionFont();
	final Font oldFont2 = app.getInteractionFont2();
	final Font probeFont = createFont(size);
	final Font probeFont2 = createFont2(size);
	app.setInteractionFont(probeFont,probeFont2);
	if (!app.initTable())
	{
	    app.setInteractionFont(oldFont, oldFont2);
	    return false;
	}
	currentFontSize = size;
	return true;
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

    @Override public void drawText(int x, int y,
				   String text)
    {
	NullCheck.notNull(text, "text");
    	drawText(x, y, TextUtils.replaceIsoControlChars(text), false);
    }

    @Override public void drawText(int x, int y,
				   String text, boolean withFont2)
    {
	NullCheck.notNull(text, "text");
	app.putString(x, y, TextUtils.replaceIsoControlChars(text), withFont2);
    }

    @Override public void endDrawSession()
    {
	drawingInProgress = false;
	Platform.runLater(()->app.paint());
    }

    @Override public void setHotPoint(final int x,final int y)
    {
	app.setHotPoint(x, y);
	if(!drawingInProgress) 
	    Platform.runLater(()->app.paint());
    }

    @Override public void drawVerticalLine(int top, int bottom,
int x)
    {
	if(top > bottom)
	{
	    Log.warning("javafx","very odd vertical line: the top is greater than the bottom, "+top+">"+bottom);
	    app.drawVerticalLine(bottom, top, x);
	} else
	    app.drawVerticalLine(top, bottom, x);
    }

    @Override public void drawHorizontalLine(int left, int right,
int y)
    {
	if(left > right)
	{
	    Log.warning("javafx","very odd horizontal line: the left is greater than the right, "+left+">"+right);
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

    BrowserImpl getCurrentBrowser()
    {
	return currentBrowser;
    }

    // change current page to curPage, if it null, change previous current page to not visible 
    void setCurrentBrowser(BrowserImpl newCurrentBrowser, boolean visibility)
    {
	if(currentBrowser != null)
	    currentBrowser.setVisibility(false);
	currentBrowser = newCurrentBrowser;
	if(currentBrowser != null)
	    currentBrowser.setVisibility(visibility);
    }

    void setCurrentBrowser(BrowserImpl newCurrentBrowser)
    {
setCurrentBrowser(newCurrentBrowser, false);
    }

    void setCurrentBrowserVisibility(boolean enable)
    {
	if(currentBrowser != null)
currentBrowser.setVisibility(enable);
    }

    void addWebViewControl(WebView webView)
    {
	app.root.getChildren().add(webView);
    }

    void disablePaint()
    {
	app.doPaint=false;
    }

    void enablePaint()
    {
	app.primary.requestFocus();
	app.doPaint=true;
    }
}
