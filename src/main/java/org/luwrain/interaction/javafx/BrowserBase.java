/*
   Copyright 2012-2018 Michael Pozhidaev <michael.pozhidaev@gmail.com>
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
    static final String LOG_COMPONENT = "browser";

    protected final String injectedScript;
    protected WebView webView = null;
    protected WebEngine webEngine = null;
    protected DomScanResult domScanRes = null;

    protected JSObject injectionRes = null;
    protected JSObject jsWindow = null;

    protected BrowserBase(String injectedScript)
    {
	NullCheck.notNull(injectedScript, "injectedScript");
	this.injectedScript = injectedScript;
    }

    public abstract void setVisibility(boolean enabled);

    protected void init(BrowserEvents events)
    {
	NullCheck.notNull(events, "events");
	Utils.ensureFxThread();
	try {
	    this.webView = new WebView();
	    this.webEngine = webView.getEngine();
	    webView.setOnKeyReleased((event)->onKeyReleased(event));
	    webEngine.getLoadWorker().stateProperty().addListener((ov,oldState,newState)->onStateChanged(events, ov, oldState, newState));
	    webEngine.getLoadWorker().progressProperty().addListener((ov,o,n)->events.onProgress(n));
	    webEngine.setOnAlert((event)->events.onAlert(event.getData()));
	    webEngine.setPromptHandler((event)->events.onPrompt(event.getMessage(),event.getDefaultValue()));
	    webEngine.setConfirmHandler((param)->events.onConfirm(param));
	    webEngine.setOnError((event)->events.onError(event.getMessage()));
	    webView.setVisible(false);
	}
	catch(Throwable e)
	{
	    Log.error(LOG_COMPONENT, "unable to initialize WebEngine and WebView:" + e.getClass().getName() + ":" + e.getMessage());
	    this.webView = null;
	    this.webEngine = null;
	}
    }

    DomScanResult getDomScanResult()
    {
	return this.domScanRes;
    }

Object executeScript(String script)
    {
	NullCheck.notNull(script, "script");
	if(script.trim().isEmpty() || webEngine == null)
	    return null;
	return webEngine.executeScript(script);
    }

    private void resetInjectionRes()
    {
	InvalidThreadException.checkThread("BrowserBase.resetInjectionRes()");
	this.injectionRes = null;
    }

    private void runInjection()
    {
	try {
	    final JSObject window = (JSObject)webEngine.executeScript("window");
	    window.setMember("console",new MyConsole());
	    this.injectionRes = (JSObject)webEngine.executeScript(injectedScript);
	    if (injectionRes != null)
		Log.debug(LOG_COMPONENT, "injection executed successfully"); else
		Log.warning(LOG_COMPONENT, "the injection result is null after running the injection script");
	}
	catch(Throwable e)
	{
	    Log.error(LOG_COMPONENT, "unable to run a browser injection:" + e.getClass().getName() + ":" + e.getMessage());
	    e.printStackTrace();
	}
    }

protected void rescanDom()
    {
	InvalidThreadException.checkThread("BrowserBase.rescanDom()"); 
	try {
	    if(injectionRes == null || injectionRes.getMember("name").equals("_luwrain_"))
		return;
final HTMLDocument webDoc = (HTMLDocument)webEngine.getDocument();
	    if(webDoc == null)
	    {
		Log.warning(LOG_COMPONENT, "no web document");
		return;
	    }
final DOMWindowImpl window = (DOMWindowImpl)((DocumentView)webDoc).getDefaultView();
this.domScanRes = new DomScanResult(window);
	    final JSObject js = (JSObject)injectionRes.getMember("dom");
	    Object o = null;
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
		final NodeInfo info = new NodeInfo(n, x, y, width, height);
		domScanRes.domMap.put(n, i);
		domScanRes.dom.add(info);
	    }
	    for(NodeInfo info: domScanRes.dom)
	    {
		final Node parent = info.getNode().getParentNode();
		if(domScanRes.domMap.containsKey(parent))
		    info.setParentIndex(domScanRes.domMap.get(parent).intValue());
	    }
	    	    this.jsWindow = (JSObject)webEngine.executeScript("window");
		    Log.debug(LOG_COMPONENT, "DOM rescanning completed");
	}
	catch(Throwable e)
	{
	    Log.error(LOG_COMPONENT, "unable to rescan DOM:" + e.getClass().getName() + ":" + e.getMessage());
	    e.printStackTrace();
	}
    }

    private void onStateChanged(BrowserEvents events, ObservableValue<? extends State> ov, State oldState, State newState)
    {
	NullCheck.notNull(events, "events");
	if (oldState == null || newState == null)
	{
	    Log.warning(LOG_COMPONENT, "oldState or newState is null in BrowserBase.onStateChanged()");
	    return;
	}
	Log.debug(LOG_COMPONENT, "New state: " + newState.toString() + ", previous was " + oldState.toString());
	final BrowserEvents.State state;
	switch(newState)
	{
	case CANCELLED:
	    //It could be a start of downloading, if cancelling was initiated not by a user
	    //FIXME:if(events.onDownloadStart(webEngine.getLocation())) 
	    state = BrowserEvents.State.CANCELLED;
	    resetInjectionRes();
	    break;
	case FAILED:	
	    state = BrowserEvents.State.FAILED;
	    resetInjectionRes();
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
	    runInjection();
	    state = BrowserEvents.State.SUCCEEDED;
	    break;
	default:
	    state = BrowserEvents.State.CANCELLED;
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

	static public long jsLong(Object o)
	{
		if(o == null) 
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
