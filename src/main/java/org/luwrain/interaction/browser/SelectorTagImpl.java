
package org.luwrain.interaction.browser;

import org.luwrain.browser.*;

// select filter for select element via tag and its attribute
	// empty or null strings threat as any values
class SelectorTagImpl extends SelectorAllImpl implements SelectorTag
	{
		public String tagName,attrName,attrValue;
		@Override public String getTagName(){return tagName;}
		@Override public void setTagName(String tagName){this.tagName=tagName;}
		@Override public String getAttrName(){return attrName;}
		@Override public void setAttrName(String attrName){this.attrName=attrName;}
		@Override public String getAttrValue(){return attrValue;}
		@Override public void setAttrValue(String attrValue){this.attrValue=attrValue;}
		
SelectorTagImpl(boolean visible,String tagName,String attrName,String attrValue)
		{ // FIXME: change strings to lower case
			super(visible);
			this.tagName=tagName;
			this.attrName=attrName;
			this.attrValue=attrValue;
		}
		// return true if current element corresponds this selector
		@Override public boolean suits(ElementList wel_)
		{
			ElementIteratorImpl wel=(ElementIteratorImpl)wel_;
			//			wel.current=wel.page.dom.get(wel.pos);
			if(visible&&!checkVisible(wel)) return false;
			// current selector's checks
			if(this.tagName!=null&&!wel.current().node.getNodeName().toLowerCase().equals(this.tagName)) 
return false;
			if(this.attrName!=null&&wel.current().node.hasAttributes())
			{ // attrValue can be null with attrName
			    if(this.attrValue!=null&&wel.current().node.getAttributes().getNamedItem(this.attrName).getNodeValue().toLowerCase().indexOf(this.attrValue)==-1) 
return false;
			}
			return true;
		}
	}
