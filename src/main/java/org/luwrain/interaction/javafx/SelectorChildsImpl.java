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

import org.luwrain.browser.*;	// select filter for any text container element's, text filtered by text filter as substring

// null string threat as any values
// TODO: make RegEx support in filter
class SelectorChildsImpl extends SelectorAllImpl implements SelectorChilds
{
	Integer[] childs=new Integer[0];
	int idx=0;
	
    SelectorChildsImpl(boolean visible,Integer[] childs)
    {
	super(visible);
	this.childs=childs;
    }
    
    @Override public void setChildsList(Integer[] childs)
    {
    	this.childs=childs;
    }

    @Override public boolean suits(ElementIterator wel_)
    { // ... we never call this method, i think
    	return false;
    }
    public boolean moveFirst(ElementIterator it)
    {
    	final ElementIteratorImpl itImpl = (ElementIteratorImpl)it;
    	if(childs.length==0) return false;
    	idx=0;
    	itImpl.pos=childs[0];
    	return true;
    }
    public boolean moveNext(ElementIterator it)
    {
    	final ElementIteratorImpl itImpl = (ElementIteratorImpl)it;
    	if(idx+1>=childs.length) return false;
    	idx++;
    	itImpl.pos=childs[idx];
    	return true;
    }
    public boolean movePrev(ElementIterator it)
    {
    	final ElementIteratorImpl itImpl = (ElementIteratorImpl)it;
    	if(idx<=0) return false;
    	idx--;
    	itImpl.pos=childs[idx];
    	return true;
    }
    public boolean moveToPos(ElementIterator it, int pos)
    {
    	final ElementIteratorImpl itImpl = (ElementIteratorImpl)it;
    	for(int i:childs)
    	{
    		if(childs[i]==pos)
   			{
    			idx=i;
    			itImpl.pos=childs[idx];
    			return true;
   			}
    	}
    	return false;
    }

	@Override public int getChildsCount()
	{
		return childs.length;
	}
}
