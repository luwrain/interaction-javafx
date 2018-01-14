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

import javafx.application.Platform;
import javafx.scene.paint.Color;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.luwrain.core.*;
import org.luwrain.base.InteractionParamColor;

class Utils
{
    static private final String LOG_COMPONENT = JavaFxInteraction.LOG_COMPONENT;

    static Object callInFxThreadSync(Callable callable)
    {
	NullCheck.notNull(callable, "callable");
	if(Platform.isFxApplicationThread())
	    try {
	    	return callable.call();
	    }
	    catch(Throwable e) 
	    {
		Log.error(LOG_COMPONENT, "callable object thrown an exception:" + e.getClass().getName() + ":" + e.getMessage());
	    	return null;
	    }
	final FutureTask<Object> query=new FutureTask<Object>(callable);
	Platform.runLater(query);
	try {
	    return query.get();
	}
	catch(InterruptedException e)
	{
	    Thread.currentThread().interrupt();
	    return null;
	}
	catch(ExecutionException e) 
	{
	    Log.error(LOG_COMPONENT, "execution exception on callable object processing:" + e.getClass().getName() + ":" + e.getMessage());
	    return null;
	}
    }

    static void runInFxThreadSync(Runnable runnable)
    {
	NullCheck.notNull(runnable, "runnable");
	if(Platform.isFxApplicationThread())
	    try {
		runnable.run();
	    	return;
	    }
	    catch(Throwable e) 
	    {
		Log.error(LOG_COMPONENT, "runnable object thrown an exception:" + e.getClass().getName() + ":" + e.getMessage());
	    	return;
	    }
	final FutureTask<Object> query=new FutureTask<Object>(runnable, null);
	Platform.runLater(query);
	try {
	    query.get();
	    return;
	}
	catch(InterruptedException e)
	{
	    Thread.currentThread().interrupt();
	    return;
	}
	catch(ExecutionException e) 
	{
	    Log.error(LOG_COMPONENT, "execution exception on runnable object processing:" + e.getClass().getName() + ":" + e.getMessage());
	    return;
	}
    }

    static void runInFxThreadAsync(Runnable runnable)
    {
	NullCheck.notNull(runnable, "runnable");
	if(Platform.isFxApplicationThread())
	    try {
		runnable.run();
		return;
	    }
	    catch(Throwable e) 
	    {
		Log.error(LOG_COMPONENT, "runnable object thrown an exception:" + e.getClass().getName() + ":" + e.getMessage());
	    	return;
	    }
	final FutureTask<Object> query=new FutureTask<Object>(runnable, null);
	Platform.runLater(query);
	return;
    }

    static Color InteractionParamColorToFx(InteractionParamColor ipc)
    {
	if(ipc.getPredefined()==null)
	    return new Color(ipc.getRed()/256,ipc.getGreen()/256,ipc.getBlue()/256,1);
	switch(ipc.getPredefined())
	{
	case BLACK:		return Color.BLACK;
	case BLUE:		return Color.BLUE;
	case CYAN:		return Color.CYAN;
	case DARK_GRAY:	return Color.DARKGRAY;
	case GRAY:		return Color.GRAY;
	case GREEN:		return Color.GREEN;
	case LIGHT_GRAY:return Color.LIGHTGRAY;
	case MAGENTA:	return Color.MAGENTA;
	case ORANGE:	return Color.ORANGE;
	case PINK:		return Color.PINK;
	case RED:		return Color.RED;
	case WHITE:		return Color.WHITE;
	case YELLOW:	return Color.YELLOW;	
	    // WARN: not predefined colors have opacity = 1
	default: 		return new Color(ipc.getRed()/256,ipc.getGreen()/256,ipc.getBlue()/256,1);
	}
    }
}
