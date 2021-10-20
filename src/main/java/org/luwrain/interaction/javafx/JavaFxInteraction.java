/*
   Copyright 2012-2021 Michael Pozhidaev <msp@luwrain.org>
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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javafx.application.Platform;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;

import org.luwrain.core.*;
import org.luwrain.util.*;
import org.luwrain.graphical.*;

public final class JavaFxInteraction implements Interaction
{
    static final String LOG_COMPONENT = "fx";
    static private final int MIN_FONT_SIZE = 4;

    private App app = null;
    private org.luwrain.interaction.KeyboardHandler keyboard;
    private boolean drawingInProgress = false;
    private int currentFontSize = 14;
    private String fontName = "Monospaced";
    private boolean graphicalMode = false;

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
	final FutureTask<Boolean> task = new FutureTask<>(()->{
		this.currentFontSize = params.initialFontSize;
		int wndWidth = params.wndWidth;
		int wndHeight = params.wndHeight;
		app.setInteractionFont(createFont(currentFontSize),createFont2(currentFontSize));
		app.setColors(
			      ColorUtils.InteractionParamColorToFx(params.fontColor),
			      ColorUtils.InteractionParamColorToFx(params.font2Color),
			      ColorUtils.InteractionParamColorToFx(params.bkgColor),
			      ColorUtils.InteractionParamColorToFx(params.splitterColor));
		app.setMargin(params.marginLeft,params.marginTop,params.marginRight,params.marginBottom);
		this.keyboard = os.getCustomKeyboardHandler("javafx");
		app.getStage().addEventHandler(KeyEvent.KEY_PRESSED, (event)->{
			if (!graphicalMode)
			    keyboard.onKeyPressed(event);
		    });
		app.getStage().addEventHandler(KeyEvent.KEY_RELEASED, (event)->{
			if (!graphicalMode)
			    keyboard.onKeyReleased(event);
		    });
		app.getStage().addEventHandler(KeyEvent.KEY_TYPED, (event)->{
			if (!graphicalMode)
			    keyboard.onKeyTyped(event);
		    });
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
	final Object res = FxThread.call(()->{
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
	if (!graphicalMode)
	    FxThread.runAsync(()->app.paint());
    }

    @Override public void setHotPoint(final int x,final int y)
    {
	app.setHotPoint(x, y);
	if(!drawingInProgress) 
	    FxThread.runAsync(()->app.paint());
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

    @Override public void showGraphical(GraphicalMode graphicalMode)
    {
	NullCheck.notNull(graphicalMode, "graphicalMode");
	if (this.graphicalMode)
	    throw new IllegalStateException("Already in graphical mode");
	final AtomicReference<RuntimeException> ex = new AtomicReference<>();
	final AtomicReference<javafx.scene.Node> node = new AtomicReference<>();
	FxThread.runSync(()->{
		final Object obj = graphicalMode.getGraphicalObj(()->{
			if (node.get() != null)
			    FxThread.runSync(()->{
				    app.remove(node.get());
				    this.graphicalMode = false;
				});
			throw new IllegalStateException("There is no node of the opened graphical mode");
		    });
		if (obj == null)
		{
		    ex.set(new NullPointerException("getGraphicalObj() returned null"));
		    return;
		}
		if (!(obj instanceof javafx.scene.Node))
		{
		    ex.set(new ClassCastException("getGraphicalObj() returned not an instance of javafx.scene.Node"));
		    return;
		}
		node.set((javafx.scene.Node)obj);
		app.putNew(node.get());
		this.graphicalMode = true;
	    });
	if (ex.get() != null)
	    throw ex.get();
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
}
