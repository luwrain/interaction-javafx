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
		@Override public boolean suits(ElementIterator wel_)
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
