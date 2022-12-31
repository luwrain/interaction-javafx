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

package org.luwrain.graphical;

import java.util.concurrent.*;
import javafx.application.Platform;

import org.luwrain.core.*;

public final class FxThread
{
    static private final String LOG_COMPONENT = "fx";

    static public void ensure()
    {
		if(!Platform.isFxApplicationThread())
		    throw new IllegalStateException("Execution in non-jfx thread");
    }

    static public Object call(Callable<Object> callable)
    {
	NullCheck.notNull(callable, "callable");
	if(Platform.isFxApplicationThread())
	    try {
	    	return callable.call();
	    }
	    catch(Throwable e) 
	    {
		Log.error(LOG_COMPONENT, "callable object thrown an exception: " + e.getClass().getName() + ": " + e.getMessage());
		throw new RuntimeException(e);
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
	    Log.error(LOG_COMPONENT, "execution exception during processing of the callable object: " + e.getClass().getName() + ": " + e.getMessage());
	    throw new RuntimeException(e);
	}
    }

    static public void runSync(Runnable runnable)
    {
	NullCheck.notNull(runnable, "runnable");
	if(Platform.isFxApplicationThread())
	{
		runnable.run();
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
	    throw new RuntimeException(e);
	}
    }

    static public void runAsync(Runnable runnable)
    {
	NullCheck.notNull(runnable, "runnable");
	if(Platform.isFxApplicationThread())
	    try {
		runnable.run();
		return;
	    }
	    catch(Throwable e) 
	    {
		Log.error(LOG_COMPONENT, "runnable object has thrown an exception:" + e.getClass().getName() + ":" + e.getMessage());
		throw new RuntimeException(e);
	    }
	final FutureTask<Object> query=new FutureTask<Object>(runnable, null);
	Platform.runLater(query);
	return;
    }
}
