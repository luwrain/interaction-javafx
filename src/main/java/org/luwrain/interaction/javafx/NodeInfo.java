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

import org.w3c.dom.Node;
import java.awt.Rectangle;

import org.luwrain.core.*;

final class NodeInfo
{
    static private final String LOG_COMPONENT = BrowserBase.LOG_COMPONENT;

    final Node node;
    Integer parent = null;
    Rectangle rect;
    boolean forText;
    int hash;
    long hashTime=0;

    NodeInfo(Node node,
int x, int y, int width, int height,
boolean forText)
    {
	NullCheck.notNull(node, "node");
	this.node = node;
	this.forText=forText;
	rect=new Rectangle(x,y,width,height);
    }

    Node getNode()
    {
    	return node;
    }

    boolean hasParent()
    {
	return parent != null;
    }

    Integer getParent()
    {
    	return parent;
    }

    void setParent(int val)
    {
    	parent=val;
    }

    boolean isVisible()
    {
	return rect.width > 0 && rect.height > 0;
    }

    void calcHash(String text)
    {
	hash=text.hashCode();
	hashTime=new java.util.Date().getTime();
    }

    String descr()
    {
    	String str=node.getNodeValue();
    	if(str==null) str="null";
	return node.getNodeName()+ "\tp:"+parent+" "+node.getClass().getSimpleName() + "\t" + str.substring(0,Math.min(160,str.length()))+"'";
	//return node.getNodeName() + " " + node.getNodeValue();
    }

    boolean getForText()
    {
	return forText;
    }

    Rectangle getRect()
    {
	return rect;
    }

    long getHashTime()
    {
	return hashTime;
    }

    int getHash()
    {
	return hash;
    }
}
