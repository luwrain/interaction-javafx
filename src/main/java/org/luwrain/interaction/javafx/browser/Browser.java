/*
   Copyright 2012-2020 Michael Pozhidaev <msp@luwrain.org>
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

package org.luwrain.interaction.javafx.browser;

import java.io.*;
import java.util.concurrent.*;

import org.luwrain.core.*;
import org.luwrain.browser.BrowserEvents;
import org.luwrain.interaction.javafx.*;

public final class Browser extends Base implements org.luwrain.browser.Browser
{
    static final int LAST_MODIFIED_SCAN_INTERVAL = 100; // lastModifiedTime rescan interval in milliseconds
    static final String LUWRAIN_NODE_TEXT="luwrain_node_text"; // javascript window's property names for using in executeScrypt

    private final JavaFxInteraction interaction;

    public Browser(JavaFxInteraction interaction, BrowserEvents events) throws IOException
    {
	super(events);
	NullCheck.notNull(interaction, "interaction");
	this.interaction = interaction;
    }

    @Override public void rescanDom()
    {
	Utils.runInFxThreadSync(()->super.rescanDom());
    }

    @Override public void close()
    {
	Utils.runInFxThreadSync(()->interaction.closeBrowser(this));
    }

    @Override public void setVisibility(boolean enable)
    {
	if(enable)
	{
	    interaction.enableGraphicalMode();
	    Utils.runInFxThreadSync(()->{
		    webView.setVisible(true);
		    webView.requestFocus();
		});
	    return;
	}
	interaction.disableGraphicalMode();
	Utils.runInFxThreadSync(()->webView.setVisible(false));
    }

    @Override public boolean getVisibility()
    {
	return webView.isVisible();//FIXME:
    }

    @Override public void loadByUrl(String url)
    {
	NullCheck.notNull(url, "url");
	    Utils.runInFxThreadSync(()->webEngine.load(url));
    }

    @Override public void loadByText(String text)
    {
	NullCheck.notNull(text, "text");
	    Utils.runInFxThreadSync(()->webEngine.loadContent(text));
    }

    @Override public boolean goHistoryPrev()
    {
	final Object res = Utils.callInFxThreadSync(()->{
		if (webEngine.getHistory().getCurrentIndex() <= 0)
		    return new Boolean(false);
		webEngine.getHistory().go(-1);
		return new Boolean(true);
	    });
	    if (res == null || !(res instanceof Boolean))
		return false;
	    return ((Boolean)res).booleanValue();
    }

    @Override public void stop()
    {
	Utils.runInFxThreadSync(()->webEngine.getLoadWorker().cancel());
    }

    @Override public String getTitle()
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

    @Override public org.luwrain.browser.BrowserIterator createIterator()
    {
	InvalidThreadException.checkThread("BrowserImpl.createIterator()");
	return new IteratorImpl(this);
    }

    @Override public int numElements()
    {
	if (domScanRes == null)
	    return 0;
	return domScanRes.dom.size();
    }
}
