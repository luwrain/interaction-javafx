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
import java.util.concurrent.*;

import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.html.*;
import org.w3c.dom.*;
import javafx.application.Platform;

import org.luwrain.core.*;
import org.luwrain.browser.*;

class BrowserIteratorImpl implements BrowserIterator
{
    static private final String LOG_COMPONENT = JavaFxInteraction.LOG_COMPONENT;

    // javascript window's property names for using in executeScrypt
	static final String GET_NODE_TEXT="get_node_text";

    private final BrowserImpl browser;
    private int pos = 0;

    BrowserIteratorImpl(BrowserImpl browser)
    {
	NullCheck.notNull(browser, "browser");
	this.browser = browser;
    }

    @Override public BrowserIterator clone()
    {
    	final BrowserIteratorImpl result = new BrowserIteratorImpl(browser);
    	result.pos=pos;
    	return result;
    }

    @Override public boolean isVisible()
    {
	return current().isVisible();
    }

    @Override public boolean forTEXT()
    {
	return current().getForText();
    }

    @Override public int getPos()
    {
	return pos;
    }

    @Override public boolean setPos(int val)
    {
	pos=val;
	return true;
    }

    @Override public String getText()
    {
    	final String text;
    	if(current().getNode() instanceof Text)
	    text = current().getNode().getNodeValue().trim(); else
	    if(current().getNode() instanceof HTMLInputElement)
	    {
		final HTMLInputElement input=((HTMLInputElement)current().getNode());
		if(input.getType().equals("checkbox")
		   ||input.getType().equals("radio"))
		    text = input.getChecked()?"on":"off"; else
		    text = input.getValue();
	    } else
		if(current().getNode() instanceof HTMLSelectElement)
		{
		    final HTMLSelectElement select = (HTMLSelectElement)current().getNode();
		    final int index = select.getSelectedIndex();
		    text = select.getOptions().item(index).getTextContent();
		    // TODO: make multiselect support
		} else
		    text = getComputedText();
	return text != null?text:"";
    }

    @Override public String getAltText()
    {
    	String text="";
		if(current().getNode() instanceof HTMLAnchorElement
   		 ||current().getNode() instanceof HTMLImageElement
   		 ||current().getNode() instanceof HTMLInputElement
   		 ||current().getNode() instanceof HTMLTextAreaElement)
   		{ // title
   			if(current().getNode().hasAttributes())
   			{
   				Node title=current().getNode().getAttributes().getNamedItem("title");
   				if(title!=null)
   					text="title:"+title.getNodeValue();
   				Node alt=current().getNode().getAttributes().getNamedItem("alt");
   				if(alt!=null)
   					text=(!text.isEmpty()?" ":"")+"alt:"+alt.getNodeValue();
   				Node placeholder=current().getNode().getAttributes().getNamedItem("placeholder");
   				if(placeholder!=null)
   					text=(!text.isEmpty()?" ":"")+"alt:"+placeholder.getNodeValue();
   			}
   		}
		return text;
    }

    @Override public String[] getMultipleText()
    {
    	if(current().getNode() instanceof HTMLSelectElement)
    	{
	    HTMLSelectElement select=(HTMLSelectElement)current().getNode();
    		Vector<String> res=new Vector<String>();
    		for(int i=select.getLength();i>=0;i--)
 			{
 				Node option=select.getOptions().item(i);
 				if(option==null) continue; // so strange but happends
 				res.add(option.getTextContent());
 			}
    		return res.toArray(new String[res.size()]);
    	} else
    	{
    		return new String[]{getText()};
    	}
    }

    @Override public Rectangle getRect()
    {
    	if(!browser.domMap.containsKey(current().getNode())) 
	    return null;
    	final int pos = browser.domMap.get(current().getNode());
    	if(browser.getDOMList().size()<=pos) return null;
    	return browser.getDOMList().get(pos).getRect();
    }

    @Override public boolean isEditable()
    {
	if(current().getNode() instanceof HTMLInputElement)
	{
	    final String inputType = ((HTMLInputElement)current().getNode()).getType();
	    if(inputType.equals("button")
	     ||inputType.equals("inage")
	     ||inputType.equals("button")
	     ||inputType.equals("submit")) 
	   		return false;
	    // all other input types are editable
	    return true;
	} else
	    if(current().getNode() instanceof HTMLSelectElement)
	{
		return true;
	} else 
		if(current().getNode() instanceof HTMLTextAreaElement)
		return true; 
	return false;
    }

    @Override public void setText(String text)
    {
	if(current().getNode() instanceof HTMLInputElement)
		{
		    final HTMLInputElement input = ((HTMLInputElement)current().getNode());
			if(input.getType().equals("checkbox")
			 ||input.getType().equals("radio"))
			{
				input.setChecked(text.isEmpty()||text.equals("0")||text.equals("off")?false:true);
			} else
			{
				input.setValue(text);
			}
		} else
	    if(current().getNode() instanceof HTMLSelectElement)
		{
		    HTMLSelectElement select=(HTMLSelectElement)current().getNode();
			for(int i=select.getLength();i>=0;i--)
			{
				Node option=select.getOptions().item(i);
				if(option==null) continue; // so strange but happends
				//Log.debug("web",option.getNodeValue()+"["+i+"]="+option.getTextContent());
				// FIXME: make method to work with select option by index not by text value (not unique)
				if(option.getTextContent().equals(text))
				{
					select.setSelectedIndex(i);
					return;
				}
			}
		}
	if(current().getNode() instanceof HTMLTextAreaElement)
	    {
	    	((HTMLTextAreaElement)current().getNode()).setTextContent(text);
	    }
    }

    @Override public String getLink()
    {
	if(current().getNode() instanceof HTMLAnchorElement)
	    return getAttributeProperty("href"); else
	    if(current().getNode() instanceof HTMLImageElement)
		return getAttributeProperty("src");
	return "";
    }

    @Override public String getAttributeProperty(String name)
    {
	if(!current().getNode().hasAttributes()) 
	    return null;
	final Node attr=current().getNode().getAttributes().getNamedItem(name);
	if(attr==null) return null;
	return attr.getNodeValue();
    }

    private String getComputedAttributeAll()
    {
    	if(!current().getNode().hasAttributes()) 
    	    return "";
    	String res="";
    	for(int i=current().getNode().getAttributes().getLength()-1;i>=0;i--)
    	{
	    Node node = current().getNode().getAttributes().item(i);
    		res+=node.getNodeName()+"="+node.getNodeValue()+";";
    	}
    	return res;
    }

    @Override public String getComputedText()
    {
	final Object res = Utils.callInFxThreadSync(()->{
		browser.htmlWnd.setMember(GET_NODE_TEXT, current().getNode());
		if(!browser.domMap.containsKey(current().getNode())) 
		    return "";
		try{
		    return browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";return x.innerText===undefined?x.nodeValue:x.innerText})()").toString();
		}
		catch(Throwable e)
		{
		    Log.error(LOG_COMPONENT, "getting calculated text:" + e.getClass().getName() + ":" + e.getMessage());
		    return "";
		}
	    });
	return res != null?res.toString():"";
    }

    @Override public String getComputedStyleProperty(final String name)
    {
	Callable<String> task=new Callable<String>(){
	    @Override public String call()
	    {
	    	Node n=current().getNode();
	    	if(n instanceof com.sun.webkit.dom.HTMLDocumentImpl)
	    		return "";
	    	if(n instanceof Text)
	    	{ // we can't get style for simple text node, we need get it from parent tag
	    		n=n.getParentNode();
	    	}
			CSSStyleDeclaration style = browser.htmlWnd.getComputedStyle((HTMLElement)n,"");
			String css=style.getPropertyValue(name);
			return css;
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
    	try
    	{
    		Callable<String> task=new Callable<String>(){ @Override public String call()
		    {
		    	Node n=current().getNode();
		    	if(n instanceof com.sun.webkit.dom.HTMLDocumentImpl)
		    		return "";
		    	if(n instanceof Text)
		    	{ // we can't get style for simple text node, we need get it from parent tag
		    		n=n.getParentNode();
		    	}
				CSSStyleDeclaration style = browser.htmlWnd.getComputedStyle((HTMLElement)n, "");
				String css=style.getCssText();
				return css;
		    }};
		    if(Platform.isFxApplicationThread())
				 return task.call();
			FutureTask<String> query=new FutureTask<String>(task);
			Platform.runLater(query);
			return query.get();
    	} // FIXME: make better error handling
    	catch(Exception e)
    	{
    		//e.printStackTrace();
    		return "";
    	}
    }

    @Override public void submitEmulate()
    {
    	Platform.runLater(()->
    	{
	    	Node node=current().getNode();
	    	Node parent=null;
	    	while((parent=node.getParentNode())!=null)
	    	{
	    		if(node instanceof HTMLInputElement
	    		 ||node instanceof HTMLSelectElement)
	    		{
	    				    browser.htmlWnd.setMember(GET_NODE_TEXT, node);
	    				    try{
	    					browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.form.submit();})()");
	    				    }
	    				    catch(Exception e)
	    				    {
	    				    } // can't click - no reaction
	    			return;
	    		} else
	   			if(node instanceof HTMLFormElement)
	    		{
	    				    browser.htmlWnd.setMember(GET_NODE_TEXT, node);
	    				    try{
	    					browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.submit();})()");
	    				    }
	    				    catch(Exception e)
	    				    {
	    				    } // can't click - no reaction
	    			return;
	    		}
	    		node=parent;
	    	}
    	});
    }
    
    @Override public void clickEmulate()
    {
	// click can be done only for non text nodes
	final Node node=current().getNode();
	/*
	if(node instanceof HTMLInputElement)
	{
	    if(((HTMLInputElement)current().getNode()).getType().equals("submit"))
	    { // submit button
		Platform.runLater(new Runnable() {
			@Override public void run()
			{
			    browser.htmlWnd.setMember(GET_NODE_TEXT, node);
			    try{
				browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.form.submit();})()");
			    }
			    catch(Exception e)
			    {
			    } // can't click - no reaction
			}
		    });
		return;
	    };
	}
	*/
	Platform.runLater(new Runnable() {
		@Override public void run()
		{
			Node n=node;
			if(n.getNodeType()==Node.TEXT_NODE)
			{ // text node click sometimes does not work, move to parent
				n=n.getParentNode();
			}
		    browser.htmlWnd.setMember(GET_NODE_TEXT, n);
		    try{
			browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.click();})()");
		    }
		    catch(Exception e)
		    {
		    } // can't click - no reaction
		}
	    });
    }

    private String getHtml()
    {
    	if(current().getNode() instanceof Text)
    		return current().getNode().getNodeValue();
    	String xml="";
    	/*
    	try
    	{
    		Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(current().getNode());
			transformer.transform(source, result);
			xml=result.getWriter().toString();
    	} catch(TransformerException ex)
    	{
    		// FIXME:
    		ex.printStackTrace();
    	}
    	*/    	
    	xml=getText()+getComputedStyleAll()+getComputedAttributeAll();
    	//Log.debug("web","Node xml:"+xml);
    	return xml;
    }

	@Override public BrowserIterator getParent()
	{
	    if(browser.getDOMList().get(pos).getParent()==null)
			return null;
		BrowserIteratorImpl parent=new BrowserIteratorImpl(browser);
		parent.pos = browser.getDOMList().get(pos).getParent();
		return parent;
	}

	@Override public String getHtmlTagName()
	{
		return current().getNode().getNodeName();
	}

	@Override public Browser getBrowser()
	{
		return browser;
	}

    private NodeInfo current()
    {
	return browser.getDOMList().get(pos);
    }
}
