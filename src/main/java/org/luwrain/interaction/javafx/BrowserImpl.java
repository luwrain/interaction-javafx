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

final class BrowserImpl extends BrowserBase implements Browser
{
    static private final String RESCAN_RESOURCE_PATH = "org/luwrain/interaction/javafx/injection.js";
    static final int LAST_MODIFIED_SCAN_INTERVAL = 100; // lastModifiedTime rescan interval in milliseconds
    static final String LUWRAIN_NODE_TEXT="luwrain_node_text"; // javascript window's property names for using in executeScrypt

    private final JavaFxInteraction interaction;

    BrowserImpl(JavaFxInteraction interaction)
    {
	super(readTextResource(RESCAN_RESOURCE_PATH));
	NullCheck.notNull(interaction, "interaction");
	this.interaction = interaction;
    }

    @Override public void init(BrowserEvents events)
    {
	NullCheck.notNull(events, "events");
	final boolean emptyList = interaction.browsers.isEmpty();
	interaction.browsers.add(this);
	Utils.runInFxThreadSync(()->{
		super.init(events);
		interaction.addWebViewControl(webView);
		if(emptyList) 
		    interaction.setCurrentBrowser(BrowserImpl.this);
	    });
    }

    private boolean initialized()
    {
	return webEngine != null && webView != null;
    }

    @Override public void doFastUpdate()
    {
    	Platform.runLater(()->
			  {
			      // check if injected object success
			      if(injectionRes == null || "_luwrain_".equals(injectionRes.getMember("name")))
				  return;
			      this.jsWindow.setMember(LUWRAIN_NODE_TEXT, injectionRes);
			      webEngine.executeScript(LUWRAIN_NODE_TEXT+".doUpdate();");
			  });
    }

    @Override public void rescanDom()
    {
	if (!initialized())
	    return;
	Utils.runInFxThreadSync(()->super.rescanDom());
    }

    @Override public void setWatchNodes(Iterable<Integer> indexes)
    {
	Platform.runLater(()->{
		// check if injected object success
		if(injectionRes == null || "_luwrain_".equals(injectionRes.getMember("name")))
		    return;
		// fill javascript array
		final JSObject js = (JSObject)injectionRes.getMember("dom");
		JSObject watchArray=(JSObject)webEngine.executeScript("[]"); // FIXME: found correct method to create js array
		int j=0;
		for(int i:indexes)
		    watchArray.setSlot(j++,i);
		// set watch member
		injectionRes.setMember("watch",watchArray);
	    });
    }

    @Override public void close()
    {
	final int pos = interaction.browsers.indexOf(this);
	final boolean success = interaction.browsers.remove(this);
	if(!success) 
	    Log.warning(LOG_COMPONENT,"Can't found WebPage to remove it from WebEngineInteraction");
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
	if (!initialized())
	    return;
	if(enable)
	{
	    interaction.disablePaint();
	    Utils.runInFxThreadSync(()->{
		    webView.setVisible(true);
		    webView.requestFocus();
		});
	    return;
	}
	interaction.enablePaint();
	Utils.runInFxThreadSync(()->webView.setVisible(false));
    }

    @Override public boolean getVisibility()
    {
	if (!initialized())
	    return false;
	return webView.isVisible();//FIXME:
    }

    @Override public void loadByUrl(String url)
    {
	NullCheck.notNull(url, "url");
	if (initialized())
	    Utils.runInFxThreadSync(()->webEngine.load(url));
    }

    @Override public synchronized void loadByText(String text)
    {
	NullCheck.notNull(text, "text");
	if (initialized())
	    Utils.runInFxThreadSync(()->webEngine.loadContent(text));
    }

    @Override public void stop()
    {
	Utils.runInFxThreadSync(()->webEngine.getLoadWorker().cancel());
    }

    @Override public synchronized String getTitle()
    {
	if(webEngine == null)
	    return "";
	return webEngine.titleProperty().get();
    }

    @Override public synchronized String getUrl()
    {
	if(webEngine == null)
	    return "";
	return webEngine.getLocation();
    }

    @Override public Object runSafely(Callable callable)
    {
	NullCheck.notNull(callable, "callable");
	return Utils.callInFxThreadSync(callable);
    }

    @Override public synchronized Object executeScript(String script)
    {
	NullCheck.notNull(script, "script");
	if(script.trim().isEmpty() || webEngine == null)
	    return null;
	return Utils.callInFxThreadSync(()->super.executeScript(script));
    }

    @Override public BrowserIterator createIterator()
    {
	InvalidThreadException.checkThread("BrowserImpl.createIterator()");
	return new IteratorImpl(this);
    }

    @Override public int numElements()
    {
	if (domScanRes == null)
	    return 0;
	return domScanRes.domMap.size();
    }

    @Override public long getLastTimeChanged()
    {
	return 0;
    }

    static String readTextResource(String path)
    {
	NullCheck.notEmpty(path, "path");
    	try {
	    final InputStream s = ClassLoader.getSystemClassLoader().getResourceAsStream(path);
	    if (s == null)
	    {
		Log.error(LOG_COMPONENT, "inaccessible resource:" + path);
		return null;
	    }
	    final BufferedReader r = new BufferedReader(new InputStreamReader(s));
	    final StringBuilder b = new StringBuilder();
	    String line = null;
	    while((line = r.readLine()) != null)
		b.append(line + "\n");
	    return new String(b);
    	}
    	catch(IOException e)
    	{
	    Log.error(LOG_COMPONENT, "unable to read system resource:" + path + ":" + e.getClass().getName() + ":" + e.getMessage());
	    return null;
    	}
    }
}
