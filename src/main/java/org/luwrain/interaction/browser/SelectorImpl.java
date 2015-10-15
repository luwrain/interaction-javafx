
package org.luwrain.interaction.browser;

import org.luwrain.browser.*;

abstract class SelectorImpl implements ElementList.Selector
{
    @Override public abstract boolean check(ElementList wel);

    @Override public boolean first(ElementList wel_)
    {
	final ElementListImpl wel=(ElementListImpl)wel_;
	wel.pos=0;
	while(wel.pos<wel.page.domIdx.size()&&!check(wel)) 
	    ++wel.pos;
	if(wel.pos>=wel.page.domIdx.size())
	{
	    wel.pos=0;
	    return false;
	}
	return true;
    }

    @Override public boolean next(ElementList wel_)
    {
	ElementListImpl wel=(ElementListImpl)wel_;
	int prev=wel.pos;
	++wel.pos;
	while(wel.pos<wel.page.domIdx.size()&&!check(wel)) 
	    ++wel.pos;
	if(wel.pos>=wel.page.domIdx.size())
	{
	    wel.pos=prev;
	    return false;
	}
	return true;
    }

    @Override public boolean prev(ElementList wel_)
    {
	final ElementListImpl wel=(ElementListImpl)wel_;
	int prev=wel.pos;
	wel.pos--;
	while(wel.pos>0&&!check(wel)) wel.pos--;
	if(wel.pos<0)
	{
	    wel.pos=prev;
	    return false;
	}
	return true;
    }

    @Override public boolean to(ElementList wel,int pos)
    {
	if(wel.getPos()==pos)
	    return true; else 
	    if(wel.getPos()<pos)
	    { // need next
		while(next(wel)) 
		    if(pos==wel.getPos()) 
			return true;
		return false;
	    } else
	    { // need prev
		while(prev(wel)) 
		    if(pos==wel.getPos()) 
			return true;
		return false;
	    }
    }
}
