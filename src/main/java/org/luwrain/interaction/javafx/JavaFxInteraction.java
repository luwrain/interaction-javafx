
package org.luwrain.interaction.javafx;

import java.util.*;
import java.util.concurrent.*;

import org.luwrain.core.*;
import org.luwrain.os.*;
import org.luwrain.util.*;
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
    static private final int MIN_FONT_SIZE = 4;
    static private final String FRAME_TITLE = "LUWRAIN";

    private KeyboardHandler keyboard;
    private boolean drawingInProgress = false;
    private int currentFontSize = 14;
    private String fontName = "Monospaced";
        private String fontName2 = "Consolas";

    private MainApp frame;
    final Thread threadfx = new Thread(new AppThread());

    final Vector<WebPage> webPages = new Vector<WebPage>();
    private WebPage currentWebPage = null;

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
				Utils.InteractionParamColorToFx(params.bkgColor),
				Utils.InteractionParamColorToFx(params.splitterColor));
		frame.setColors2(
				Utils.InteractionParamColorToFx(params.fontColor2),
				Utils.InteractionParamColorToFx(params.bkgColor2));
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
	final Font probeFont2 = createFont2(size);
	frame.setInteractionFont(probeFont,probeFont2);
	if (!frame.initTable())
	{
	    Log.error("javafx", "table reinitialization with new font size failed, rolling back to previous settings");
	    frame.setInteractionFont(oldFont,oldFont);
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
    	drawText(x,y,text,false);
    }
    @Override public void drawText(int x,int y,String text,boolean font2)
    {
		if(text==null) 
		    return;
		frame.putString(x,y, Str.replaceIsoControlChars(text),font2);
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
    private Font createFont2(int desirableFontSize)
    {
		final Font res=Font.font(fontName2,desirableFontSize);
		Log.debug("javafx", "try to select font2: \""+fontName2+"\" but using font: \"" + res.getName() + "\"");
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
