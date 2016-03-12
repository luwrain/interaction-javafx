
package org.luwrain.interaction.javafx;

import javafx.application.Application;

class AppThread implements Runnable
{
    static final Object sync=new Object();
    static boolean ready = false;

    static void waitJavaFx()
    {
	synchronized(sync)
	{
	    try {
		while(!ready && !Thread.currentThread().interrupted()) 
		    sync.wait();
	    } 
	    catch(InterruptedException e)
	    {
		Thread.currentThread().interrupt();
	    }
	}
    }

    static void notifyJavaFx()
    {
	synchronized(sync)
	{
	    ready = true;
	    sync.notify();
	}
    }

    @Override public void run()
    {
	MainJavafxApp.launch(MainJavafxApp.class);
	System.exit(2);
    }
} //MainJavafxThread
