
package org.luwrain.interaction.javafx;

import org.luwrain.browser.*;

// select filter for select element via tag and its computed style attribute
// empty or null strings threat as any values
class SelectorCssImpl extends SelectorAllImpl implements SelectorCss
	{
		String tagName,styleName,styleValue;
		@Override public String getTagName(){	return tagName;}
		@Override public void setTagName(String tagName){	this.tagName=tagName;}
		@Override public String getStyleName(){return styleName;}
		@Override public void setStyleName(String styleName){this.styleName=styleName;}
		@Override public String getStyleValue(){return styleValue;}
		@Override public void setStyleValue(String styleValue){this.styleValue=styleValue;}
		
SelectorCssImpl(boolean visible, String tagName,
		 String styleName, String styleValue)
		{ // FIXME: change strings to lower case
			super(visible);
			this.tagName=tagName;
			this.styleName=styleName;
			this.styleValue=styleValue;
		}
		// return true if current element corresponds this selector
		@Override public boolean suits(ElementIterator wel_)
		{
			ElementIteratorImpl wel=(ElementIteratorImpl)wel_;
			//			wel.current=wel.page.dom.get(wel.pos);
			if(visible&&!checkVisible(wel)) 
return false;
			// current selector's checks
			if(this.tagName!=null&&!wel.current().node.getNodeName().toLowerCase().equals(this.tagName)) return false;
			// make access to computed style
			String value=wel.getComputedStyleProperty(this.styleName);
			if(this.styleValue!=null&&value!=null)
			{ // attrValue can be null with attrName
				if(this.styleValue!=null&&value.toLowerCase().indexOf(this.styleValue)==-1) return false;
			}
			return true;
		}
	}
