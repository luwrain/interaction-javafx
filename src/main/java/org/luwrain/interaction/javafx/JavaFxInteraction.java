package org.luwrain.interaction.javafx;

import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.luwrain.browser.Browser;
import org.luwrain.core.EventConsumer;
import org.luwrain.core.Interaction;
import org.luwrain.core.InteractionParamColor;
import org.luwrain.core.InteractionParams;
import org.luwrain.core.Log;
import org.luwrain.core.events.KeyboardEvent;
import org.luwrain.interaction.browser.WebPage;

import javafx.stage.Screen;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class JavaFxInteraction implements Interaction
{
	public static Color InteractionParamColorToFx(InteractionParamColor ipc)
	{
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
			default: 		return new Color(ipc.getRed(),ipc.getGreen(),ipc.getBlue(),1);
		}
	}

	private static final int MIN_FONT_SIZE = 4;
    private static final String FRAME_TITLE = "Luwrain";
    private boolean drawingInProgress=false;
    private int currentFontSize = 14;
    private String fontName = "Consolas";

    private EventConsumer eventConsumer;

    public boolean leftAltPressed = false;
    public boolean rightAltPressed = false;
    public boolean controlPressed = false;
    public boolean shiftPressed = false;
    
    public MainJavafxApp frame;
    
    // in javafx keyTyped have no information about pressed alphabetic key
    String lastKeyPressed=null;
    
    static <A> A fxcall(Callable<A> task, A onfail)
    {
		FutureTask<A> query=new FutureTask<A>(task){};
		if(Platform.isFxApplicationThread()) {try {task.call();} catch(Exception e) {e.printStackTrace();} return onfail;}
		// call from awt thread 
		Platform.runLater(query);
		// waiting for rescan end
		try {return query.get();} catch(InterruptedException|ExecutionException e) {e.printStackTrace();return onfail;}
    }
    static void fxcall(Callable<Boolean> task)
    {
		FutureTask<Boolean> query=new FutureTask<Boolean>(task){};
		if(Platform.isFxApplicationThread()) {try {task.call();} catch(Exception e) {e.printStackTrace();} return;}
		// call from awt thread 
		Platform.runLater(query);
		// waiting for rescan end
		try {query.get();} catch(InterruptedException|ExecutionException e) {e.printStackTrace();}
    }
    
	class MainJavafxThread implements Runnable
	{
		@Override public void run()
		{
			System.out.println("thread");
			MainJavafxApp.launch(MainJavafxApp.class);
			System.exit(2);
		}
	}
	MainJavafxThread javafx=new MainJavafxThread();
	Thread threadfx=new Thread(javafx);
	
	@Override public boolean init(InteractionParams params)
	{
		if (params == null)
		    return false;
		if (params.fontName != null && !params.fontName.trim().isEmpty())
		    fontName = params.fontName;
		
		threadfx.start();
		// FIXME: replace sleep to wait from Application init
		try{Thread.sleep(1000);}catch(Exception e){}
		frame=MainJavafxApp.that;
		//synchronized(frame.awaiting){try{frame.awaiting.wait();}catch(Exception e) {e.printStackTrace();}}

		Callable<Boolean> task=new Callable<Boolean>()
		{
			@Override public Boolean call() throws Exception
			{
				currentFontSize = params.initialFontSize;
				int wndWidth = params.wndWidth;
				int wndHeight = params.wndHeight;
				Rectangle2D screenSize=Screen.getPrimary().getBounds();
				if(wndWidth<0)
				{
					Log.debug("javafx","interaction params have window width equal to "+wndWidth+", taking screen width "+screenSize.getWidth());
					wndWidth=(int)(screenSize.getWidth()-5);// FIXME:+5 eliminates white line in empty X area;
				}
				if(wndHeight<0)
				{
					Log.debug("javafx","interaction params have window height equal to "+wndHeight+", taking screen height "+screenSize.getHeight());
					wndHeight=(int)(screenSize.getHeight()-5);// FIXME:+5 eliminates white line in empty X area;
				}
				Log.info("javafx","creating window "+wndWidth+"x"+wndHeight+" at position ("+params.wndLeft+","+params.wndTop+")");
				Log.info("javafx","initial font size is "+params.initialFontSize);
				
				frame.setInteractionFont(createFont(currentFontSize));
				frame.setColors(
						InteractionParamColorToFx(params.fontColor),
						InteractionParamColorToFx(params.bkgColor),
						InteractionParamColorToFx(params.splitterColor));
				frame.setMargin(params.marginLeft,params.marginTop,params.marginRight,params.marginBottom);
				frame.setSize(wndWidth,wndHeight);
				//frame.primary.requestFocus();
				
				frame.primary.addEventHandler(KeyEvent.KEY_PRESSED,new EventHandler<KeyEvent>()
				{
					@Override public void handle(KeyEvent event) {onKeyPressed(event);}
				});
				frame.primary.addEventHandler(KeyEvent.KEY_RELEASED,new EventHandler<KeyEvent>(){
					@Override public void handle(KeyEvent event) {onKeyReleased(event);}
				});
				frame.primary.addEventHandler(KeyEvent.KEY_TYPED,new EventHandler<KeyEvent>(){
					@Override public void handle(KeyEvent event) {onKeyTyped(event);}
				});
					
				return true;
			}
		};
		boolean res=fxcall(task,false);
		if(!res) return false;
		
		// FIXME: uggly javafx window resize was not size childs in the moment
		try{Thread.sleep(100);}catch(Exception e){}
		
		// enable window
		task=new Callable<Boolean>()
		{
			@Override public Boolean call() throws Exception
			{
				frame.primary.show();
				return null;
			}};
		fxcall(task);

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
		this.eventConsumer=eventConsumer;
	}

	@Override public void stopInputEventsAccepting()
	{
		this.eventConsumer=null;
	}

	@Override public boolean setDesirableFontSize(int size)
	{
		// TODO Auto-generated method stub
		return false;
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
		if(text==null) return;
		frame.putString(x,y,text);
	}

	@Override public void endDrawSession()
	{
		drawingInProgress=false;
		fxcall(new Callable<Boolean>(){@Override public Boolean call() throws Exception
		{
			frame.paint();
			return true;
		}});
	}

	@Override public void setHotPoint(int x,int y)
	{
		fxcall(new Callable<Boolean>(){@Override public Boolean call() throws Exception
		{
			frame.setHotPoint(x,y);
			if(!drawingInProgress) frame.paint();
			return true;
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
		//List<String> names=Font.getFontNames();
		Font f=new Font(fontName,desirableFontSize);
		// Font f = new Font("Dejavu Sans Mono", Font.PLAIN, desirableFontSize);
		return f;
	}
	
	void onKeyPressed(KeyEvent event)
	{
		controlPressed=event.isControlDown();
		shiftPressed=event.isShiftDown();
		leftAltPressed=event.isAltDown();
		//Log.debug("web","KeyPressed: "+event.getCode().getName()+" "+(event.isControlDown()?"ctrl ":"")+(event.isAltDown()?"alt ":"")+(event.isShiftDown()?"shift ":"")+(event.isMetaDown()?"meta ":""));
		//if(event.) rightAltPressed=true; // todo: make decision about left/right ALT modifiers
		
		if(eventConsumer==null) return;
		int code;
		switch(event.getCode())
		{
		// Functions keys;
			case F1:code=KeyboardEvent.F1;break;
			case F2:code=KeyboardEvent.F2;break;
			case F3:code=KeyboardEvent.F3;break;
			case F4:code=KeyboardEvent.F4;break;
			case F5:code=KeyboardEvent.F5;break;
			case F6:code=KeyboardEvent.F6;break;
			case F7:code=KeyboardEvent.F7;break;
			case F8:code=KeyboardEvent.F8;break;
			case F9:code=KeyboardEvent.F9;break;
			case F10:code=KeyboardEvent.F10;break;
			case F11:code=KeyboardEvent.F11;break;
			case F12:code=KeyboardEvent.F12;break;
			// Arrows;
			case LEFT:code=KeyboardEvent.ARROW_LEFT;break;
			case RIGHT:code=KeyboardEvent.ARROW_RIGHT;break;
			case UP:code=KeyboardEvent.ARROW_UP;break;
			case DOWN:code=KeyboardEvent.ARROW_DOWN;break;
			// Jump keys;
			case HOME:code=KeyboardEvent.HOME;break;
			case END:code=KeyboardEvent.END;break;
			case INSERT:code=KeyboardEvent.INSERT;break;
			case PAGE_DOWN:code=KeyboardEvent.PAGE_DOWN;break;
			case PAGE_UP:code=KeyboardEvent.PAGE_UP;break;
			case WINDOWS:code=KeyboardEvent.WINDOWS;break;
			case CONTEXT_MENU:code=KeyboardEvent.CONTEXT_MENU;break;
			// modificators
			case CONTROL:code=KeyboardEvent.CONTROL;break;
			case SHIFT:code=KeyboardEvent.SHIFT;break;
			case ALT:code=KeyboardEvent.LEFT_ALT;break;
			case ALT_GRAPH:code=KeyboardEvent.RIGHT_ALT;break;
			default:
				String ch=event.getText();
				if((shiftPressed||leftAltPressed||rightAltPressed)&&!ch.isEmpty())
				{
					KeyboardEvent emulated=new KeyboardEvent(false,0,ch.toLowerCase().charAt(0),shiftPressed,controlPressed,leftAltPressed,rightAltPressed);
					eventConsumer.enqueueEvent(emulated);
				}
			return;
		}
		eventConsumer.enqueueEvent(new KeyboardEvent(true,code,' ',shiftPressed,controlPressed,leftAltPressed,rightAltPressed));
	}
	void onKeyReleased(KeyEvent event)
	{
		controlPressed=event.isControlDown();
		shiftPressed=event.isShiftDown();
		leftAltPressed=event.isAltDown();
		//Log.debug("web","KeyReleased: "+event.getCode().getName()+" "+(event.isControlDown()?"ctrl ":"")+(event.isAltDown()?"alt ":"")+(event.isShiftDown()?"shift ":"")+(event.isMetaDown()?"meta ":""));
	}
	void onKeyTyped(KeyEvent event)
	{
		controlPressed=event.isControlDown();
		shiftPressed=event.isShiftDown();
		leftAltPressed=event.isAltDown();
		//Log.debug("web","KeyTyped: "+lastKeyPressed+" "+(event.isControlDown()?"ctrl ":"")+(event.isAltDown()?"alt ":"")+(event.isShiftDown()?"shift ":"")+(event.isMetaDown()?"meta ":"")+event.toString());
		if(eventConsumer==null) return;
		int code;
		String keychar=event.getCharacter();
//System.out.println("tab:"+KeyCode.TAB+", "+event.getCode()+", "+(event.getCode()==KeyCode.TAB?"true":"false"));
			 if(keychar.equals(KeyCode.BACK_SPACE.impl_getChar())) code=KeyboardEvent.BACKSPACE;
		else if(keychar.equals(KeyCode.ENTER.impl_getChar())||keychar.equals("\n")||keychar.equals("\r")) code=KeyboardEvent.ENTER;
		else if(keychar.equals(KeyCode.ESCAPE.impl_getChar())) code=KeyboardEvent.ESCAPE;
		else if(keychar.equals(KeyCode.DELETE.impl_getChar())) code=KeyboardEvent.DELETE;
		else if(keychar.equals(KeyCode.TAB.impl_getChar())) code=KeyboardEvent.TAB;
		else
		{
			// FIXME: javafx characters return as String type we need a char (now return first symbol)
			KeyboardEvent emulated=new KeyboardEvent(false,0,lastKeyPressed==null?event.getCharacter().charAt(0):lastKeyPressed.toLowerCase().charAt(0),shiftPressed,controlPressed,leftAltPressed,rightAltPressed);
			Log.debug("web","emulated: "+emulated.toString());
			eventConsumer.enqueueEvent(emulated);
			return;
		}
		final int _code=code;
		eventConsumer.enqueueEvent(new KeyboardEvent(true,code,' ',shiftPressed,controlPressed,leftAltPressed,rightAltPressed));
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////
	// list of current open web pages, each one make own WebEngine and WebView
	public Vector<WebPage> webPages=new Vector<WebPage>();
	// current visible (or not) WebPage, can be null any time
	private WebPage currentWebPage=null;
	public WebPage getCurPage(){return currentWebPage;}

	// change current page to curPage, if it null, change previous current page to not visible 
	public void setCurPage(WebPage curPage,boolean visibility)
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
	public void setCurPage(WebPage curPage)
	{
		setCurPage(curPage,false);
	}
	
	public void setCurPageVisibility(boolean enable)
	{
		if(currentWebPage!=null)
		{
			currentWebPage.setVisibility(enable);
		} else
		{
			// todo: make warning to log about no current web page
		}
	}

	@Override public Browser createBrowser()
	{
		return (Browser)new WebPage(this);
	}

}