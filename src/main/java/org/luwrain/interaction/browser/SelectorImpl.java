/*
   Copyright 2015 Roman Volovodov <gr.rPman@gmail.com>
   Copyright 2012-2015 Michael Pozhidaev <michael.pozhidaev@gmail.com>

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

package org.luwrain.interaction.browser;

import org.luwrain.browser.*;

/** General navigation operations based on the behaviour of {code: suits()} method*/
abstract class SelectorImpl implements Selector
{
    /** Moves the iterator to the first element, approved by the {@code: suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param it The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    @Override public boolean moveFirst(ElementList it)
    {
	final ElementListImpl itImpl = (ElementListImpl)it;
	final int origState = itImpl.pos;
	final int count = itImpl.page.numElements();
	itImpl.pos = 0;
	while(itImpl.pos < count && !suits(itImpl)) 
	    ++itImpl.pos;
	if(itImpl.pos >= count)
	{
	    itImpl.pos = origState;
	    return false;
	}
	return true;
    }

    /** Moves the iterator to the next element, approved by the {@code: suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param it The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    @Override public boolean moveNext(ElementList it)
    {
	final ElementListImpl itImpl = (ElementListImpl)it;
	final int origState = itImpl.pos;
	final int count = itImpl.page.numElements();
	++itImpl.pos;
	while(itImpl.pos < count && !suits(itImpl)) 
	    ++itImpl.pos;
	if(itImpl.pos >= count)
	{
	    itImpl.pos = origState;
	    return false;
	}
	return true;
    }

    /** Moves the iterator to the previous element, approved by the {@code: suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param it The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    @Override public boolean movePrev(ElementList it)
    {
	final ElementListImpl itImpl = (ElementListImpl)it;
	final int origState = itImpl.pos;
	itImpl.pos--;
	while(itImpl.pos >= 0 && !suits(itImpl)) 
	    --itImpl.pos;
	if(itImpl.pos<0)
	{
	    itImpl.pos = origState;
	    return false;
	}
	return true;
    }

    @Override public boolean moveToPos(ElementList it, int pos)
    {
	if(it.getPos() == pos)
	    return true; else 
	    if(it.getPos() < pos)
	    {
		while(moveNext(it)) 
		    if(pos == it.getPos()) 
			return true;
		return false;
	    } else
	    {
		while(movePrev(it)) 
		    if(pos == it.getPos()) 
			return true;
		return false;
	    }
    }
}
