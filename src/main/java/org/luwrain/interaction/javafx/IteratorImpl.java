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

import org.luwrain.core.*;
import org.luwrain.browser.*;

final class IteratorImpl implements BrowserIterator
{
    static private final String LOG_COMPONENT = BrowserBase.LOG_COMPONENT;

    private final BrowserBase browser;
    private int pos;

    //Set by prepare() function
    private DomScanResult scanRes = null;
    private List<NodeInfo> dom = null;
    private NodeInfo nodeInfo = null;

    IteratorImpl(BrowserBase browser, int pos)
    {
	NullCheck.notNull(browser, "browser");
	if (pos < 0)
	    throw new IllegalArgumentException("pos (" + pos + ") may not be negative");
	this.browser = browser;
	this.pos = pos;
    }

    IteratorImpl (BrowserBase browser)
    {
	this(browser, 0);
    }

    @Override public int getPos()
    {
	return pos;
    }

    @Override public boolean setPos(int value)
    {
	prepare("BrowserImpl.setPos()");
	if (value < 0 || value >= scanRes.dom.size())
	    throw new IndexOutOfBoundsException("value (" + value + ") must be non-negative and less than " + scanRes.dom.size());
	this.pos = value;
	return true;
    }

    @Override public BrowserIterator clone()
    {
	return new IteratorImpl(browser, pos);
    }

    @Override public String getText()
    {
	prepare("BrowserImpl.getText()");
    	if(nodeInfo.getNode() instanceof Text)
	{
	    final String text = nodeInfo.getNode().getNodeValue().trim();
	    return text != null?text:"";
	}
	if(nodeInfo.getNode() instanceof HTMLInputElement)
	{
	    final HTMLInputElement input=((HTMLInputElement)nodeInfo.getNode());
	    final String text;
	    if(input.getType().equals("checkbox") ||
	       input.getType().equals("radio"))
		text = input.getChecked()?"on":"off"; else
		text = input.getValue();
	    return text != null?text:"";
	}
	if(nodeInfo.getNode() instanceof HTMLSelectElement)
	{
	    final HTMLSelectElement select = (HTMLSelectElement)nodeInfo.getNode();
	    final int index = select.getSelectedIndex();
	    final String text = select.getOptions().item(index).getTextContent();
	    // TODO: make multiselect support
	    return text != null?text:"";
	}
	final String text = getComputedText();
	return text != null?text:"";
    }

    @Override public String getAltText()
    {
	prepare("BrowserImpl.getAltText()");
	String text = "";
	if(nodeInfo.getNode() instanceof HTMLAnchorElement ||
	   nodeInfo.getNode() instanceof HTMLImageElement ||
	   nodeInfo.getNode() instanceof HTMLInputElement ||
	   nodeInfo.getNode() instanceof HTMLTextAreaElement)
	{ // title
	    if(nodeInfo.getNode().hasAttributes())
	    {
		final Node title = nodeInfo.getNode().getAttributes().getNamedItem("title");
		if(title != null)
		    text = "title:" + title.getNodeValue();
		final Node alt = nodeInfo.getNode().getAttributes().getNamedItem("alt");
		if(alt != null)
		    text = (!text.isEmpty()?" ":"")+"alt:"+alt.getNodeValue();
		final Node placeholder = nodeInfo.getNode().getAttributes().getNamedItem("placeholder");
		if(placeholder != null)
		    text = (!text.isEmpty()?" ":"")+"alt:"+placeholder.getNodeValue();
	    }
	}
	return text;
    }

    @Override public String[] getMultipleText()
    {
	prepare("BrowserImpl.getMultipleText()");
    	if(nodeInfo.getNode() instanceof HTMLSelectElement)
    	{
	    final HTMLSelectElement select = (HTMLSelectElement)nodeInfo.getNode();
	    final List<String> res = new LinkedList();
	    for(int i = select.getLength() - 1;i >= 0;i--)
	    {
		final Node option = select.getOptions().item(i);
		if(option == null)
		    continue; // so strange but happens
		res.add(option.getTextContent());
	    }
	    return res.toArray(new String[res.size()]);
    	}
	return new String[]{getText()};
    }

    @Override public Rectangle getRect()
    {
	prepare("BrowserImpl.getRect()");
	return nodeInfo.getRect();
    }

    @Override public boolean isEditable()
    {
	prepare("BrowserImpl.isEditable()");
	if(nodeInfo.getNode() instanceof HTMLInputElement)
	{
	    final String inputType = ((HTMLInputElement)nodeInfo.getNode()).getType();
	    switch(inputType.toLowerCase().trim())
	    {
	    case "button":
	    case "inage":
	    case "submit":
		return false;
	    default:
		// all other input types are editable
		return true;
	    }
	}
	if(nodeInfo.getNode() instanceof HTMLSelectElement)
	    return true;
	if(nodeInfo.getNode() instanceof HTMLTextAreaElement)
	    return true; 
	return false;
    }

    @Override public void setText(String text)
    {
	NullCheck.notNull(text, "text");
	prepare("IteratorImpl.setText()");
	if(nodeInfo.getNode() instanceof HTMLInputElement)
	{
	    final HTMLInputElement input = ((HTMLInputElement)nodeInfo.getNode());
	    if(input.getType().equals("checkbox") ||
	       input.getType().equals("radio"))
	    {
		input.setChecked(text.isEmpty()||text.equals("0")||text.equals("off")?false:true);
		return;
	    }
	    input.setValue(text);
	    return;
	} //HTMLInputElement
	if(nodeInfo.getNode() instanceof HTMLSelectElement)
	{
	    final HTMLSelectElement select=(HTMLSelectElement)nodeInfo.getNode();
	    for(int i = select.getLength();i>=0;i--)
	    {
		Node option=select.getOptions().item(i);
		if(option == null)
		    continue; // very strange, but happens
		// FIXME: make method to work with select option by index not by text value (not unique)
		if(option.getTextContent().equals(text))
		{
		    select.setSelectedIndex(i);
		    return;
		}
	    }
	    return;
	} //HTMLSelectElement
	if(nodeInfo.getNode() instanceof HTMLTextAreaElement)
	{
	    ((HTMLTextAreaElement)nodeInfo.getNode()).setTextContent(text);
	    return;
	} //HTMLTextAreaElement
    }

    @Override public String getClassName()
    {
	prepare("IteratorImpl.getClassName()");
	String className = nodeInfo.getNode().getClass().getName();
	final int dotPos = className.lastIndexOf(".");
	if (dotPos >= 0 && dotPos + 1 < className.length())
	    className = className.substring(dotPos + 1);
	if (className == null)
	    className = "";
	if (className.toLowerCase().startsWith("html"))
	    className = className.substring(4);
	if (className.toLowerCase().endsWith("impl"))
	    className = className.substring(0, className.length() - 4);
	if (className.toLowerCase().endsWith("element"))
	    className = className.substring(0, className.length() - 7);
	return className;
    }

    @Override public boolean isInput()
    {
		prepare("IteratorImpl.isInput()");
		return nodeInfo.getNode() instanceof HTMLInputElement;
    }

    @Override public String getInputType()
    {
	prepare("IteratorImpl.getInputType()");
	if (!(nodeInfo.getNode() instanceof HTMLInputElement))
	    return "";
	    final HTMLInputElement input = ((HTMLInputElement)nodeInfo.getNode());
	    final String res = input.getType();
	    return res != null?res:"";
    }

        @Override public boolean setInputText(String text)
    {
	NullCheck.notNull(text, "text");
	prepare("IteratorImpl.setInputText()");
	if(!(nodeInfo.getNode() instanceof HTMLInputElement))
	    return false;
	    	    final HTMLInputElement input = ((HTMLInputElement)nodeInfo.getNode());
    input.setValue(text);
    return true;
    }

    @Override public String getLink()
    {
	prepare("BrowserImpl.getLink()");
	if(nodeInfo.getNode() instanceof HTMLAnchorElement)
	    return getAttr("href"); else
	    if(nodeInfo.getNode() instanceof HTMLImageElement)
		return getAttr("src");
	return "";
    }

    @Override public String getAttr(String name)
    {
	prepare("IteratorImpl.getAttr()");
	if(!nodeInfo.getNode().hasAttributes()) 
	    return null;
	final Node attr = nodeInfo.getNode().getAttributes().getNamedItem(name);
	if(attr == null)
	    return null;
	return attr.getNodeValue();
    }

    @Override public Map<String, String> getAttrs()
    {
	prepare("BrowserImpl.getAttrs()");
	if(!nodeInfo.getNode().hasAttributes()) 
	    return new HashMap();
	final NamedNodeMap attrs = nodeInfo.getNode().getAttributes();
	final HashMap<String, String> res = new HashMap();
	for(int i = 0;i < attrs.getLength();i++)
	{
	    final Node node = attrs.item(i);
	    if (node == null)
		continue;
	    final String name = node.getNodeName();
	    if (name == null || name.isEmpty())
		continue;
	    final String value = node.getNodeValue();
	    res.put(name, value != null?value:"");
	}
	return res;
    }


    @Override public String getComputedText()
    {
	prepare("BrowserImpl.getComputedText()");
	if(!scanRes.domMap.containsKey(nodeInfo.getNode())) 
	    return "";
	try{
	    final Object obj = executeScriptWithNode(nodeInfo.getNode(), "(function(){var x=window.LUWRAIN_OBJ;return x.innerText===undefined?x.nodeValue:x.innerText})()");
	    return obj != null?obj.toString():"";
	}
	catch(Throwable e)
	{
	    Log.error(LOG_COMPONENT, "getting calculated text:" + e.getClass().getName() + ":" + e.getMessage());
	    return "";
	}
    }

    @Override public String getComputedStyleProperty(String name)
    {
	NullCheck.notEmpty(name, "name");
	prepare("BrowserImpl.getComputedStyleProperty()");
	if(nodeInfo.getNode() instanceof com.sun.webkit.dom.HTMLDocumentImpl)
	    return "";
	final Node node = findNonTextNode(nodeInfo);
	final CSSStyleDeclaration style = scanRes.window.getComputedStyle((HTMLElement)node, "");
	return style.getPropertyValue(name);
    }

    @Override public String getComputedStyleAll()
    {
	prepare("BrowserImpl.getComputedStyleAll()");
	if(nodeInfo.getNode() instanceof com.sun.webkit.dom.HTMLDocumentImpl)
	    return "";
	final Node node = findNonTextNode(nodeInfo);
	if (node == null)
	    return "";
	final CSSStyleDeclaration style = scanRes.window.getComputedStyle((HTMLElement)node, "");
	return style.getCssText();
    }

    @Override public void emulateSubmit()
    {
	prepare("BrowserImpl.emulateSubmit()");
	Node node = findNonTextNode(nodeInfo);
	while(node != null)
	{
	    if(node instanceof HTMLInputElement ||
	       node instanceof HTMLSelectElement)
	    {
		try{
		    executeScriptWithNode(node, "(function(){var x=window.LUWRAIN_OBJ;x.form.submit();})()");
		}
		catch(Throwable e)
		{
		    Log.debug(LOG_COMPONENT, "unable to emulate submit:" + e.getClass().getName() + ":" + e.getMessage());
		}
		return;
	    }
	    if(node instanceof HTMLFormElement)
	    {
		try{
		    executeScriptWithNode(node, "(function(){var x=window.LUWRAIN_OBJ;x.submit();})()");
		}
		catch(Throwable e)
		{
		    Log.debug(LOG_COMPONENT, "unable to emulate submit:" + e.getClass().getName() + ":" + e.getMessage());
		}
		return;
	    }
	    node = node.getParentNode();
	}
    }

    @Override public void emulateClick()
    {
	prepare("IteratorImpl.emulateClick()");
	final Node node = findNonTextNode(nodeInfo);
	try {
	    executeScriptWithNode(node, "(function(){var x=window.LUWRAIN_OBJ; x.click();})()");
	}
	catch(Throwable e)
	{
	    Log.debug(LOG_COMPONENT, "unable to emulate click:" + e.getClass().getName() + ":" + e.getMessage());
	} 
    }

    @Override public boolean isParent(BrowserIterator it)
    {
	NullCheck.notNull(it, "it");
	prepare("BrowserImpl.isParent()");
	final BrowserIterator parent = getParent();
	if (parent == null)
	    return false;
	return it.getPos() == parent.getPos();
    }

    @Override public boolean hasParent()
    {
	prepare("BrowserImpl.hasParent()");
	return nodeInfo.hasParent();
    }

    @Override public BrowserIterator getParent()
    {
	prepare("BrowserImpl.getParent()");
	if(!nodeInfo.hasParent())
	    return null;
	return new IteratorImpl(browser, nodeInfo.getParentIndex());
    }

    @Override public String getTagName()
    {
	prepare("IteratorImpl.getTagName()");
final String res = nodeInfo.getNode().getNodeName();
return res != null?res:"";
    }

    @Override public Browser getBrowser()
    {
	return (BrowserImpl)browser;
    }

    private Node findNonTextNode(NodeInfo info)
    {
	NullCheck.notNull(info, "info");
	Node node = info.getNode();
	while (node != null && node.getNodeType() == Node.TEXT_NODE)
	    node = node.getParentNode(); // text node click sometimes does not work, move to parent
	return node;
    }

    private Object executeScriptWithNode(Node node, String scriptText)
    {
	NullCheck.notNull(node, "node");
	NullCheck.notNull(scriptText, "scriptText");
	if (scriptText.isEmpty())
	    return null;
	final String tmpObjName = "luwrain_tmp_obj";
	scanRes.window.setMember(tmpObjName, node);
	return browser.executeScript(scriptText.replaceAll("LUWRAIN_OBJ", tmpObjName));
    }

    private void prepare(String funcName)
    {
	NullCheck.notEmpty(funcName, "funcName");
	InvalidThreadException.checkThread(funcName);
	this.scanRes = browser.getDomScanResult();
	if (scanRes == null)
	    throw new RuntimeException(funcName + ": No scan result in the browser, it means that there were no rescanDom() calls");
	this.dom = scanRes.dom;
	if (pos >= dom.size())
	    throw new RuntimeException(funcName + ": the internal index points outside of the DOM, it means there could be rescanDom() calls and thsi iterator is no longer actual");
	this.nodeInfo = dom.get(pos);
    }
}
