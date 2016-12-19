
package org.luwrain.interaction.javafx;

import java.awt.Rectangle;
import java.util.*;
import java.util.concurrent.*;

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

import org.w3c.dom.Node;
import org.w3c.dom.html.*;
import org.w3c.dom.views.DocumentView;
import com.sun.webkit.dom.DOMWindowImpl;

import org.luwrain.core.*;
import org.luwrain.browser.*;
import org.luwrain.browser.Events.WebState;

class BrowserImpl implements Browser
{
    private final JavaFxInteraction interaction;
    private WebView webView = null;
    private WebEngine webEngine = null;
    private boolean busy = false;

    // list of all nodes in web page
    private Vector<NodeInfo> dom=new Vector<NodeInfo>();
    LinkedHashMap<org.w3c.dom.Node,Integer> domIdx = new LinkedHashMap<org.w3c.dom.Node, Integer>();
    private HTMLDocument htmlDoc = null;
    DOMWindowImpl htmlWnd = null;
    private JSObject window = null;
    private boolean userStops = false;

    BrowserImpl(JavaFxInteraction interaction)
    {
	NullCheck.notNull(interaction, "interaction");
	this.interaction = interaction;
    }

    @Override public void init(org.luwrain.browser.Events events)
    {
	final BrowserImpl browser = this;
	final boolean emptyList = interaction.browsers.isEmpty();
	interaction.browsers.add(this);
	Platform.runLater(()->{
		webView = new WebView();
		webEngine = webView.getEngine();
		webView.setOnKeyReleased((event)->onKeyReleased(event));
		webEngine.getLoadWorker().stateProperty().addListener((ov,oldState,newState)->onStateChange(events, ov, oldState, newState));
		webEngine.getLoadWorker().progressProperty().addListener((ov,o,n)->events.onProgress(n));
		webEngine.setOnAlert((event)->events.onAlert(event.getData()));
		webEngine.setPromptHandler((event)->events.onPrompt(event.getMessage(),event.getDefaultValue()));
		webEngine.setConfirmHandler((param)->events.onConfirm(param));
		webEngine.setOnError((event)->events.onError(event.getMessage()));
		webView.setVisible(false);
		interaction.addWebViewControl(webView);
		if(emptyList) 
		    interaction.setCurrentBrowser(browser);
	    });
    }

    private void onStateChange(org.luwrain.browser.Events events, ObservableValue<? extends State> ov,
			       State oldState, State newState)
    {
	Log.debug("javafx","browser state changed to: "+newState.name()+", "+webEngine.getLoadWorker().getState().toString()+", url:"+webEngine.getLocation());
	if(newState == State.CANCELLED)
	{ // if canceled not by user, so that is a file downloads
	    if(!userStops)
	    { // if it not by user
		if(events.onDownloadStart(webEngine.getLocation())) 
		    return;
	    }
	}
	final WebState state;
	switch(newState)
	{
	case CANCELLED:
	    state=WebState.CANCELLED;
	    break;
	case FAILED:	
	    state=WebState.FAILED;
	    break;
	case READY:		
	    state=WebState.READY;
	    break;
	case RUNNING:	
	    state=WebState.RUNNING;
	    break;
	case SCHEDULED:	
	    state=WebState.SCHEDULED;
	    break;
	case SUCCEEDED:	
	    state=WebState.SUCCEEDED;
	    break;
	default:
	    state = WebState.CANCELLED;
	}
	events.onChangeState(state);
    }

    private void onKeyReleased(KeyEvent event)
    {
	//Log.debug("web","KeyReleased: "+event.toString());
	switch(event.getCode())
	{
	case ESCAPE:
	    interaction.setCurrentBrowserVisibility(false);break;
	default:break;
	}
    }

    @Override public boolean isBusy()
    {
		return busy;
    }

    @Override public void Remove()
    {
	final int pos = interaction.browsers.indexOf(this);
	final boolean success = interaction.browsers.remove(this);
	if(!success) 
	    Log.warning("web","Can't found WebPage to remove it from WebEngineInteraction");
	setVisibility(false);
	if(pos!=-1)
	{
	    if(pos < interaction.browsers.size())
	    {
		interaction.setCurrentBrowser(interaction.browsers.get(pos));
	    }
	} else
	{
	    if(interaction.browsers.isEmpty()) 
interaction.setCurrentBrowser(null); else 
	    	interaction.setCurrentBrowser(interaction.browsers.lastElement());
	}
    }

    @Override public void setVisibility(boolean enable)
    {
	if(enable)
	{
	    interaction.disablePaint();
	Platform.runLater(()->{
		webView.setVisible(true);
		webView.requestFocus();
	    });
    } else
	{
	    Platform.runLater(()->{
		    interaction.enablePaint();
			webView.setVisible(false);
	    });
	}
    }

    @Override public boolean getVisibility()
    {
	return webView.isVisible();
    }

    private boolean checkNodeForIgnoreChildren(Node node)
    {
    	if(node == null) 
return false;
    	final Node parent = node.getParentNode();
    	if(parent == null) 
return false;
    	if(parent instanceof HTMLAnchorElement)
return true;
    	return checkNodeForIgnoreChildren(parent);
    }

    @Override public void RescanDOM()
    {
    	busy=true;
    	final Callable<Integer> task = ()->{
	    htmlDoc = (HTMLDocument)webEngine.getDocument();
	    if(htmlDoc == null)
		return null;
	    htmlWnd = (DOMWindowImpl)((DocumentView)htmlDoc).getDefaultView();
	    dom = new Vector<NodeInfo>();
	    domIdx = new LinkedHashMap<org.w3c.dom.Node, Integer>();
	    final JSObject js = (JSObject)webEngine.executeScript("(function(){"
								  + "function nodewalk(node){"
								  +   "var res=[];"
								  +   "if(node){"
								  + 	  "node=node.firstChild;"
								  +     "while(node!= null){"
								  +       "if(node.nodeType!=3||node.nodeValue.trim()!=='') "
								  +       "res[res.length]=node;"
								  +       "res=res.concat(nodewalk(node));"
								  +       "node=node.nextSibling;}"
								  +     "}"
								  +   "return res;"
								  + "};"
								  + "var lst=nodewalk(document);"
								  + "var res=[];"
								  + "for(var i=0;i<lst.length;i++){"
								  + "res.push({"
								  +   "n:lst[i],"
								  +   "r:(lst[i].getBoundingClientRect?"
								  +     "lst[i].getBoundingClientRect():"
								  +     "((function(nnn){"
								  +     "try{"
								  +       "var range=document.createRange();"
								  +       "range.selectNodeContents(nnn);"
								  +       "return range.getBoundingClientRect();}"
								  +     "catch(e)"
								  +       "{return null;};"
								  +     "})(lst[i])"
								  +     "))"
								  +   "});"
								  + "};"
								  + "return res;})()");
	    Object o;
	    for(int i=0;!(o=js.getMember(String.valueOf(i))).getClass().equals(String.class);i++)
	    {
		final JSObject rect=(JSObject)((JSObject)o).getMember("r");
		final org.w3c.dom.Node n=(org.w3c.dom.Node)((JSObject)o).getMember("n");
		final NodeInfo info=new NodeInfo();
		if(rect == null)
		    info.rect = new Rectangle(0,0,0,0); else
		{
		    final int x = (int)Double.parseDouble(rect.getMember("left").toString());
		    final int y = (int)Double.parseDouble(rect.getMember("top").toString());
		    final int width=(int)Double.parseDouble(rect.getMember("width").toString());
		    final int height=(int)Double.parseDouble(rect.getMember("height").toString());
		    info.rect = new Rectangle(x,y,width,height);
		}
		info.node = n;
		info.forTEXT = !n.hasChildNodes();
		// make decision about TEXT nodes by class
		if(n instanceof HTMLAnchorElement
		   ||n instanceof HTMLButtonElement
		   ||n instanceof HTMLInputElement
		   //||n.getClass() == com.sun.webkit.dom.HTMLPreElementImpl.class
		   ||n instanceof HTMLSelectElement
		   ||n instanceof HTMLTextAreaElement
		   ||n instanceof HTMLSelectElement)
		    info.forTEXT=true;
		final boolean ignore = checkNodeForIgnoreChildren(n);
		if(ignore) 
		    info.forTEXT=false;
		domIdx.put(n, i);
		dom.add(info);
	    }
	    for(NodeInfo info: dom)
	    {
		final org.w3c.dom.Node parent = info.node.getParentNode();
		if(domIdx.containsKey(parent))
		    info.parent = domIdx.get(parent);
	    }
	    window = (JSObject)webEngine.executeScript("window");
	    return null;
	};
	FutureTask<Integer> query=new FutureTask<Integer>(task){};
	if(Platform.isFxApplicationThread())
	{ // direct call
	    try {
		task.call();
	    }
	    catch(Exception e) 
	    {
		e.printStackTrace();
	    }
	} else
	{
	    Platform.runLater(query);
	    try {
		query.get();
	    }
	    catch(InterruptedException e)
	    {
		Thread.currentThread().interrupt();
	    }
	    catch(ExecutionException e) 
	    {
		e.printStackTrace();
	    }
	}
	busy=false;
    }

    @Override public void load(String url)
    {
	NullCheck.notNull(url, "url");
    	Platform.runLater(()->webEngine.load(url));
    }

    @Override public void loadContent(String text)
    {
	NullCheck.notNull(text, "text");
	Platform.runLater(()->webEngine.loadContent(text));
    }

    @Override public void stop()
    {
	Platform.runLater(()->webEngine.getLoadWorker().cancel());
    }

    @Override public String getTitle()
    {
		if(webEngine == null)
return "";
		return webEngine.titleProperty().get();
    }

    @Override public String getUrl()
    {
		if(webEngine == null)
		    return "";
		return webEngine.getLocation();
    }

    @Override public Object executeScript(String script)
    {
	NullCheck.notNull(script, "script");
		if(webEngine == null)
		    return null;
		//FIXME:In JavaFX thread
		return webEngine.executeScript(script);
    }

    @Override public SelectorAll selectorAll(boolean visible)
    {
    	return new SelectorAllImpl(visible);
    }

    @Override public SelectorText selectorText(boolean visible, String filter)
    {
	NullCheck.notNull(filter, "filter");
	return new SelectorTextImpl(visible,filter);
    }

    @Override public SelectorTag selectorTag(boolean visible,
String tagName, String attrName, String attrValue)
    {
	NullCheck.notNull(tagName, "tagName");
	NullCheck.notNull(attrName, "attrName");
	NullCheck.notNull(attrValue, "attrValue");
	return new SelectorTagImpl(visible,tagName,attrName,attrValue);
    }

    @Override public SelectorCss selectorCss(boolean visible,String tagName,String styleName,String styleValue)
    {
	return new SelectorCssImpl(visible,tagName,styleName,styleValue);
    }

    @Override public ElementIterator iterator()
    {
	return new ElementIteratorImpl(this);
    }

    @Override public int numElements()
    {
	return domIdx.size();
    }

	@Override public SelectorChildren rootChildren(boolean visible)
	{
		Vector<Integer> childs=new Vector<Integer>();
		SelectorAllImpl all=new SelectorAllImpl(visible);
		ElementIteratorImpl list=new ElementIteratorImpl(this);
		all.moveFirst(list);
		while(true)
		{
			NodeInfo info=list.current(); 
			if(info.parent==null)
				childs.add(domIdx.get(info.node));
			if(!all.moveNext(list)) break;
		}
		return new SelectorChildrenImpl(visible,childs.toArray(new Integer[childs.size()]));
	}

    Vector<NodeInfo> getDom()
    {
	return dom;
    }
}
