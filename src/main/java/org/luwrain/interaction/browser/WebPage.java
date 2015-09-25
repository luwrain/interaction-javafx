package org.luwrain.interaction.browser;

import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import netscape.javascript.JSObject;

import org.luwrain.browser.Browser;
import org.luwrain.browser.BrowserEvents;
import org.luwrain.browser.ElementList;
import org.luwrain.core.Interaction;
import org.luwrain.core.Log;
import org.luwrain.core.events.KeyboardEvent;
import org.luwrain.interaction.javafx.JavaFxInteraction;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.views.DocumentView;

import com.sun.webkit.dom.DOMWindowImpl;

public class WebPage implements Browser
{
	private JavaFxInteraction wi;
	
	public WebView webView;
	public WebEngine webEngine;
	
	//public JFXPanel jfx=new JFXPanel();
	
	// used to save DOM structure with RescanDOM
	public static class NodeInfo
	{
		public org.w3c.dom.Node node;
		public Rectangle rect;
		public boolean forTEXT;
		public boolean isVisible(){return rect.width>0&&rect.height>0;}
	}
	// list of all nodes in web page
	Vector<NodeInfo> dom=new Vector<NodeInfo>();
	// index map for fast get node position
	LinkedHashMap<org.w3c.dom.Node,Integer> domIdx=new LinkedHashMap<org.w3c.dom.Node, Integer>();

	public HTMLDocument htmlDoc=null;
	public DOMWindowImpl htmlWnd=null;
	
	public JSObject window=null;
	
	private boolean userStops=false;
	
	@Override public String getBrowserTitle()
	{
		return "ВебБраузер";
	}
	@Override public Browser setInteraction(Interaction interactiion)
	{
		wi=(JavaFxInteraction)interactiion;
		return null;
	}
	
	public WebPage(JavaFxInteraction interactiion)
	{
		wi=interactiion;
	}
	// make new empty WebPage (like about:blank) and add it to WebEngineInteraction's webPages
	public void init(final BrowserEvents events)
	{
		final WebPage that=this;
		final boolean emptyList=wi.webPages.isEmpty();
		wi.webPages.add(this);

		Platform.runLater(new Runnable()
		{
			@Override public void run()
			{
				webView=new WebView();
				webEngine=webView.getEngine();

				webView.setOnKeyReleased(new EventHandler<KeyEvent>()
				{
					@Override public void handle(KeyEvent event)
					{
						Log.debug("web","KeyReleased: "+event.toString());
						switch(event.getCode())
						{
							case ESCAPE:wi.setCurPageVisibility(false);break;
							default:break;
						}
						
					}
				});
				/*
				webView.setOnKeyReleased(new EventHandler<KeyEvent>()
				{
					@Override public void handle(KeyEvent event)
					{
						Log.debug("web","KeyReleased: "+event.toString());
						switch(event.getCode())
						{
							case ALT:wi.leftAltPressed=false;break;
							case ALT_GRAPH:wi.rightAltPressed=false;break;
							case CONTROL:wi.controlPressed=false;break;
							case SHIFT:wi.shiftPressed=false;break;
							default: break;
						}
					}
				});
				webView.setOnKeyTyped(new EventHandler<KeyEvent>()
				{
					@Override public void handle(final KeyEvent event)
					{
						Log.debug("web","KeyTyped: "+event.toString());
						if(wi.eventConsumer==null) return;
						int code;
						switch(event.getCode())
						{
							case BACK_SPACE:code=KeyboardEvent.BACKSPACE;break;
							case ENTER:code=KeyboardEvent.ENTER;break;
							case ESCAPE:code=KeyboardEvent.ESCAPE;break;
							case DELETE:code=KeyboardEvent.DELETE;break;
							case TAB:code=KeyboardEvent.TAB;break;
							default:
								// FIXME: javafx characters return as String type we need a char (now return first symbol)
								SwingUtilities.invokeLater(new Runnable() { @Override public void run()
								{
									wi.eventConsumer.enqueueEvent(
											new KeyboardEvent(false,0,event.getCharacter().charAt(0),wi.shiftPressed,wi.controlPressed,wi.leftAltPressed,wi.rightAltPressed));
								}});
							return;
						}
						final int _code=code;
						SwingUtilities.invokeLater(new Runnable() { @Override public void run()
						{
							wi.eventConsumer.enqueueEvent(
									new KeyboardEvent(true,_code,' ',wi.shiftPressed,wi.controlPressed,wi.leftAltPressed,wi.rightAltPressed));
						}});
					}
				});
				webView.setOnKeyPressed(new EventHandler<KeyEvent>()
				{
					@Override public void handle(final KeyEvent event)
					{
						Log.debug("web","KeyPressed: "+event.toString());
						if(wi.eventConsumer==null) return;
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
							// Modifiers;
							case ALT:
								wi.leftAltPressed=true;
								code=KeyboardEvent.LEFT_ALT;
							break;
							case ALT_GRAPH:
								wi.rightAltPressed=true;
								code=KeyboardEvent.RIGHT_ALT;
							break;
							case CONTROL:
								wi.controlPressed=true;
								code=KeyboardEvent.CONTROL;
							break;
							case SHIFT:
								wi.shiftPressed=true;
								code=KeyboardEvent.SHIFT;
							break;
							default:
								return;
						}
						// todo: make tests for alt/ctrl/shift modifiers work, while web page changed its visibility
						final int _code=code;
						SwingUtilities.invokeLater(new Runnable() { @Override public void run()
						{
							wi.eventConsumer.enqueueEvent(
									new KeyboardEvent(true,_code,' ',wi.shiftPressed,wi.controlPressed,wi.leftAltPressed,wi.rightAltPressed));
						}});
					}
				});
				*/
				webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>()
				{
					@Override public void changed(ObservableValue<? extends State> ov,State oldState,final State newState)
					{
						Log.debug("web","State changed to: "+newState.name()+", "+webEngine.getLoadWorker().getState().toString()+", url:"+webEngine.getLocation());
						SwingUtilities.invokeLater(new Runnable() { @Override public void run()
						{
							if(newState==State.CANCELLED)
							{ // if canceled not by user, so that is a file downloads
								if(!userStops)
								{ // if it not by user
									if(events.onDownloadStart(webEngine.getLocation())) return;
								}
							}
							events.onChangeState(newState);
						}});
					}
				});
				webEngine.getLoadWorker().progressProperty().addListener(new ChangeListener<Number>()
				{
					@Override public void changed(ObservableValue<? extends Number> ov,Number o,final Number n)
					{
						//Log.debug("web","progress: from "+o+" to "+n);
						SwingUtilities.invokeLater(new Runnable() { @Override public void run()
						{
							events.onProgress(n);
						}});
					}
				});
				webEngine.setOnAlert(new EventHandler<WebEvent<String>>()
				{
					@Override public void handle(final WebEvent<String> event)
					{
						Log.debug("web","ALERT: "+event.getData());
						SwingUtilities.invokeLater(new Runnable() { @Override public void run()
						{
							events.onAlert(event.getData());
						}});
					}
				});
				webEngine.setPromptHandler(new Callback<PromptData,String>()
				{
					@Override public String call(final PromptData event)
					{
						Log.debug("web","PROMPT: '"+event.getMessage()+"', default '"+event.getDefaultValue()+"'");
						FutureTask<String> query=new FutureTask<String>(new Callable<String>(){
							@Override public String call() throws Exception
							{
								return events.onPrompt(event.getMessage(),event.getDefaultValue());
							}
						});
						SwingUtilities.invokeLater(query);
						// FIXME: make error handling better
						try {return query.get();} catch(InterruptedException|ExecutionException e)
						{e.printStackTrace();return null;}
					}
				});
				webEngine.setOnError(new EventHandler<WebErrorEvent>()
				{
					@Override public void handle(final WebErrorEvent event)
					{
						Log.debug("web","ERROR: type="+event.getEventType().getName()+", '"+event.getMessage()+"'");
						SwingUtilities.invokeLater(new Runnable() { @Override public void run()
						{
							events.onError(event.getMessage());
						}});
					}
				});

				//Scene scene = new Scene(webView);
				//jfx.setVisible(false);
				webView.setVisible(false);
				//jfx.setScene(scene);
				wi.frame.root.getChildren().add(webView);
				
				if(emptyList) wi.setCurPage(that);
			}
		});
	}
	// remove WebPage from WebEngineInteraction's webPages list and stop WebEngine work, prepare for destroy
	@Override public void Remove()
	{
		int pos=wi.webPages.indexOf(this);
		boolean success=wi.webPages.remove(this);
		if(!success) Log.warning("web","Can't found WebPage to remove it from WebEngineInteraction");
		setVisibility(false);
		if(pos!=-1)
		{
			if(pos<wi.webPages.size())
			{
				wi.setCurPage(wi.webPages.get(pos));
			}
		} else
		{
			if(wi.webPages.isEmpty()) wi.setCurPage(null);
			else wi.setCurPage(wi.webPages.lastElement());
		}
	}
	
	@Override public void setVisibility(boolean enable)
	{
		if(enable)
		{ // set visibility for this webpage on and change focus to it later (text page visibility is off)
			webView.setVisible(true);
			wi.frame.doPaint=false;
			Platform.runLater(new Runnable(){@Override public void run()
			{
				Log.debug("web","request focus "+webView);
				webView.requestFocus();
			}});
		} else
		{ // set text page visibility to on and current webpage to off
			//wi.frame.setVisible(true);
			wi.frame.primary.requestFocus();
			wi.frame.doPaint=true;
			webView.setVisible(false);
		}
	}
	@Override public boolean getVisibility()
	{
		return webView.isVisible();
	}
	
	// rescan current page DOM model and refill list of nodes with it bounded rectangles
	@Override public void RescanDOM()
	{
		Callable<Integer> task=new Callable<Integer>(){
		@Override public Integer call() throws Exception
		{
			htmlDoc = (HTMLDocument)webEngine.getDocument();
			htmlWnd =(DOMWindowImpl)((DocumentView)htmlDoc).getDefaultView();
			//
			dom=new Vector<WebPage.NodeInfo>();
			domIdx=new LinkedHashMap<org.w3c.dom.Node, Integer>();
			JSObject js=(JSObject)webEngine.executeScript("(function(){function nodewalk(node){var res=[];if(node){node=node.firstChild;while(node!= null){if(node.nodeType!=3||node.nodeValue.trim()!=='') res[res.length]=node;res=res.concat(nodewalk(node));node=node.nextSibling;}}return res;};var lst=nodewalk(document);var res=[];for(var i=0;i<lst.length;i++){res.push({n:lst[i],r:(lst[i].getBoundingClientRect?lst[i].getBoundingClientRect():(lst[i].parentNode.getBoundingClientRect?lst[i].parentNode.getBoundingClientRect():null))});};return res;})()");;
			Object o;
			for(int i=0;!(o=js.getMember(String.valueOf(i))).getClass().equals(String.class);i++)
			{
				JSObject rect=(JSObject)((JSObject)o).getMember("r");
				org.w3c.dom.Node n=(org.w3c.dom.Node)((JSObject)o).getMember("n");
				NodeInfo info=new NodeInfo();
				if(rect==null)
				{
					info.rect=new Rectangle(0,0,0,0);
				} else
				{
					int x=(int)Double.parseDouble(rect.getMember("left").toString());
					int y=(int)Double.parseDouble(rect.getMember("top").toString());
					int width=(int)Double.parseDouble(rect.getMember("width").toString());
					int height=(int)Double.parseDouble(rect.getMember("height").toString());
					info.rect=new Rectangle(x,y,width,height);
				}
				info.node=n;
				// by default, only leap nodes good for TEXT 
				info.forTEXT=!n.hasChildNodes();
				// make decision about TEXT nodes by class
				if(n.getClass()==com.sun.webkit.dom.HTMLAnchorElementImpl.class
				 ||n.getClass()==com.sun.webkit.dom.HTMLButtonElementImpl.class
				 ||n.getClass()==com.sun.webkit.dom.HTMLInputElementImpl.class
				 //||n.getClass()==com.sun.webkit.dom.HTMLPreElementImpl.class
				 ||n.getClass()==com.sun.webkit.dom.HTMLSelectElementImpl.class
				 ||n.getClass()==com.sun.webkit.dom.HTMLTextAreaElementImpl.class
				) info.forTEXT=true;
				domIdx.put(n, i);
				dom.add(info);
				//
			}
			// reselect window object (for example if page was reloaded)
			window=(JSObject)webEngine.executeScript("window");
			return null;
		}};
		FutureTask<Integer> query=new FutureTask<Integer>(task){};
		if(Platform.isFxApplicationThread()) {try {task.call();} catch(Exception e) {e.printStackTrace();} return;}
		// call from awt thread 
		Platform.runLater(query);
		// waiting for rescan end
		try {query.get();} catch(InterruptedException|ExecutionException e) {e.printStackTrace();}
	}
	
	// start loading page via link
	@Override public void load(final String link)
	{
		Platform.runLater(new Runnable()
		{
			@Override public void run()
			{
				webEngine.load(link);
			}
		});
	}
	// start loading page by it's content
	@Override public void loadContent(final String text)
	{
		Platform.runLater(new Runnable()
		{
			@Override public void run()
			{
				webEngine.loadContent(text);
			}
		});
	}
	
	@Override public void stop()
	{
		Platform.runLater(new Runnable()
		{
			@Override public void run()
			{
				webEngine.getLoadWorker().cancel();
			}
		});
	}
	
	@Override public String getTitle()
	{
		if(webEngine==null) return null; // FIXME: throw exception when webEngine not ready to use
		return webEngine.titleProperty().get();
	}
	@Override public String getUrl()
	{
		if(webEngine==null) return null; // FIXME: throw exception when webEngine not ready to use
		return webEngine.getLocation();
	}
	
	@Override public Object executeScript(String script)
	{
		if(webEngine==null) return null; // FIXME: throw exception when webEngine not ready to use
		return webEngine.executeScript(script);
	}
	
	@Override public ElementList.SelectorALL selectorALL(boolean visible)
	{
		return new WebElementList.SelectorALL(visible);
	}
	@Override public ElementList.SelectorTEXT selectorTEXT(boolean visible,String filter)
	{
		return new WebElementList.SelectorTEXT(visible,filter);
	}
	@Override public ElementList.SelectorTAG selectorTAG(boolean visible,String tagName,String attrName,String attrValue)
	{
		return new WebElementList.SelectorTAG(visible,tagName,attrName,attrValue);
	}
	@Override public ElementList.SelectorCSS selectorCSS(boolean visible,String tagName,String styleName,String styleValue)
	{
		return new WebElementList.SelectorCSS(visible,tagName,styleName,styleValue);
	}
	@Override public ElementList elementList()
	{
		return new WebElementList(this);
	}
}
