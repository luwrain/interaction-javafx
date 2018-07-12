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
import java.util.*;
import java.util.concurrent.*;

import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.html.*;
import org.w3c.dom.*;
import javafx.application.Platform;

import org.luwrain.core.*;
import org.luwrain.browser.*;

final class IteratorImpl implements BrowserIterator
{
    static private final String LOG_COMPONENT = JavaFxInteraction.LOG_COMPONENT;
    static final String GET_NODE_TEXT="get_node_text"; // javascript window's property names for using in executeScrypt

    private final BrowserImpl browser;
    private int pos = 0;

    IteratorImpl(BrowserImpl browser)
    {
	NullCheck.notNull(browser, "browser");
	this.browser = browser;
    }

    @Override public BrowserIterator clone()
    {
	InvalidThreadException.checkThread("BrowserImpl.clone()");
    	final IteratorImpl result = new IteratorImpl(browser);
    	result.pos=pos;
    	return result;
    }

    @Override public boolean isVisible()
    {
	InvalidThreadException.checkThread("BrowserImpl.isVisible()");
	return current().isVisible();
    }

    @Override public boolean forTEXT()
    {
	InvalidThreadException.checkThread("BrowserImpl.forTEXT()");
	return current().getForText();
    }

    @Override public int getPos()
    {
	InvalidThreadException.checkThread("BrowserImpl.getPos()");
	return pos;
    }

    @Override public boolean setPos(int value)
    {
	InvalidThreadException.checkThread("BrowserImpl.setPos()");
	pos = value;
	return true;
    }

    @Override public String getText()
    {
	InvalidThreadException.checkThread("BrowserImpl.getText()");
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
	InvalidThreadException.checkThread("BrowserImpl.getAltText()");
	String text = "";
	if(current().getNode() instanceof HTMLAnchorElement
	   ||current().getNode() instanceof HTMLImageElement
	   ||current().getNode() instanceof HTMLInputElement
	   ||current().getNode() instanceof HTMLTextAreaElement)
	{ // title
	    if(current().getNode().hasAttributes())
	    {
		final Node title = current().getNode().getAttributes().getNamedItem("title");
		if(title != null)
		    text = "title:"+title.getNodeValue();
		final Node alt = current().getNode().getAttributes().getNamedItem("alt");
		if(alt != null)
		    text = (!text.isEmpty()?" ":"")+"alt:"+alt.getNodeValue();
		final Node placeholder = current().getNode().getAttributes().getNamedItem("placeholder");
		if(placeholder != null)
		    text = (!text.isEmpty()?" ":"")+"alt:"+placeholder.getNodeValue();
	    }
	}
	return text;
    }

    @Override public String[] getMultipleText()
    {
	InvalidThreadException.checkThread("BrowserImpl.getMultipleText()");
    	if(current().getNode() instanceof HTMLSelectElement)
    	{
	    final HTMLSelectElement select = (HTMLSelectElement)current().getNode();
	    final LinkedList<String> res = new LinkedList<String>();
	    for(int i = select.getLength();i >= 0;i--)
	    {
		final Node option = select.getOptions().item(i);
		if(option == null)
		    continue; // so strange but happends
		res.add(option.getTextContent());
	    }
	    return res.toArray(new String[res.size()]);
    	}
	return new String[]{getText()};
    }

    @Override public Rectangle getRect()
    {
	InvalidThreadException.checkThread("BrowserImpl.getRect()");
	/*
    	if(!browser.domMap.containsKey(current().getNode())) 
	    return null;
    	final int pos = browser.domMap.get(current().getNode());
    	if(browser.dom.size()<=pos)
	    return null;
    	return browser.dom.get(pos).getRect();
	*/
	return null;
    }

    @Override public boolean isEditable()
    {
	InvalidThreadException.checkThread("BrowserImpl.isEditable()");
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
	NullCheck.notNull(text, "text");
	InvalidThreadException.checkThread("BrowserImpl.setText()");
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
	InvalidThreadException.checkThread("BrowserImpl.getLink()");
	if(current().getNode() instanceof HTMLAnchorElement)
	    return getAttribute("href"); else
	    if(current().getNode() instanceof HTMLImageElement)
		return getAttribute("src");
	return "";
    }

    @Override public String getAttribute(String name)
    {
	InvalidThreadException.checkThread("BrowserImpl.getAttributeProperty()");
	if(!current().getNode().hasAttributes()) 
	    return null;
	final Node attr = current().getNode().getAttributes().getNamedItem(name);
	if(attr == null)
	    return null;
	return attr.getNodeValue();
    }

    @Override public String getComputedText()
    {
	InvalidThreadException.checkThread("BrowserImpl.getComputedText()");
	/*
	browser.htmlWnd.setMember(GET_NODE_TEXT, current().getNode());
	if(!browser.domMap.containsKey(current().getNode())) 
	    return "";
	try{
final Object obj = browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";return x.innerText===undefined?x.nodeValue:x.innerText})()");
return obj != null?obj.toString():"";
	}
	catch(Throwable e)
	{
	    Log.error(LOG_COMPONENT, "getting calculated text:" + e.getClass().getName() + ":" + e.getMessage());
	    return "";
	}
	*/
	return "FIXME";
    }

    @Override public String getComputedStyleProperty(String name)
    {
	NullCheck.notEmpty(name, "name");
	InvalidThreadException.checkThread("BrowserImpl.getComputedStyleProperty()");
	/*
	Node n = current().getNode();
	if(n instanceof com.sun.webkit.dom.HTMLDocumentImpl)
	    return "";
	if(n instanceof Text)
	    n=n.getParentNode(); // we can't get style for simple text node, we need get it from parent tag
	final CSSStyleDeclaration style = browser.htmlWnd.getComputedStyle((HTMLElement)n,"");
	return style.getPropertyValue(name);
	*/
	return "FIXME";
    }

    @Override public String getComputedStyleAll()
    {
	InvalidThreadException.checkThread("BrowserImpl.getComputedStyleAll()");
	/*
	Node n = current().getNode();
	if(n instanceof com.sun.webkit.dom.HTMLDocumentImpl)
	    return "";
	if(n instanceof Text)
	    n = n.getParentNode(); // we can't get style for simple text node, we need get it from parent tag
	final CSSStyleDeclaration style = browser.htmlWnd.getComputedStyle((HTMLElement)n, "");
	return style.getCssText();
	*/
	return "FIXME";
    }

    @Override public void emulateSubmit()
    {
	InvalidThreadException.checkThread("BrowserImpl.emulateSubmit()");
	Node node = current().getNode();
	Node parent = null;
	while((parent=node.getParentNode()) != null)
	{
	    if(node instanceof HTMLInputElement
	       ||node instanceof HTMLSelectElement)
	    {
		//FIXME:		browser.htmlWnd.setMember(GET_NODE_TEXT, node);
		try{
		    browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.form.submit();})()");
		}
		catch(Throwable e)
		{
		    // If we are unable to emulate a submit, silently doing nothing
		    Log.debug(LOG_COMPONENT, "unable to emulate a submit:" + e.getClass().getName() + ":" + e.getMessage());
		}
		return;
	    }
	    if(node instanceof HTMLFormElement)
	    {
		//FIXME:		browser.htmlWnd.setMember(GET_NODE_TEXT, node);
		try{
		    browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.submit();})()");
		}
		catch(Throwable e)
		{
		    // If we are unable to emulate a submit, silently doing nothing
		    Log.debug(LOG_COMPONENT, "unable to emulate a submit:" + e.getClass().getName() + ":" + e.getMessage());
		}
		return;
	    }
	    node = parent;
	}
    }


    @Override public void emulateClick()
    {
	InvalidThreadException.checkThread("BrowserImpl.clickEmulate()");
	Node node = current().getNode();
	if(node.getNodeType() == Node.TEXT_NODE)
	    node = node.getParentNode(); // text node click sometimes does not work, move to parent
	//FIXME:	browser.htmlWnd.setMember(GET_NODE_TEXT, node);
	try{
	    browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.click();})()");
	}
	catch(Throwable e)
	{
	    //If we are unable to emulate a click , silently doing nothing
	    Log.debug(LOG_COMPONENT, "unable to emulate a click:" + e.getClass().getName() + ":" + e.getMessage());
	} 
    }

    @Override public boolean isParent(BrowserIterator it)
    {
	NullCheck.notNull(it, "it");
	InvalidThreadException.checkThread("BrowserImpl.isParent()");
	final BrowserIterator parent = getParent();
	if (parent == null)
	    return false;
	return pos == parent.getPos();
    }

    @Override public boolean hasParent()
    {
	InvalidThreadException.checkThread("BrowserImpl.hasParent()");
	return getParent() != null;
    }

    @Override public BrowserIterator getParent()
    {
	InvalidThreadException.checkThread("BrowserImpl.getParent()");
	/*
	if(current().getParent() == null)
	    return null;
	final BrowserIteratorImpl parent = new BrowserIteratorImpl(browser);
	parent.pos = browser.dom.get(pos).getParent();//FIXME:
	return parent;
	*/
	return null;
    }

    @Override public String getHtmlTagName()
    {
	InvalidThreadException.checkThread("BrowserImpl.getHtmlTagName()");
	return current().getNode().getNodeName();
    }

    @Override public Browser getBrowser()
    {
	InvalidThreadException.checkThread("BrowserImpl.getBrowser()");
	return browser;
    }

    private String getComputedAttributeAll()
    {
	InvalidThreadException.checkThread("BrowserImpl.getComputedAttributeAll()");
    	if(!current().getNode().hasAttributes()) 
    	    return "";
    	String res = "";
    	for(int i=current().getNode().getAttributes().getLength()-1;i >= 0;i--)
    	{
	    final Node node = current().getNode().getAttributes().item(i);
	    res += node.getNodeName()+"="+node.getNodeValue()+";";
    	}
    	return res;
    }

    private String getHtml()
    {
	InvalidThreadException.checkThread("BrowserImpl.getHtml()");
    	if(current().getNode() instanceof Text)
	    return current().getNode().getNodeValue();
    	String xml = getText() + getComputedStyleAll() + getComputedAttributeAll();
    	return xml;
    }

    private NodeInfo current()
    {
	/*
	return browser.dom.get(pos);
	*/
	return null;
    }
}
