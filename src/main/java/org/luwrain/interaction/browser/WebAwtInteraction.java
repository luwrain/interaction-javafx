/*
   Copyright 2012-2015 Michael Pozhidaev <msp@altlinux.org>

   This file is part of the Luwrain.

   Luwrain is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   Luwrain is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
 */

package org.luwrain.interaction.browser;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.SwingUtilities;

import org.luwrain.core.events.KeyboardEvent;
import org.luwrain.browser.Browser;
import org.luwrain.core.EventConsumer;
import org.luwrain.core.Interaction;
import org.luwrain.core.InteractionParamColor;
import org.luwrain.core.InteractionParams;
import org.luwrain.core.Log;

@Deprecated public class WebAwtInteraction implements Interaction
{
	private static final int MIN_FONT_SIZE=4;
	private static final String FRAME_TITLE="Luwrain";

	public MainFrame frame;
	private boolean drawingInProgress=false;
	private String fontName=java.awt.Font.MONOSPACED;
	private int currentFontSize=14;

	public EventConsumer eventConsumer;
	boolean leftAltPressed=false;
	boolean rightAltPressed=false;
	boolean controlPressed=false;
	boolean shiftPressed=false;
	
	public static WebAwtInteraction that;
	
	public static Color InteractionParamColorToAWT(InteractionParamColor ipc)
	{
		if(ipc.getPredefined()==null)
			return new Color(ipc.getRed(),ipc.getGreen(),ipc.getBlue());
		switch(ipc.getPredefined())
		{
			case BLACK:		return Color.BLACK;
			case BLUE:		return Color.BLUE;
			case CYAN:		return Color.CYAN;
			case DARK_GRAY:	return Color.DARK_GRAY;
			case GRAY:		return Color.GRAY;
			case GREEN:		return Color.GREEN;
			case LIGHT_GRAY:return Color.LIGHT_GRAY;
			case MAGENTA:	return Color.MAGENTA;
			case ORANGE:	return Color.ORANGE;
			case PINK:		return Color.PINK;
			case RED:		return Color.RED;
			case WHITE:		return Color.WHITE;
			case YELLOW:	return Color.YELLOW;	
			default: 		return new Color(ipc.getRed(),ipc.getGreen(),ipc.getBlue());
		}
	}

	private void onKeyPress(KeyEvent event)
	{
		if((event.getModifiers()&InputEvent.CTRL_MASK)!=0) controlPressed=true;
		if((event.getModifiers()&InputEvent.SHIFT_MASK)!=0) shiftPressed=true;
		if((event.getModifiers()&InputEvent.ALT_MASK)!=0) leftAltPressed=true;
		if((event.getModifiers()&InputEvent.ALT_GRAPH_MASK)!=0) rightAltPressed=true;
		
		that=this;
		//Log.debug("webawt",event.toString());
		if(eventConsumer==null) return;
		int code;
		switch(event.getKeyCode())
		{
		// Functions keys;
			case KeyEvent.VK_F1:
				code=KeyboardEvent.F1;
			break;
			case KeyEvent.VK_F2:
				code=KeyboardEvent.F2;
			break;
			case KeyEvent.VK_F3:
				code=KeyboardEvent.F3;
			break;
			case KeyEvent.VK_F4:
				code=KeyboardEvent.F4;
			break;
			case KeyEvent.VK_F5:
				code=KeyboardEvent.F5;
			break;
			case KeyEvent.VK_F6:
				code=KeyboardEvent.F6;
			break;
			case KeyEvent.VK_F7:
				code=KeyboardEvent.F7;
			break;
			case KeyEvent.VK_F8:
				code=KeyboardEvent.F8;
			break;
			case KeyEvent.VK_F9:
				code=KeyboardEvent.F9;
			break;
			case KeyEvent.VK_F10:
				code=KeyboardEvent.F10;
			break;
			case KeyEvent.VK_F11:
				code=KeyboardEvent.F11;
			break;
			case KeyEvent.VK_F12:
				code=KeyboardEvent.F12;
			break;
			// Arrows;
			case KeyEvent.VK_LEFT:
				code=KeyboardEvent.ARROW_LEFT;
			break;
			case KeyEvent.VK_RIGHT:
				code=KeyboardEvent.ARROW_RIGHT;
			break;
			case KeyEvent.VK_UP:
				code=KeyboardEvent.ARROW_UP;
			break;
			case KeyEvent.VK_DOWN:
				code=KeyboardEvent.ARROW_DOWN;
			break;
			// Jump keys;
			case KeyEvent.VK_HOME:
				code=KeyboardEvent.HOME;
			break;
			case KeyEvent.VK_END:
				code=KeyboardEvent.END;
			break;
			case KeyEvent.VK_INSERT:
				code=KeyboardEvent.INSERT;
			break;
			case KeyEvent.VK_PAGE_DOWN:
				code=KeyboardEvent.PAGE_DOWN;
			break;
			case KeyEvent.VK_PAGE_UP:
				code=KeyboardEvent.PAGE_UP;
			break;
			case KeyEvent.VK_WINDOWS:
				code=KeyboardEvent.WINDOWS;
			break;
			case KeyEvent.VK_CONTEXT_MENU:
				code=KeyboardEvent.CONTEXT_MENU;
			break;
			/*
			// Modifiers;
			case KeyEvent.VK_ALT:
				leftAltPressed=true;
				code=KeyboardEvent.LEFT_ALT;
			break;
			case KeyEvent.VK_ALT_GRAPH:
				rightAltPressed=true;
				code=KeyboardEvent.RIGHT_ALT;
			break;
			case KeyEvent.VK_CONTROL:
				controlPressed=true;
				code=KeyboardEvent.CONTROL;
			break;
			case KeyEvent.VK_SHIFT:
				shiftPressed=true;
				code=KeyboardEvent.SHIFT;
			break;
			*/
			default:
				return;
		}
		final int _code=code;
		SwingUtilities.invokeLater(new Runnable() { @Override public void run()
		{
			if(eventConsumer!=null) eventConsumer
				.enqueueEvent(new KeyboardEvent(true,_code,' ',shiftPressed,controlPressed,leftAltPressed,rightAltPressed));
		}});
	}

	private void onKeyRelease(KeyEvent event)
	{
		if(eventConsumer==null) return;
		switch(event.getKeyCode())
		{
			case KeyEvent.VK_ALT:
				leftAltPressed=false;
				return;
			case KeyEvent.VK_ALT_GRAPH:
				rightAltPressed=false;
				return;
			case KeyEvent.VK_CONTROL:
				controlPressed=false;
				return;
			case KeyEvent.VK_SHIFT:
				shiftPressed=false;
				return;
			default:
				return;
		}
	}

	private void onKeyTyping(KeyEvent event)
	{
		System.out.println(event.toString());
		if(eventConsumer==null) return;
		int code;
		switch(event.getKeyChar())
		{
			case KeyEvent.VK_BACK_SPACE:
				code=KeyboardEvent.BACKSPACE;
			break;
			case KeyEvent.VK_ENTER:
				code=KeyboardEvent.ENTER;
			break;
			case KeyEvent.VK_ESCAPE:
				code=KeyboardEvent.ESCAPE;
			break;
			case KeyEvent.VK_DELETE:
				code=KeyboardEvent.DELETE;
			break;
			case KeyEvent.VK_TAB:
				code=KeyboardEvent.TAB;
			break;
			default:
				if(eventConsumer!=null) eventConsumer.enqueueEvent(new KeyboardEvent(false,0,event.getKeyChar(),shiftPressed,controlPressed,
						leftAltPressed,rightAltPressed));
				return;
		}
		if(eventConsumer!=null) eventConsumer
				.enqueueEvent(new KeyboardEvent(true,code,' ',shiftPressed,controlPressed,leftAltPressed,rightAltPressed));
	}

	public boolean init(InteractionParams params)
	{
		if(params==null) return false;
		if(params.fontName!=null&&!params.fontName.trim().isEmpty()) fontName=params.fontName;
		currentFontSize=params.initialFontSize;
		int wndWidth=params.wndWidth;
		int wndHeight=params.wndHeight;
		Dimension screenSize=Toolkit.getDefaultToolkit().getScreenSize();
		if(wndWidth<0)
		{
			Log.debug("awt","interaction params have window width equal to "+wndWidth+", taking screen width "+screenSize.width);
			wndWidth=screenSize.width+5;// FIXME:+5 eliminates white line in
										// empty X area;
		}
		if(wndHeight<0)
		{
			Log.debug("awt","interaction params have window height equal to "+wndHeight+", taking screen height "+screenSize.height);
			wndHeight=screenSize.height+5;// FIXME:+5 eliminates white line in
											// empty X area;
		}
		Log.info("awt","creating window "+wndWidth+"x"+wndHeight+" at position ("+params.wndLeft+","+params.wndTop+")");
		Log.info("awt","initial font size is "+params.initialFontSize);

		frame=new org.luwrain.interaction.browser.MainFrame("awt");
		frame.setInteractionFont(createFont(currentFontSize));
		frame.setColors(InteractionParamColorToAWT(params.fontColor),InteractionParamColorToAWT(params.bkgColor),InteractionParamColorToAWT(params.splitterColor));
		frame.setMargin(params.marginLeft,params.marginTop,params.marginRight,params.marginBottom);
		frame.setSize(wndWidth,wndHeight);
		frame.setFocusTraversalKeysEnabled(false);
		frame.addKeyListener(new KeyListener()
		{
			public void keyPressed(KeyEvent event){onKeyPress(event);}
			public void keyReleased(KeyEvent event){onKeyRelease(event);}
			public void keyTyped(KeyEvent event){onKeyTyping(event);}
		});
		frame.setVisible(true);
		if(!frame.initTable())
		{
			Log.fatal("awt","error occurred on table initialization");
			return false;
		}
		return true;
	}

	public void close()
	{
		// FIXME:
	}

	public boolean setDesirableFontSize(int fontSize)
	{
		if(fontSize<MIN_FONT_SIZE) return false;
		frame.setInteractionFont(createFont(fontSize));
		if(frame.initTable())
		{
			currentFontSize=fontSize;
			return true;
		}
		frame.setInteractionFont(createFont(currentFontSize));
		return false;
	}

	public int getFontSize()
	{
		return currentFontSize;
	}

	public int getWidthInCharacters()
	{
		return frame.getTableWidth();
	}

	public int getHeightInCharacters()
	{
		return frame.getTableHeight();
	}

	public void startInputEventsAccepting(EventConsumer eventConsumer)
	{
		this.eventConsumer=eventConsumer;
	}

	public void stopInputEventsAccepting()
	{
		eventConsumer=null;
	}

	public void startDrawSession()
	{
		drawingInProgress=true;
	}

	public void drawText(int x,int y,String text)
	{
		if(text==null) return;
		// Log.debug("awt", "text:" + x + "," + y + ":" + text);
		frame.putString(x,y,text);
	}

	public void clearRect(int left,int top,int right,int bottom)
	{
		frame.clearRect(left,top,right,bottom);
	}

	public void endDrawSession()
	{
		drawingInProgress=false;
		frame.paint(frame.getGraphics());
	}

	public void setHotPoint(int x,int y)
	{
		frame.setHotPoint(x,y);
		if(!drawingInProgress) frame.paint(frame.getGraphics());
	}

	public void drawVerticalLine(int top,int bottom,int x)
	{
		// Log.debug("awt", "have vertical line (" + top + "->" + bottom +
		// ") at " + x);
		if(top>bottom)
		{
			Log.warning("awt","very odd vertical line: the top is greater than the bottom, "+top+">"+bottom);
			frame.drawVerticalLine(bottom,top,x);
		} else
			frame.drawVerticalLine(top,bottom,x);
	}

	public void drawHorizontalLine(int left,int right,int y)
	{
		// Log.debug("awt", "have horizontal line (" + left + "->" + right +
		// ") at " + y);
		if(left>right)
		{
			Log.warning("awt","very odd horizontal line: the left is greater than the right, "+left+">"+right);
			frame.drawHorizontalLine(right,left,y);
		} else
			frame.drawHorizontalLine(left,right,y);
	}

	private Font createFont(int desirableFontSize)
	{
		Font f=new Font(fontName,Font.PLAIN,desirableFontSize);
		// Font f = new Font("Dejavu Sans Mono", Font.PLAIN, desirableFontSize);
		return f;
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
		return null;//(Browser)new WebPage(this);
	}
}
