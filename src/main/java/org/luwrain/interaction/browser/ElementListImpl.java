package org.luwrain.interaction.browser;

import java.awt.Rectangle;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javafx.application.Platform;

import org.w3c.dom.Node;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.html.*;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.sun.webkit.dom.*;
import org.w3c.dom.*;

import org.luwrain.browser.ElementList;
import org.luwrain.core.Log;
import org.luwrain.interaction.browser.WebPage.NodeInfo;

class ElementListImpl implements ElementList
{
    // javascript window's property names for using in executeScrypt
    static final String GET_NODE_TEXT="get_node_text";

    WebPage page;
    int pos=0;
    WebPage.NodeInfo current;

    ElementListImpl(WebPage page)
    {
	this.page=page;
    }

    @Override public int getPos()
    {
	return pos;
    }

    @Override public String getType()
    {
	if(current.node instanceof org.w3c.dom.Text)
	    return "text"; else 
	    if(current.node instanceof HTMLInputElement)
	    {
		try {
		    return "input "+current.node.getAttributes().getNamedItem("type").getNodeValue();
		}
		catch(Exception e) 
		{
		    return "input";
		}
	    } else
		if(current.node instanceof HTMLButtonElement)
		{
		    return "button";
		} else
		    if(current.node instanceof HTMLAnchorElement)
		    {
			return "link";
		    } else
			if(current.node instanceof HTMLImageElement)
			{
			    return "image";
			} else
			{
			    return "other text";
			}
    }

    @Override public String getText()
    {
    	if(current.node instanceof Text)
    		return current.node.getNodeValue().trim(); else 
	    if(current.node instanceof HTMLInputElement)
	    { // input element
	    	return ((HTMLInputElement)current.node).getValue();
	    } else
	    { // any other element
	    	return getComputedText();
	    }
    }
    @Override public Rectangle getRect()
    {
    	if(!page.domIdx.containsKey(current.node)) return null;
    	int pos=page.domIdx.get(current.node);
    	if(page.dom.size()<=pos) return null;
    	return page.dom.get(pos).rect;
    }

    @Override public boolean isEditable()
    {
	if(current.node instanceof HTMLInputElement)
	{
	    if(((HTMLInputElement)current.node).getType().equals("text")) 
		return true;
	} else 
	    if(current.node instanceof HTMLTextAreaElement)
		return true; 
	return false;
    }

    @Override public void setText(String text)
    {
    	Log.debug("web","setText: "+current.node.getClass().getSimpleName()+", rect:"+page.dom.get(page.domIdx.get(current.node)).rect);
	if(current.node instanceof HTMLInputElement)
	{
	    if(((HTMLInputElement)current.node).getType().equals("text"))
	    {
	    	((HTMLInputElement)current.node).setValue(text);
	    }
	} else 
	    if(current.node instanceof HTMLTextAreaElement)
	    {
	    	((HTMLTextAreaElement)current.node).setTextContent(text);
	    }
    }

    @Override public String getLink()
    {
	if(current.node instanceof HTMLAnchorElement)
	    return getAttributeProperty("href"); else
	    if(current.node instanceof HTMLImageElement)
		return getAttributeProperty("src");
	return "";
    }

    @Override public String getAttributeProperty(String name)
    {
	if(!current.node.hasAttributes()) 
	    return null;
	final Node attr=current.node.getAttributes().getNamedItem(name);
	return attr.getNodeValue();
    }

    @Override public String getComputedText()
    {
	Callable<String> task=new Callable<String>(){
	    @Override public String call()
	    {
		page.htmlWnd.setMember(GET_NODE_TEXT, current.node);
		try{
		    return page.webEngine.executeScript("(function(){var x=window."+GET_NODE_TEXT+";return x.innerText===undefined?x.nodeValue:x.innerText})()").toString();
		}
		catch(Exception e)
		{
		    e.printStackTrace();
		    return "";
		}
	    }};
	if(Platform.isFxApplicationThread()) 
	    try{
		return task.call();}
	    catch
	    (Exception e)
	    {
		return null;
	    }
	FutureTask<String> query=new FutureTask<String>(task);
	Platform.runLater(query);
	try{
	    return query.get();
	}
	catch(Exception e)
	{
	    return null;
	}
	// FIXME: make better error handling
    }

    @Override public String getComputedStyleProperty(final String name)
    {
	Callable<String> task=new Callable<String>(){
	    @Override public String call()
	    {
		CSSStyleDeclaration style = page.htmlWnd.getComputedStyle((HTMLElement)current.node, "");
		return style.getPropertyValue(name);
	    }};
	if(Platform.isFxApplicationThread()) 
	    try{
		return task.call();}
	    catch(Exception e)
	    {
		return null;
	    }
	FutureTask<String> query=new FutureTask<String>(task);
	Platform.runLater(query);
	try{
	    return query.get();}
	catch(Exception e)
	{
	    return null;
	}
	// FIXME: make better error handling
    }

    @Override public String getComputedStyleAll()
    {
	Callable<String> task=new Callable<String>(){
	    @Override public String call()
	    {
		CSSStyleDeclaration style = page.htmlWnd.getComputedStyle((HTMLElement)current.node, "");
		return style.getCssText();
	    }};
	if(Platform.isFxApplicationThread()) try
					     {
						 return task.call();
					     }
	    catch(Exception e)
	    {
		return null;
	    }
	FutureTask<String> query=new FutureTask<String>(task);
	Platform.runLater(query);
	try{
	    return query.get();
	}
	catch(Exception e)
	{
	    return null;
	}
	// FIXME: make better error handling
    }

    @Override public void clickEmulate()
    {
	// click can be done only for non text nodes
	// FIXME: emulate click for text nodex via parent node
	if(current.node instanceof HTMLInputElement)
	{
	    if(((HTMLInputElement)current.node).getType().equals("submit"))
	    { // submit button
		Platform.runLater(new Runnable() {
			@Override public void run()
			{
			    page.htmlWnd.setMember(GET_NODE_TEXT, current.node);
			    try{
				page.webEngine.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.form.submit();})()");
			    }
			    catch(Exception e)
			    {
			    } // can't click - no reaction
			}
		    });
		return;
	    };
	}
	Platform.runLater(new Runnable() {
		@Override public void run()
		{
		    page.htmlWnd.setMember(GET_NODE_TEXT, current.node);
		    try{
			page.webEngine.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.click();})()");
		    }
		    catch(Exception e)
		    {
		    } // can't click - no reaction
		}
	    });
    }

    private String getHtml()
    {
    	if(current.node instanceof Text)
    		return current.node.getNodeValue();

    	String xml="";
    	/*
    	try
    	{
    		Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(current.node);
			transformer.transform(source, result);
			xml=result.getWriter().toString();
    	} catch(TransformerException ex)
    	{
    		// FIXME:
    		ex.printStackTrace();
    	}
    	*/    	
    	xml=getText()+getComputedStyleAll();
    	//Log.debug("web","Node xml:"+xml);
    	return xml;
    }
    
    public boolean isChanged()
	{
		NodeInfo info=page.dom.get(pos);
		if(info.hashTime==0)
		{ // need to get hash
			info.calcHash(getHtml());
			// first time changes are undefined
			return false;
		}
		// last changes already scanned
		int oldHash=info.hash;
		info.calcHash(getHtml());
		//System.out.println("["+pos+":"+current.node.getClass().getSimpleName()+":"+info.hash+"]");
		//if(oldHash!=info.hash) System.out.println("changes detected");
		return oldHash!=info.hash;
	}

	@Override public boolean isChangedAround(Selector selector,int pos,int count)
	{
    	int cnt;
    	selector.to(this,pos);
    	// step cnt elements before
    	cnt=count;
    	while(selector.prev(this)&&cnt-->0)
	    	if(this.isChanged())
	    		return true;
    	cnt=count;
    	while(selector.next(this)&&cnt-->0)
	    	if(this.isChanged())
	    		return true;
		return false;
	}
}
