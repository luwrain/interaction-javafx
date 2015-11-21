
package org.luwrain.interaction.browser;

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
    @Override public boolean checkVisible(ElementList it)
    {
	final ElementIteratorImpl itImpl = (ElementIteratorImpl)it;
	return itImpl.current().isVisible();
    }

    /** return true if current element suits the condition of this selector.*/
    @Override public boolean suits(ElementList it)
    {
	final ElementIteratorImpl itImpl = (ElementIteratorImpl)it;
	if(visible&&!checkVisible(itImpl))
	    return false;
	return true;
    }
}
