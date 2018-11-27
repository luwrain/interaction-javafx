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
    private int parentIndex = -1;
    private final Rectangle rect;

    NodeInfo(Node node,
int x, int y, int width, int height)
    {
	NullCheck.notNull(node, "node");
	this.node = node;
	this.rect=new Rectangle(x,y,width,height);
    }

    Node getNode()
    {
    	return node;
    }

    boolean hasParent()
    {
	return parentIndex >= 0;
    }

    int getParentIndex()
    {
    	return parentIndex;
    }

    void setParentIndex(int value)
    {
	if (value < 0)
	    throw new IllegalArgumentException("value (" + value + ") may not be negative");
	parentIndex = value;
    }

    Rectangle getRect()
    {
	return rect;
    }
}
