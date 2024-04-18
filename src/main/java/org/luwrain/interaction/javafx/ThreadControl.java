/*
   Copyright 2012-2024 Michael Pozhidaev <msp@luwrain.org>
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

import javafx.application.Application;

final class ThreadControl implements Runnable
{
    static final Object syncObj = new Object();
    static volatile App appObj = null;

    static App waitAppStart()
    {
	synchronized(syncObj) {
	    try {
		while(appObj == null && !Thread.currentThread().interrupted()) 
		    syncObj.wait();
	    } 
	    catch(InterruptedException e)
	    {
		Thread.currentThread().interrupt();
	    }
	}
	return appObj;
    }

    static void appStarted(App obj)
    {
	if (obj == null)
	    return;
	synchronized(syncObj) {
	    appObj = obj;
	    syncObj.notify();
	}
    }

    @Override public void run()
    {
	App.launch(App.class);
	System.exit(0);
    }
} //MainJavafxThread
