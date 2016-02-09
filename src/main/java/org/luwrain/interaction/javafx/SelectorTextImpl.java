/*
   Copyright 2015-2016 Roman Volovodov <gr.rPman@gmail.com>
   Copyright 2012-2016 Michael Pozhidaev <michael.pozhidaev@gmail.com>

   This file is part of the LUWRAIN.

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

import org.w3c.dom.Node;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.html.*;

import com.sun.webkit.dom.*;

import org.luwrain.browser.*;	// select filter for any text container element's, text filtered by text filter as substring

// null string threat as any values
// TODO: make RegEx support in filter
class SelectorTextImpl extends SelectorAllImpl implements SelectorText
{
    String filter;

    SelectorTextImpl(boolean visible,String filter)
    {
	super(visible);
	this.filter=filter;
    }

    @Override public String getFilter()
    {
	return filter;
    }

    @Override public void setFilter(String filter)
    {
	this.filter=filter;
    }

    @Override public boolean suits(ElementIterator wel_)
    {
	final ElementIteratorImpl wel = (ElementIteratorImpl)wel_;
	if(wel.page.dom.size()<=wel.pos||wel.pos<0) return false;
	//	wel.current = wel.page.dom.get(wel.pos);
	if(visible&&!checkVisible(wel)) 
	    return false;
	// current selector's checks
	if(!wel.current().forTEXT) 
return false;
	String text=wel.getText(); // TODO: if filter is null, we can skip getText for each node in list to speed up walking but consume empty text nodes
	//System.out.println("CHECK: node:"+wel.current.node.getNodeName()+", "+(!(wel.current.node instanceof HTMLElement)?wel.current.node.getNodeValue():((HTMLElement)wel.current.node).getTextContent())); // +" text:"+info.forTEXT+);
	if(text==null) 
	    text="";else 
	    text=text.trim();
	if(!(wel.current().node instanceof HTMLAnchorElement)
	   &&!(wel.current().node instanceof HTMLInputElement)
	   &&!(wel.current().node instanceof HTMLButtonElement)
	   //&&!(wel.current.node.getAttributes().getNamedItem("onclick")==null)
	   &&text.isEmpty()) 
	    return false;
	if(filter!=null&&text.toLowerCase().indexOf(filter)==-1) return false;
	//System.out.println("CHECK: ok");
	return true;
    }
}
