
package org.luwrain.interaction.javafx;

import java.awt.Rectangle;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.EventHandler;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import netscape.javascript.JSObject;

import org.luwrain.browser.*;
import org.luwrain.browser.Events.WebState;
import org.luwrain.core.Interaction;
import org.luwrain.core.Log;
import org.luwrain.interaction.javafx.JavaFxInteraction;
import org.w3c.dom.Node;
import org.w3c.dom.html.*;
import org.w3c.dom.views.DocumentView;

import com.sun.webkit.dom.DOMWindowImpl;

class WebPage implements Browser
{
    private JavaFxInteraction wi;

    WebView webView;
    WebEngine webEngine;

    // list of all nodes in web page
    Vector<NodeInfo> dom=new Vector<NodeInfo>();
    // index map for fast get node position
    LinkedHashMap<org.w3c.dom.Node,Integer> domIdx = new LinkedHashMap<org.w3c.dom.Node, Integer>();

    HTMLDocument htmlDoc=null;
    DOMWindowImpl htmlWnd=null;

    JSObject window=null;
    private boolean userStops=false;

    @Override public String getBrowserTitle()
    {
	return "ВебБраузер";//FIXME:
    }

    @Override public Browser setInteraction(Interaction interaction)
    {
	wi=(JavaFxInteraction)interaction;
	return null;
    }

    public WebPage(JavaFxInteraction interaction)
    {
	wi = interaction;
    }

    static <A> A fxcall(Callable<A> task, A onfail)
    {
		FutureTask<A> query=new FutureTask<A>(task){};
		if(Platform.isFxApplicationThread())
		{ // direct call
			try {return task.call();}
			catch(Exception e) {e.printStackTrace();}
		    return onfail;
		} else
		{
			// call from awt thread 
			Platform.runLater(query);
			// waiting for rescan end
			try {return query.get();}
			catch(InterruptedException|ExecutionException e) {e.printStackTrace();}
		    return onfail;
		}
    }
    
	// make new empty WebPage (like about:blank) and add it to WebEngineInteraction's webPages
    public void init(final org.luwrain.browser.Events events)
    {
	final WebPage that=this;
	final boolean emptyList=wi.webPages.isEmpty();
	wi.webPages.add(this);

	Platform.runLater(new Runnable() {
		@Override public void run()
		{
		    webView=new WebView();
		    webEngine=webView.getEngine();
		    webView.setOnKeyReleased(new EventHandler<KeyEvent>()
		    {
				 @Override public void handle(KeyEvent event)
				 {
				     //Log.debug("web","KeyReleased: "+event.toString());
				     switch(event.getCode())
				     {
				     case ESCAPE:wi.setCurPageVisibility(false);break;
				     default:break;
				     }
				 }
		    });
		    webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>()
		    {
		    	@Override public void changed(ObservableValue<? extends State> ov,State oldState,final State newState)
		    	{
		    		Log.debug("web","State changed to: "+newState.name()+", "+webEngine.getLoadWorker().getState().toString()+", url:"+webEngine.getLocation());
		    			if(newState==State.CANCELLED)
						{ // if canceled not by user, so that is a file downloads
						    if(!userStops)
						    { // if it not by user
							if(events.onDownloadStart(webEngine.getLocation())) return;
						    }
						}
						WebState state=WebState.CANCELLED;
						switch(newState)
						{
							case CANCELLED:	state=WebState.CANCELLED;break;
							case FAILED:	state=WebState.FAILED;break;
							case READY:		state=WebState.READY;break;
							case RUNNING:	state=WebState.RUNNING;break;
							case SCHEDULED:	state=WebState.SCHEDULED;break;
							case SUCCEEDED:	state=WebState.SUCCEEDED;break;
						}
						events.onChangeState(state);
		    	}
		    });

		    webEngine.getLoadWorker().progressProperty().addListener(new ChangeListener<Number>()
		    {
		    	@Override public void changed(ObservableValue<? extends Number> ov,Number o,final Number n)
		    	{
		    		events.onProgress(n);
		    	}
		    });

		    webEngine.setOnAlert(new EventHandler<WebEvent<String>>() {
			    @Override public void handle(final WebEvent<String> event)
			    {
						Log.debug("web","t:"+Thread.currentThread().getId()+" ALERT:"+event.getData());
							events.onAlert(event.getData());
					}
				});

				webEngine.setPromptHandler(new Callback<PromptData,String>() {
					@Override public String call(final PromptData event)
					{
					    Log.debug("web","t:"+Thread.currentThread().getId()+" PROMPT:"+event.getMessage()+"', default '"+event.getDefaultValue()+"'");
								return events.onPrompt(event.getMessage(),event.getDefaultValue());
					}
				});

				webEngine.setConfirmHandler(new Callback<String,Boolean>()
				{
					@Override public Boolean call(String param)
					{
						Log.debug("web","t:"+Thread.currentThread().getId()+" CONFIRM: "+param);
						return events.onConfirm(param);
					}
				});

				webEngine.setOnError(new EventHandler<WebErrorEvent>()
				{
					@Override public void handle(final WebErrorEvent event)
					{
						Log.debug("web","thread:"+(Platform.isFxApplicationThread()?"javafx":"main")+"ERROR:"+event.getMessage());
							events.onError(event.getMessage());
					}
				});

				webView.setVisible(false);
				wi.addWebViewControl(webView);

				if(emptyList) 
					wi.setCurPage(that);
			}
		});
    }

	private boolean busy=false;
    @Override public boolean isBusy()
    {
		return busy;
    }
    
    // remove WebPage from WebEngineInteraction's webPages list and stop WebEngine work, prepare for destroy
    @Override public void Remove()
    {
	int pos=wi.webPages.indexOf(this);
	final boolean success=wi.webPages.remove(this);
	if(!success) 
	    Log.warning("web","Can't found WebPage to remove it from WebEngineInteraction");
	setVisibility(false);
	if(pos!=-1)
	{
	    if(pos<wi.webPages.size())
	    {
		wi.setCurPage(wi.webPages.get(pos));
	    }
	} else
	{
	    if(wi.webPages.isEmpty()) wi.setCurPage(null); else 
	    	wi.setCurPage(wi.webPages.lastElement());
	}
    }

    @Override public void setVisibility(boolean enable)
    {
	if(enable)
	{ // set visibility for this webpage on and change focus to it later (text page visibility is off)
	    wi.disablePaint();
	    Platform.runLater(new Runnable(){@Override public void run() {
		//Log.debug("web","request focus "+webView);
		webView.setVisible(true);
		webView.requestFocus();
	    }});
	} else
	{ // set text page visibility to on and current webpage to off
	    //wi.frame.setVisible(true);
	    Platform.runLater(new Runnable(){@Override public void run()
	    {
		    wi.enablePaint();
			webView.setVisible(false);
	    }});
	}
    }

    @Override public boolean getVisibility()
    {
	return webView.isVisible();
    }

    // check node in scanned dom structure to have in parent one of ignore children situation, for example anchor tag, return true if children must be ignored 
    private boolean checkNodeForIgnoreChildren(Node node)
    {
    	if(node==null) return false;
    	Node parrent=node.getParentNode();
    	if(parrent==null) return false;
    	if(parrent instanceof HTMLAnchorElement) return true;
    	return checkNodeForIgnoreChildren(parrent);
    }
    
    // rescan current page DOM model and refill list of nodes with it bounded rectangles
    @Override public void RescanDOM()
    {
    	busy=true;
    	Callable<Integer> task=new Callable<Integer>(){
	    @Override public Integer call() throws Exception
	    {
		htmlDoc=(HTMLDocument)webEngine.getDocument();
		if(htmlDoc==null) return null;
		htmlWnd=(DOMWindowImpl)((DocumentView)htmlDoc).getDefaultView();
		
		dom=new Vector<NodeInfo>();
		domIdx=new LinkedHashMap<org.w3c.dom.Node, Integer>();
		JSObject js=(JSObject)webEngine.executeScript("(function(){function nodewalk(node){var res=[];if(node){node=node.firstChild;while(node!= null){if(node.nodeType!=3||node.nodeValue.trim()!=='') res[res.length]=node;res=res.concat(nodewalk(node));node=node.nextSibling;}}return res;};var lst=nodewalk(document);var res=[];for(var i=0;i<lst.length;i++){res.push({n:lst[i],r:(lst[i].getBoundingClientRect?lst[i].getBoundingClientRect():(lst[i].parentNode.getBoundingClientRect?lst[i].parentNode.getBoundingClientRect():null))});};return res;})()");;
		Object o;
		for(int i=0;!(o=js.getMember(String.valueOf(i))).getClass().equals(String.class);i++)
		{
		    JSObject rect=(JSObject)((JSObject)o).getMember("r");
		    final org.w3c.dom.Node n=(org.w3c.dom.Node)((JSObject)o).getMember("n");
		    final NodeInfo info=new NodeInfo();
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
		    // by default, only leaf nodes good for TEXT 
		    info.forTEXT=!n.hasChildNodes();
		    // make decision about TEXT nodes by class
		    if(n instanceof HTMLAnchorElement
		      ||n instanceof HTMLButtonElement
		      ||n instanceof HTMLInputElement
		    //||n.getClass()==com.sun.webkit.dom.HTMLPreElementImpl.class
		      ||n instanceof HTMLSelectElement
		      ||n instanceof HTMLTextAreaElement
		      ||n instanceof HTMLSelectElement)
		    {
		    	info.forTEXT=true;
		    }
		    boolean ignore=checkNodeForIgnoreChildren(n);
		    //Log.debug("web","DOM: "+i+": "+info.node.getClass().getSimpleName()+", r:"+info.rect.x+"x"+info.rect.y+"-"+info.rect.width+"x"+info.rect.height+" ignore:"+ignore+", text:"+info.forTEXT);
		    if(ignore) info.forTEXT=false;
		    //if(info.forTEXT&&info.isVisible()) System.out.println("DOM: node:"+n.getNodeName()+", "+(!(n instanceof HTMLElement)?n.getNodeValue():((HTMLElement)n).getTextContent())); // +" text:"+info.forTEXT+
		    domIdx.put(n, i);
		    dom.add(info);
		    //
		}
		// reselect window object (for example if page was reloaded)
		window=(JSObject)webEngine.executeScript("window");
		return null;
	    }};

		FutureTask<Integer> query=new FutureTask<Integer>(task){};
		if(Platform.isFxApplicationThread())
		{ // direct call
			try {task.call();}
			catch(Exception e) {e.printStackTrace();}
		} else
		{
			// call from awt thread 
			Platform.runLater(query);
			// waiting for rescan end
			try {query.get();}
			catch(InterruptedException|ExecutionException e) {e.printStackTrace();}
		}
		busy=false;
    }

	// start loading page via link
    @Override public void load(String link)
    {
    	final String l = link;
    	Platform.runLater(new Runnable() {
    		@Override public void run()
    		{
				webEngine.load(l);
    		}});
    }

    // start loading page by it's content
    @Override public void loadContent(String text)
    {
		final String t = text;
		Platform.runLater(new Runnable() {
			@Override public void run()
			{
			    webEngine.loadContent(t);
			}});
    }

    @Override public void stop()
    {
		Platform.runLater(new Runnable() {
			@Override public void run()
			{
			    webEngine.getLoadWorker().cancel();
			}});
    }

    @Override public String getTitle()
    {
		if(webEngine==null)// return null; // FIXME: throw exception when webEngine not ready to use
		    throw new NullPointerException("webEngine not initialized");
		return webEngine.titleProperty().get();
    }

    @Override public String getUrl()
    {
		if(webEngine==null) //return null; // FIXME: throw exception when webEngine not ready to use
		    throw new NullPointerException("webEngine not initialized");
		return webEngine.getLocation();
    }

    @Override public Object executeScript(String script)
    {
		if(webEngine==null) //return null; // FIXME: throw exception when webEngine not ready to use
		    throw new NullPointerException("webEngine initialized");
		return webEngine.executeScript(script);
    }

    @Override public SelectorAll selectorAll(boolean visible)
    {
    	return new SelectorAllImpl(visible);
    }

    @Override public SelectorText selectorText(boolean visible,String filter)
    {
	return new SelectorTextImpl(visible,filter);
    }

    @Override public SelectorTag selectorTag(boolean visible,String tagName,String attrName,String attrValue)
    {
	return new SelectorTagImpl(visible,tagName,attrName,attrValue);
    }

    @Override public SelectorCss selectorCss(boolean visible,String tagName,String styleName,String styleValue)
    {
	return new SelectorCssImpl(visible,tagName,styleName,styleValue);
    }

    @Override public ElementList elementList()
    {
	return new ElementIteratorImpl(this);
    }

    @Override public int numElements()
    {
	return domIdx.size();
    }
}
