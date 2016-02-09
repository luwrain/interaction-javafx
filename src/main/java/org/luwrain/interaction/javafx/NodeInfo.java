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

class NodeInfo
{
    org.w3c.dom.Node node;
    java.awt.Rectangle rect;
    boolean forTEXT;
    int hash;
    long hashTime=0;

    public boolean isVisible()
    {
	return rect.width>0&&rect.height>0;
    }

    void calcHash(String text)
    {
	hash=text.hashCode();
	hashTime=new java.util.Date().getTime();
    }
}
