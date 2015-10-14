
package org.luwrain.interaction.browser;

import org.luwrain.browser.*;

// select filter all nodes on page

class SelectorAllImpl extends SelectorImpl implements ElementList.SelectorALL
	{
		boolean visible;
		@Override public boolean isVisible(){	return visible;	}
		@Override public void setVisible(boolean visible)	{this.visible=visible;}
		
SelectorAllImpl(boolean visible)
		{
			this.visible=visible;
		}
		// return true if current element is visible
		@Override public boolean checkVisible(ElementList wel_)
		{
			ElementListImpl wel=(ElementListImpl)wel_;
			return wel.current.isVisible();
		}
		// return true if current element corresponds this selector
		@Override public boolean check(ElementList wel_)
		{
			ElementListImpl wel=(ElementListImpl)wel_;
			wel.current=wel.page.dom.get(wel.pos);
			if(visible&&!checkVisible(wel)) return false;
			return true;
		}
	};
