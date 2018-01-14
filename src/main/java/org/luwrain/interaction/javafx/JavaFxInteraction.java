/*
   Copyright 2012-2017 Michael Pozhidaev <michael.pozhidaev@gmail.com>
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

import org.luwrain.core.*;
import org.luwrain.base.*;
//import org.luwrain.util.*;
import org.luwrain.browser.*;

import javafx.stage.Screen;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;

public class JavaFxInteraction implements Interaction
{
    static final String LOG_COMPONENT = "javafx";

    static private final int MIN_FONT_SIZE = 4;
    static private final String FRAME_TITLE = "LUWRAIN";

    private org.luwrain.interaction.KeyboardHandler keyboard;
    private boolean drawingInProgress = false;
    private int currentFontSize = 14;
    private String fontName = "Monospaced";

    private MainApp frame;
    final Thread threadfx = new Thread(new AppThread());

    final Vector<BrowserImpl> browsers = new Vector<BrowserImpl>();
    private BrowserImpl currentBrowser = null;

    @Override public boolean init(final InteractionParams params,final OperatingSystem os)
    {
	NullCheck.notNull(params, "params");
	if (params.fontName != null && !params.fontName.trim().isEmpty())
	    fontName = params.fontName;
	threadfx.start();
	// wait for thread starts and finished javafx init
	AppThread.waitJavaFx();
	frame = MainApp.getClassObject();
	Callable<Boolean> task=new Callable<Boolean>()
	{
	    @Override public Boolean call() throws Exception
	    {
		currentFontSize = params.initialFontSize;
		int wndWidth = params.wndWidth;
		int wndHeight = params.wndHeight;
		frame.setInteractionFont(createFont(currentFontSize),createFont2(currentFontSize));
		frame.setColors(
				Utils.InteractionParamColorToFx(params.fontColor),
				Utils.InteractionParamColorToFx(params.font2Color),
				Utils.InteractionParamColorToFx(params.bkgColor),
				Utils.InteractionParamColorToFx(params.splitterColor));
		frame.setMargin(params.marginLeft,params.marginTop,params.marginRight,params.marginBottom);
		//frame.primary.requestFocus();
		// FIXME: make better OS abstraction, but now we have only two OS types, windows like and other, like *nix
		keyboard = os.getCustomKeyboardHandler("javafx");
		frame.primary.addEventHandler(KeyEvent.KEY_PRESSED, (event)->keyboard.onKeyPressed(event));
		frame.primary.addEventHandler(KeyEvent.KEY_RELEASED, (event)->keyboard.onKeyReleased(event));
		frame.primary.addEventHandler(KeyEvent.KEY_TYPED, (event)->keyboard.onKeyTyped(event));
		if(wndWidth<0||wndHeight<0)
		{
		    // undecorated full visible screen size
		    Rectangle2D screenSize=Screen.getPrimary().getVisualBounds();
		    frame.setUndecoratedSizeAndShow(screenSize.getWidth(),screenSize.getHeight());
		} else
		{
		    frame.setSizeAndShow(wndWidth,wndHeight);
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
	if(!frame.initTable())
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
	final Font oldFont = frame.getInteractionFont();
	final Font oldFont2 = frame.getInteractionFont2();
	final Font probeFont = createFont(size);
	final Font probeFont2 = createFont2(size);
	frame.setInteractionFont(probeFont,probeFont2);
	if (!frame.initTable())
	{
	    frame.setInteractionFont(oldFont, oldFont2);
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
	return frame.getTableWidth();
    }

    @Override public int getHeightInCharacters()
    {
	return frame.getTableHeight();
    }

    @Override public void startDrawSession()
    {
	drawingInProgress = true;
    }

    @Override public void clearRect(int left,int top,int right,int bottom)
    {
	frame.clearRect(left, top, right, bottom);
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
	frame.putString(x, y, TextUtils.replaceIsoControlChars(text), withFont2);
    }

    @Override public void endDrawSession()
    {
	drawingInProgress = false;
	Platform.runLater(()->frame.paint());
    }

    @Override public void setHotPoint(final int x,final int y)
    {
	frame.setHotPoint(x, y);
	if(!drawingInProgress) 
	    Platform.runLater(()->frame.paint());
    }

    @Override public void drawVerticalLine(int top, int bottom,
int x)
    {
	if(top > bottom)
	{
	    Log.warning("javafx","very odd vertical line: the top is greater than the bottom, "+top+">"+bottom);
	    frame.drawVerticalLine(bottom, top, x);
	} else
	    frame.drawVerticalLine(top, bottom, x);
    }

    @Override public void drawHorizontalLine(int left, int right,
int y)
    {
	if(left > right)
	{
	    Log.warning("javafx","very odd horizontal line: the left is greater than the right, "+left+">"+right);
	    frame.drawHorizontalLine(right, left, y);
	} else
	    frame.drawHorizontalLine(left, right, y);
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
	frame.root.getChildren().add(webView);
    }

    void disablePaint()
    {
	frame.doPaint=false;
    }

    void enablePaint()
    {
	frame.primary.requestFocus();
	frame.doPaint=true;
    }
}
