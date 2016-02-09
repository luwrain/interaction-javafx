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
