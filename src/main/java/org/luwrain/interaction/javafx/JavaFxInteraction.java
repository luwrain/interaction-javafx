
package org.luwrain.interaction.javafx;

import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.stage.Screen;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;

import org.luwrain.browser.Browser;
import org.luwrain.core.*;
import org.luwrain.os.*;
import org.luwrain.util.Str;

public class JavaFxInteraction implements Interaction
{
    private static final int MIN_FONT_SIZE = 4;
    private static final String FRAME_TITLE = "LUWRAIN";
    private boolean drawingInProgress=false;
    private int currentFontSize = 14;
    private String fontName = "Consolas";
    final Vector<WebPage> webPages=new Vector<WebPage>();
    private WebPage currentWebPage=null;
    private KeyboardHandler keyboard;
    private MainJavafxApp frame;


    static class MainJavafxThread implements Runnable
    {
	static Object sync=new Object();
	static boolean ready=false;

	static void waitJavaFx()
	{
	    synchronized(sync)
	    {
		try {
		    while(!ready) 
			sync.wait();
		} 
		catch(InterruptedException e)
		{
		    // TODO: make better error handling
		    e.printStackTrace();
		}
	    }
	}

	public static void notifyJavaFx()
	{
	    synchronized(sync)
	    {
		ready=true;sync.notify();
	    }
	}

	@Override public void run()
	{
	    MainJavafxApp.launch(MainJavafxApp.class);
	    // closed via Alt+F4 or any other window based task killer
	    System.exit(2);
	}
    } //MainJavafxThread

    final Thread threadfx=new Thread(new MainJavafxThread());

    @Override public boolean init(final InteractionParams params,final OperatingSystem os)
    {
	NullCheck.notNull(params, "params");
	if (params.fontName != null && !params.fontName.trim().isEmpty())
	    fontName = params.fontName;
	threadfx.start();
	// wait for thread starts and finished javafx init
	MainJavafxThread.waitJavaFx();
	frame=MainJavafxApp.getClassObject();

	Callable<Boolean> task=new Callable<Boolean>()
	{
	    @Override public Boolean call() throws Exception
	    {
		currentFontSize = params.initialFontSize;
		int wndWidth = params.wndWidth;
		int wndHeight = params.wndHeight;
		frame.setInteractionFont(createFont(currentFontSize));
		frame.setColors(
				Utils.InteractionParamColorToFx(params.fontColor),
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
		    Log.debug("javafx", "Undecorated mode, visible screen size: "+screenSize.getWidth()+"x"+screenSize.getHeight());
		    frame.setUndecoratedSizeAndShow(screenSize.getWidth(),screenSize.getHeight());
		} else
		{
		    Log.debug("javafx", "Typical window mode, size:"+wndWidth+"x"+wndHeight);
		    frame.setSizeAndShow(wndWidth,wndHeight);
		}
		return true;
	    }
	};

	boolean res=Utils.fxcall(task,false);
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
	keyboard.setEventConsumer(eventConsumer);
    }

    @Override public void stopInputEventsAccepting()
    {
    keyboard.setEventConsumer(null);
    }

    @Override public boolean setDesirableFontSize(int size)
    {
	Log.debug("javafx", "trying to change font size to " + size);
	final Font oldFont = frame.getInteractionFont();
	final Font probeFont = createFont(size);
	frame.setInteractionFont(probeFont);
	if (!frame.initTable())
	{
	    Log.error("javafx", "table reinitialization with new font size failed, rolling back to previous settings");
	    frame.setInteractionFont(oldFont);
	    return false;
	}
	Log.debug("javafx", "the table said new size is OK, saving new settings");
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
	drawingInProgress=true;
    }

    @Override public void clearRect(int left,int top,int right,int bottom)
    {
	frame.clearRect(left,top,right,bottom);
    }

    @Override public void drawText(int x,int y,String text)
    {
		if(text==null) 
		    return;
		frame.putString(x,y, Str.replaceIsoControlChars(text));
    }

    @Override public void endDrawSession()
    {
	drawingInProgress=false;
		Platform.runLater(new Runnable(){
			@Override public void run()
			{
			    frame.paint();
			}});
    }

    @Override public void setHotPoint(final int x,final int y)
    {
			frame.setHotPoint(x,y);
			Platform.runLater(new Runnable(){
				@Override public void run()
				{
				    if(!drawingInProgress) 
					frame.paint();
				}});
    }

    @Override public void drawVerticalLine(int top,int bottom,int x)
    {
	if(top>bottom)
	{
	    Log.warning("javafx","very odd vertical line: the top is greater than the bottom, "+top+">"+bottom);
	    frame.drawVerticalLine(bottom,top,x);
	} else
	    frame.drawVerticalLine(top,bottom,x);
    }

    @Override public void drawHorizontalLine(int left,int right,int y)
    {
	if(left>right)
	{
	    Log.warning("javafx","very odd horizontal line: the left is greater than the right, "+left+">"+right);
	    frame.drawHorizontalLine(right,left,y);
	} else
	    frame.drawHorizontalLine(left,right,y);
    }

    private Font createFont(int desirableFontSize)
    {
		final Font res=Font.font(fontName,desirableFontSize);
		Log.debug("javafx", "try to select font: \""+fontName+"\" but using font: \"" + res.getName() + "\"");
		return res;
    }

    @Override public Browser createBrowser()
    {
	return new WebPage(this);
    }


WebPage getCurPage()
{
return currentWebPage;
}

    // change current page to curPage, if it null, change previous current page to not visible 
void setCurPage(WebPage curPage,boolean visibility)
    {
	if(currentWebPage!=null)
	{ // change visibility current page to off
	    currentWebPage.setVisibility(false);
	}
	currentWebPage = curPage;
	if(curPage==null)
	{
	} else
	{
	    currentWebPage.setVisibility(visibility);
	}
    }

    void setCurPage(WebPage curPage)
    {
	setCurPage(curPage,false);
    }

    void setCurPageVisibility(boolean enable)
    {
	if(currentWebPage!=null)
	{
	    currentWebPage.setVisibility(enable);
	} else
	{
	    // todo: make warning to log about no current web page
	}
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
