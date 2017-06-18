/*
   Copyright 2012-2017 Michael Pozhidaev <michael.pozhidaev@gmail.com>
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

import java.awt.Rectangle;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.input.KeyEvent;
import netscape.javascript.JSObject;

import org.w3c.dom.Node;
import org.w3c.dom.html.*;
import org.w3c.dom.views.DocumentView;
import com.sun.webkit.dom.DOMWindowImpl;

import org.luwrain.core.*;
import org.luwrain.browser.*;

class BrowserImpl implements Browser
{
    static final String LOG_COMPONENT = "javafx-browser";
    static private final String RESCAN_RESOURCE_PATH = "org/luwrain/interaction/javafx/rescan.js";
    static final ClassLoader cl=ClassLoader.getSystemClassLoader();
    static final String luwrainJS;

    /** lastModifiedTime rescan interval in milliseconds */
    static final int LAST_MODIFIED_SCAN_INTERVAL=100;
    // javascript window's property names for using in executeScrypt
	static final String LUWRAIN_NODE_TEXT="luwrain_node_text";

    private final JavaFxInteraction interaction;
    private WebView webView = null;
    private WebEngine webEngine = null;
    private boolean busy = false;
    private Vector<NodeInfo> dom=new Vector<NodeInfo>();
LinkedHashMap<Node,Integer> domMap = new LinkedHashMap<Node, Integer>();
    private HTMLDocument htmlDoc = null;
    DOMWindowImpl htmlWnd = null;//FIXME:
    private JSObject window = null;
    private boolean userStops = false;

    private JSObject luwrainJSobject=null;
	private long lastModifiedTime;
	
	public class MyConsole
	{
		public void log(Object str)
		{
			Log.info("javafx-dom",str.toString());
		}
	}

    /** return current browser's list of nodes, WARNING, use w3c node only in Browser's thread */
    @Override public Vector<NodeInfo> getDOMList()
    {
    	return dom;
    }
    /** return reverse index HashMap for accessing NodeInfo index in dom list by w3c Node */
    @Override public int getNodeIndex(org.w3c.dom.Node node)
    {
	NullCheck.notNull(node, "node");
	if (domMap == null || !domMap.containsKey(node))
	    return -1;
    	return domMap.get(node).intValue();
    }
    
    public BrowserImpl(JavaFxInteraction interaction)
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
    // start changes detection
	Timer timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask()
    {
        @Override public void run()
        {
        	Platform.runLater(()->{
				{
					if(luwrainJSobject==null) return;
					long time=(long)(double)luwrainJSobject.getMember("domLastTime");
					if(time==lastModifiedTime) return;
					//System.out.println("modified");
					// does not call changed event first time page loaded
					if(lastModifiedTime!=0)
						events.onPageChanged();
					lastModifiedTime=time;
				}});
        }
    }, 0, LAST_MODIFIED_SCAN_INTERVAL);

    }

    @Override public void doFastUpdate()
    {
    	Platform.runLater(()->
    	{
        	// check if injected object success
        	if(luwrainJSobject==null||"_luwrain_".equals(luwrainJSobject.getMember("name")))
        		return;
    		window.setMember(LUWRAIN_NODE_TEXT,luwrainJSobject);
    		webEngine.executeScript(LUWRAIN_NODE_TEXT+".doUpdate();");
    	});
    }
    
    @Override public void RescanDOM()
    {
    	busy=true;
    	final Callable<Integer> task = ()->{
	    // check if injected object success
	    if(luwrainJSobject==null||"_luwrain_".equals(luwrainJSobject.getMember("name")))
    		return null;
	    // prepare some  objects document and window
	    htmlDoc = (HTMLDocument)webEngine.getDocument();
	    if(htmlDoc == null)
		return null;
	    htmlWnd = (DOMWindowImpl)((DocumentView)htmlDoc).getDefaultView();
	    dom = new Vector<NodeInfo>();
	    domMap = new LinkedHashMap<Node, Integer>();
	    lastModifiedTime=jsLong(luwrainJSobject.getMember("domLastTime"));
	    final JSObject js = (JSObject)luwrainJSobject.getMember("dom");
	    //JSObject watchArray=(JSObject)webEngine.executeScript("[]");
	    //int j=0;
	    Object o;
	    for(int i=0;!(o=js.getSlot(i)).getClass().equals(String.class);i++)
	    {
		final JSObject rect=(JSObject)((JSObject)o).getMember("r");
		final Node n=(Node)((JSObject)o).getMember("n");
		int x = 0;
		int y = 0;
		int width = 0;
		int height = 0;
		if(rect != null)
		{
		    x = (int)jsLong(rect.getMember("left"));
		    y = (int)jsLong(rect.getMember("top"));
		    width=(int)jsLong(rect.getMember("width"));
		    height=(int)jsLong(rect.getMember("height"));
		}
		boolean forText = !n.hasChildNodes();
		// make decision about TEXT nodes by class
		if(n instanceof HTMLAnchorElement
		   ||n instanceof HTMLButtonElement
		   ||n instanceof HTMLInputElement
		   //||n.getClass() == com.sun.webkit.dom.HTMLPreElementImpl.class
		   ||n instanceof HTMLSelectElement
		   ||n instanceof HTMLTextAreaElement
		   ||n instanceof HTMLSelectElement)
		    forText = true;
		final boolean ignore = checkNodeForIgnoreChildren(n);
		if(ignore) 
		    forText = false;
		final NodeInfo info=new NodeInfo(n,x,y,width,height,forText);
		domMap.put(n, i);
		dom.add(info);
		//Log.debug("javafx-dom", i+": "+info.descr());
	    }
	    for(NodeInfo info: dom)
	    {
		final Node parent = info.getNode().getParentNode();
		if(domMap.containsKey(parent))
		    info.setParent(domMap.get(parent));
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

    @Override public void setWatchNodes(Iterable<Integer> indexes)
    {
	Platform.runLater(()->{
		// check if injected object success
		if(luwrainJSobject==null||"_luwrain_".equals(luwrainJSobject.getMember("name")))
			return;
		// fill javascript array
		final JSObject js = (JSObject)luwrainJSobject.getMember("dom");
		JSObject watchArray=(JSObject)webEngine.executeScript("[]"); // FIXME: found correct method to create js array
		int j=0;
		for(int i:indexes)
			watchArray.setSlot(j++,i);
		// set watch member
	    luwrainJSobject.setMember("watch",watchArray);
	});
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
	if (webEngine == null || webView == null)
	    return;
	if(enable)
	{
	    interaction.disablePaint();
	    Utils.runInFxThreadAsync(()->{
		    webView.setVisible(true);
		    webView.requestFocus();
		});
	    return;
	}
	interaction.enablePaint();
	Utils.runInFxThreadAsync(()->webView.setVisible(false));
    }

    @Override public boolean getVisibility()
    {
	return webView.isVisible();
    }

    @Override public void loadByUrl(String url)
    {
	NullCheck.notNull(url, "url");
Utils.runInFxThreadAsync(()->webEngine.load(url));
    }

    @Override public void loadByText(String text)
    {
	NullCheck.notNull(text, "text");
Utils.runInFxThreadAsync(()->webEngine.loadContent(text));
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
	if(script.trim().isEmpty() || webEngine == null)
	    return null;
	final Callable<Object> task = ()->webEngine.executeScript(script);
	return Utils.runInFxThreadSync(task);
    }

    @Override public ElementIterator iterator()
    {
	return new ElementIteratorImpl(this);
    }

    @Override public int numElements()
    {
	return domMap.size();
    }

	@Override public long getLastTimeChanged()
	{
		return lastModifiedTime;
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
	final Events.State state;
	switch(newState)
	{
	case CANCELLED:
	    state = Events.State.CANCELLED;
	    break;
	case FAILED:	
	    state = Events.State.FAILED;
	    break;
	case READY:		
	    state = Events.State.READY;
	    break;
	case RUNNING:	
	    state = Events.State.RUNNING;
	    break;
	case SCHEDULED:	
	    state = Events.State.SCHEDULED;
	    break;
	case SUCCEEDED:	
	    state = Events.State.SUCCEEDED;
	    break;
	default:
	    state = Events.State.CANCELLED;
	}
	//
	switch(newState)
	{
		//case READY:
		case SUCCEEDED:
			JSObject window=(JSObject)webEngine.executeScript("window");
			window.setMember("console",new MyConsole());
	    	luwrainJSobject=(JSObject)webEngine.executeScript(luwrainJS);
	    	// FIXME: check that luwrain object exists
    	break;
		default:
			luwrainJSobject=null;
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

	static public long jsLong(Object o)
	{
		if(o==null) 
return 0;
		if(o instanceof Double) 
return (long)(double)o;
		if(o instanceof Integer) 
return (long)(int)o;
		//throw new Exception("js have unknown number type: "+o.getClass().getName());
		// FIXME: it can be happened or not?
		return (long)Double.parseDouble(o.toString());
	}

    /** load resource text file as javascript and replace to luwrain member */
    static String getJSResource(String path)
    {
	NullCheck.notNull(path, "path");
    	try {
	    final InputStream s = cl.getResourceAsStream(path);
	    if (s == null)
	    {
		Log.error("javafx", "inaccessible resource:" + path);
		return null;
	    }
	    final BufferedReader r = new BufferedReader(new InputStreamReader(cl.getResourceAsStream(path)));
	    final StringBuilder b = new StringBuilder();
	    String line = null;
	    while((line = r.readLine()) != null)
		b.append(line + "\n");
	    return new String(b);
    	}
    	catch(IOException e)
    	{
	    Log.error("javafx", "unable to read system resource:" + path + ":" + e.getClass().getName() + ":" + e.getMessage());
	    return null;
    	}
    }

    static 
    {
	luwrainJS=getJSResource(RESCAN_RESOURCE_PATH);
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
}
