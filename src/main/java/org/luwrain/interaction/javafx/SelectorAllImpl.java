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

import org.luwrain.browser.*;

/** The selector for iteration over all elements on the page*/
class SelectorAllImpl extends SelectorImpl implements SelectorAll
{
    /** Consider only visible elements of the page*/
    protected boolean visible;

    SelectorAllImpl(boolean visible)
    {
	this.visible=visible;
    }

    @Override public boolean isVisible()
    {
	return visible;	
    }

    @Override public void setVisible(boolean visible)	
    {
	this.visible=visible;
    }

    // return true if current element is visible
    @Override public boolean checkVisible(ElementIterator it)
    {
	final ElementIteratorImpl itImpl = (ElementIteratorImpl)it;
	return itImpl.current().isVisible();
    }

    /** return true if current element suits the condition of this selector.*/
    @Override public boolean suits(ElementIterator it)
    {
	final ElementIteratorImpl itImpl = (ElementIteratorImpl)it;
	if(visible&&!checkVisible(itImpl))
	    return false;
	return true;
    }
}
