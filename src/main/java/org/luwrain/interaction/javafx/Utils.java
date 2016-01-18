
package org.luwrain.interaction.javafx;

import javafx.application.Platform;
import javafx.scene.paint.Color;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.luwrain.core.InteractionParamColor;

class Utils
{
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
    
    public static <A> A fxcall(Callable<A> task, A onfail)
    {
	FutureTask<A> query=new FutureTask<A>(task){};
	if(Platform.isFxApplicationThread()) 
	{
	    try {
		task.call();
	    } 
	    catch(Exception e) 
	    {
		e.printStackTrace();} return onfail;
	}
	// call from awt thread 
	Platform.runLater(query);
	// waiting for rescan end
	try {return query.get();} catch(InterruptedException|ExecutionException e) {e.printStackTrace();return onfail;}
    }

    public static void fxcall(Callable<Boolean> task)
    {
	FutureTask<Boolean> query=new FutureTask<Boolean>(task){};
	if(Platform.isFxApplicationThread()) 
	{
	    try {
		task.call();
	    } 
	    catch(Exception e) 
	    {
		e.printStackTrace();
	    } 
	    return;
	}
	// call from awt thread 
	Platform.runLater(query);
	// waiting for rescan end
	try {
	    query.get();
	} 
	catch(InterruptedException|ExecutionException e) 
	{
	    e.printStackTrace();
	}
    }
    
    public static void fxnowait(Runnable query)
    {
    	if(Platform.isFxApplicationThread())
    	{
    	    try {
    	    	query.run();
    	    }
    	    catch(Exception e)
    	    {
   	    	e.printStackTrace();
    	    }
    	    return;
    	}
   		Platform.runLater(query);
    }


}
