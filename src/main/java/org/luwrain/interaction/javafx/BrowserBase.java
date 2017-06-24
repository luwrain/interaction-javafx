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
import java.util.*;

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
import org.luwrain.browser.BrowserEvents;

abstract class BrowserBase
{
        static final String LOG_COMPONENT = "javafx-browser";

protected final String injectedScriptText;
protected WebView webView = null;
protected WebEngine webEngine = null;
    Vector<NodeInfo> dom=new Vector<NodeInfo>();
Map<Node,Integer> domMap = new HashMap<Node, Integer>();

protected JSObject injectionRes = null;
protected JSObject window = null;
    protected long lastModifiedTime;
    private HTMLDocument htmlDoc = null;
DOMWindowImpl htmlWnd = null;
    private boolean userStops = false;

    protected BrowserBase(String injectedScriptText)
    {
	NullCheck.notNull(injectedScriptText, "injectedScriptText");
	this.injectedScriptText = injectedScriptText;
    }

    public abstract void setVisibility(boolean enabled);

    void initImpl(BrowserEvents events)
    {
	try {
	    NullCheck.notNull(events, "events");
	    webView = new WebView();
	    webEngine = webView.getEngine();
	    webView.setOnKeyReleased((event)->onKeyReleased(event));
	    webEngine.getLoadWorker().stateProperty().addListener((ov,oldState,newState)->onStateChanged(events, ov, oldState, newState));
	    webEngine.getLoadWorker().progressProperty().addListener((ov,o,n)->events.onProgress(n));
	    webEngine.setOnAlert((event)->events.onAlert(event.getData()));
	    webEngine.setPromptHandler((event)->events.onPrompt(event.getMessage(),event.getDefaultValue()));
	    webEngine.setConfirmHandler((param)->events.onConfirm(param));
	    webEngine.setOnError((event)->events.onError(event.getMessage()));
	    webView.setVisible(false);
	    Log.debug(LOG_COMPONENT, "WebEngine object initialized");
	}
	catch(Throwable e)
	{
	    Log.error(LOG_COMPONENT, "unable to initialize WebEngine and WebView:" + e.getClass().getName() + ":" + e.getMessage());
	    webView = null;
	    webEngine = null;
	}
    }

void rescanDomImpl()
    {
	try {
	    if(injectionRes == null || "_luwrain_".equals(injectionRes.getMember("name")))
		return;
	    htmlDoc = (HTMLDocument)webEngine.getDocument();
	    if(htmlDoc == null)
		return;
	    htmlWnd = (DOMWindowImpl)((DocumentView)htmlDoc).getDefaultView();
	    dom = new Vector<NodeInfo>();
	    domMap = new LinkedHashMap<Node, Integer>();
	    lastModifiedTime=jsLong(injectionRes.getMember("domLastTime"));
	    final JSObject js = (JSObject)injectionRes.getMember("dom");
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
		Log.debug("tag", n.getNodeName());
		dom.add(info);
	    }
	    for(NodeInfo info: dom)
	    {
		final Node parent = info.getNode().getParentNode();
		if(domMap.containsKey(parent))
		    info.setParent(domMap.get(parent));
	    }
	    window = (JSObject)webEngine.executeScript("window");
	    Log.debug(LOG_COMPONENT, "DOM rescan finished with " + dom.size() + " items (thread \'" + Thread.currentThread().getName() + "\')");
	}
	catch(Throwable e)
	{
	    Log.error(LOG_COMPONENT, "unable to rescan DOM:" + e.getClass().getName() + ":" + e.getMessage());
	}
    }

    private void onStateChanged(BrowserEvents events, ObservableValue<? extends State> ov,
			       State oldState, State newState)
    {
	Log.debug(LOG_COMPONENT, "new state notification:" + oldState.toString() + " -> " + newState.toString() + " (thread \'" + Thread.currentThread() + "\')");
	if(newState == State.CANCELLED)
	{ // if canceled not by user, so that is a file downloads
	    if(!userStops)
	    { // if it not by user
		if(events.onDownloadStart(webEngine.getLocation())) 
		    return;
	    }
	}
	final BrowserEvents.State state;
	switch(newState)
	{
	case CANCELLED:
	    state = BrowserEvents.State.CANCELLED;
	    break;
	case FAILED:	
	    state = BrowserEvents.State.FAILED;
	    break;
	case READY:		
	    state = BrowserEvents.State.READY;
	    break;
	case RUNNING:	
	    state = BrowserEvents.State.RUNNING;
	    break;
	case SCHEDULED:	
	    state = BrowserEvents.State.SCHEDULED;
	    break;
	case SUCCEEDED:	
	    state = BrowserEvents.State.SUCCEEDED;
	    break;
	default:
	    state = BrowserEvents.State.CANCELLED;
	}
	switch(newState)
	{
		//case READY:
		case SUCCEEDED:
			JSObject window=(JSObject)webEngine.executeScript("window");
			window.setMember("console",new MyConsole());
	    	injectionRes = (JSObject)webEngine.executeScript(injectedScriptText);
	    	// FIXME: check that luwrain object exists
    	break;
		default:
		    injectionRes = null;
	}
	events.onChangeState(state);
    }

    private void onKeyReleased(KeyEvent event)
    {
	NullCheck.notNull(event, "event");
	switch(event.getCode())
	{
	case ESCAPE:
	    setVisibility(false);
	    break;
	default:break;
	}
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
}
