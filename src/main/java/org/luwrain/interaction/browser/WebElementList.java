package org.luwrain.interaction.browser;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;

import org.w3c.dom.Node;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.html.*;
import com.sun.webkit.dom.*;

import org.luwrain.browser.ElementList;
import org.luwrain.browser.ElementList.SelectorALL;

public class WebElementList implements ElementList
{
	// javascript window's property names for using in executeScrypt
	static final String GET_NODE_TEXT="get_node_text";
	
	// select filter all nodes on page
	public static abstract class Selector implements ElementList.Selector
	{
		@Override public abstract boolean check(ElementList wel);
		@Override public boolean first(ElementList wel_)
		{
			WebElementList wel=(WebElementList)wel_;
			wel.pos=0;
			while(wel.pos<wel.page.domIdx.size()&&!check(wel)) wel.pos++;
			if(wel.pos>=wel.page.domIdx.size())
			{
				wel.pos=0;
				return false;
			}
			return true;
		}
		@Override public boolean next(ElementList wel_)
		{
			WebElementList wel=(WebElementList)wel_;
			int prev=wel.pos;
			wel.pos++;
			while(wel.pos<wel.page.domIdx.size()&&!check(wel)) wel.pos++;
			if(wel.pos>=wel.page.domIdx.size())
			{
				wel.pos=prev;
				return false;
			}
			return true;
		}
		@Override public boolean prev(ElementList wel_)
		{
			WebElementList wel=(WebElementList)wel_;
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
	}
	public static class SelectorALL extends Selector implements ElementList.SelectorALL
	{
		boolean visible;
		@Override public boolean isVisible(){	return visible;	}
		@Override public void setVisible(boolean visible)	{this.visible=visible;}
		
		public SelectorALL(boolean visible)
		{
			this.visible=visible;
		}
		// return true if current element is visible
		@Override public boolean checkVisible(ElementList wel_)
		{
			WebElementList wel=(WebElementList)wel_;
			return wel.current.isVisible();
		}
		// return true if current element corresponds this selector
		@Override public boolean check(ElementList wel_)
		{
			WebElementList wel=(WebElementList)wel_;
			wel.current=wel.page.dom.get(wel.pos);
			if(visible&&!checkVisible(wel)) return false;
			return true;
		}
	};
	// select filter for select element via tag and its attribute
	// empty or null strings threat as any values
	public static class SelectorTAG extends SelectorALL implements ElementList.SelectorTAG
	{
		public String tagName,attrName,attrValue;
		@Override public String getTagName(){return tagName;}
		@Override public void setTagName(String tagName){this.tagName=tagName;}
		@Override public String getAttrName(){return attrName;}
		@Override public void setAttrName(String attrName){this.attrName=attrName;}
		@Override public String getAttrValue(){return attrValue;}
		@Override public void setAttrValue(String attrValue){this.attrValue=attrValue;}
		
		public SelectorTAG(boolean visible,String tagName,String attrName,String attrValue)
		{ // FIXME: change strings to lower case
			super(visible);
			this.tagName=tagName;
			this.attrName=attrName;
			this.attrValue=attrValue;
		}
		// return true if current element corresponds this selector
		@Override public boolean check(ElementList wel_)
		{
			WebElementList wel=(WebElementList)wel_;
			wel.current=wel.page.dom.get(wel.pos);
			if(visible&&!checkVisible(wel)) return false;
			// current selector's checks
			if(this.tagName!=null&&!wel.current.node.getNodeName().toLowerCase().equals(this.tagName)) return false;
			if(this.attrName!=null&&wel.current.node.hasAttributes())
			{ // attrValue can be null with attrName
				if(this.attrValue!=null&&wel.current.node.getAttributes().getNamedItem(this.attrName).getNodeValue().toLowerCase().indexOf(this.attrValue)==-1) return false;
			}
			return true;
		}
	}
	// select filter for select element via tag and its computed style attribute
	// empty or null strings threat as any values
	public static class SelectorCSS extends SelectorALL implements ElementList.SelectorCSS
	{
		String tagName,styleName,styleValue;
		@Override public String getTagName(){	return tagName;}
		@Override public void setTagName(String tagName){	this.tagName=tagName;}
		@Override public String getStyleName(){return styleName;}
		@Override public void setStyleName(String styleName){this.styleName=styleName;}
		@Override public String getStyleValue(){return styleValue;}
		@Override public void setStyleValue(String styleValue){this.styleValue=styleValue;}
		
		public SelectorCSS(boolean visible,String tagName,String styleName,String styleValue)
		{ // FIXME: change strings to lower case
			super(visible);
			this.tagName=tagName;
			this.styleName=styleName;
			this.styleValue=styleValue;
		}
		// return true if current element corresponds this selector
		@Override public boolean check(ElementList wel_)
		{
			WebElementList wel=(WebElementList)wel_;
			wel.current=wel.page.dom.get(wel.pos);
			if(visible&&!checkVisible(wel)) return false;
			// current selector's checks
			if(this.tagName!=null&&!wel.current.node.getNodeName().toLowerCase().equals(this.tagName)) return false;
			// make access to computed style
			String value=wel.getComputedStyleProperty(this.styleName);
			if(this.styleValue!=null&&value!=null)
			{ // attrValue can be null with attrName
				if(this.styleValue!=null&&value.toLowerCase().indexOf(this.styleValue)==-1) return false;
			}
			return true;
		}
	}
	// select filter for any text container element's, text filtered by text filter as substring
	// null string threat as any values
	// TODO: make RegEx support in filter
	public static class SelectorTEXT extends SelectorALL implements ElementList.SelectorTEXT
	{
		String filter;
		@Override public String getFilter(){return filter;}
		@Override public void setFilter(String filter){this.filter=filter;}
		
		public SelectorTEXT(boolean visible,String filter)
		{
			super(visible);
			this.filter=filter;
		}
		@Override public boolean check(ElementList wel_)
		{
			WebElementList wel=(WebElementList)wel_;
			wel.current=wel.page.dom.get(wel.pos);
			if(visible&&!checkVisible(wel)) return false;
			// current selector's checks
			if(!wel.current.forTEXT) return false;
			String text=wel.getText(); // TODO: if filter is null, we can skip getText for each node in list to speed up walking but consume empty text nodes
			if(text==null) text="";else text=text.trim();
			if(!wel.current.node.getClass().equals(HTMLAnchorElementImpl.class)
			 &&!wel.current.node.getClass().equals(HTMLInputElementImpl.class)
			 &&text.isEmpty()) return false;
			if(filter!=null&&text.toLowerCase().indexOf(filter)==-1) return false;
			return true;
		}
	}
	
	// select filter for any visible elements on page and navigate them relative (i.e. left,right,above,below,inside,outside)
	// next and prev methods mapped to right and left
	public static class SelectorRELATIVE extends SelectorALL
	{
		public SelectorRELATIVE()
		{
			super(true);
		}
		// FIXME: 
	}
	
	// link to page for listed elements
	public WebPage page;
	// current index in dom structure
	// FIXME: move to similar element when RescanDOM called
	public int pos=0;
	// current element WebPage.NodeInfo
	public WebPage.NodeInfo current;
	
	public String getType()
	{
		if(current.node.getClass()==com.sun.webkit.dom.TextImpl.class)
		{
			return "text";
		} else if(current.node.getClass()==com.sun.webkit.dom.HTMLInputElementImpl.class)
		{
			try {return "input "+current.node.getAttributes().getNamedItem("type").getNodeValue();}
			catch(Exception e) {return "input";}
		} else
		if(current.node.getClass()==com.sun.webkit.dom.HTMLButtonElementImpl.class)
		{
			return "button";
		} else
		if(current.node.getClass()==com.sun.webkit.dom.HTMLAnchorElementImpl.class)
		{
			return "link";
		} else
		if(current.node.getClass()==com.sun.webkit.dom.HTMLImageElementImpl.class)
		{
			return "image";
		} else
		{
			return "other text";
		}
	}
	
	// return text for current element if not null
	public String getText()
	{
		if(current.node.getClass()==com.sun.webkit.dom.TextImpl.class)
		{
			return current.node.getNodeValue().trim();
		} else if(current.node.getClass()==com.sun.webkit.dom.HTMLInputElementImpl.class)
		{
			//try {return current.node.getAttributes().getNamedItem("value").getNodeValue();} catch(Exception e) {return "";}
			return ((HTMLInputElementImpl)current.node).getValue();
		} else
		{
			return getComputedText();
		}
	}
	
	public boolean isEditable()
	{
		if(current.node.getClass()==com.sun.webkit.dom.HTMLInputElementImpl.class)
		{
			if(((HTMLInputElementImpl)current.node).getType().equals("text")) return true;
		} else if(current.node.getClass()==com.sun.webkit.dom.HTMLTextAreaElementImpl.class)
		{
			return true;
		}
		return false;
	}
	public void setText(String text)
	{
		if(current.node.getClass()==com.sun.webkit.dom.HTMLInputElementImpl.class)
		{
			if(((HTMLInputElementImpl)current.node).getType().equals("text"))
			{
				//Log.debug("edit","text:"+current.node.getClass().getSimpleName());
				//((HTMLElement)current.node).setTextContent(text);
				((HTMLInputElementImpl)current.node).setValue(text);
				//.setAttribute("value",text);
			}
		} else if(current.node.getClass()==com.sun.webkit.dom.HTMLTextAreaElementImpl.class)
		{
			// fixme:!!!
		}
	}
	
	public String getLink()
	{
		if(current.node.getClass()==com.sun.webkit.dom.HTMLAnchorElementImpl.class)
		{
			return getAttributeProperty("href");
		} else
		if(current.node.getClass()==com.sun.webkit.dom.HTMLImageElementImpl.class)
		{
			return getAttributeProperty("src");
		}
		return "";
	}
	
	public String getAttributeProperty(String name)
	{
		if(!current.node.hasAttributes()) return null;
		Node attr=current.node.getAttributes().getNamedItem(name);
		return attr.getNodeValue();
	}
	
	public String getComputedText()
	{
		Callable<String> task=new Callable<String>(){
		@Override public String call()
		{
			page.htmlWnd.setMember(GET_NODE_TEXT, current.node);
			try{return page.webEngine.executeScript("(function(){var x=window."+GET_NODE_TEXT+";return x.innerText===undefined?x.nodeValue:x.innerText})()").toString();}
			catch(Exception e){e.printStackTrace();return "";}
		}};
		if(Platform.isFxApplicationThread()) try{return task.call();}catch(Exception e){return null;}
		FutureTask<String> query=new FutureTask<String>(task);
		Platform.runLater(query);
		try{return query.get();}catch(Exception e){return null;}
		// FIXME: make better error handling
	}
	
	public String getComputedStyleProperty(final String name)
	{
		Callable<String> task=new Callable<String>(){
		@Override public String call()
		{
			CSSStyleDeclaration style = page.htmlWnd.getComputedStyle((HTMLElement)current.node, "");
			return style.getPropertyValue(name);
		}};
		if(Platform.isFxApplicationThread()) try{return task.call();}catch(Exception e){return null;}
		FutureTask<String> query=new FutureTask<String>(task);
		Platform.runLater(query);
		try{return query.get();}catch(Exception e){return null;}
		// FIXME: make better error handling
	}
	
	public String getComputedStyleAll()
	{
		Callable<String> task=new Callable<String>(){
		@Override public String call()
		{
			CSSStyleDeclaration style = page.htmlWnd.getComputedStyle((HTMLElement)current.node, "");
			return style.getCssText();
		}};
		if(Platform.isFxApplicationThread()) try{return task.call();}catch(Exception e){return null;}
		FutureTask<String> query=new FutureTask<String>(task);
		Platform.runLater(query);
		try{return query.get();}catch(Exception e){return null;}
		// FIXME: make better error handling
	}
	
	public void clickEmulate()
	{
		Platform.runLater(new Runnable()
		{
			@Override public void run()
			{
				page.htmlWnd.setMember(GET_NODE_TEXT, current.node);
				try{page.webEngine.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.click();})()");}
				catch(Exception e){} // can't click - no reaction
			}
		});
		
	}
	
	public WebElementList(WebPage page)
	{
		this.page=page;
	}
}
