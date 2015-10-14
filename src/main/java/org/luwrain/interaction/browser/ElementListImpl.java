package org.luwrain.interaction.browser;

import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;

import org.w3c.dom.Node;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.html.*;

import com.sun.webkit.dom.*;

import org.luwrain.browser.ElementList;
import org.luwrain.browser.ElementList.SelectorALL;
import org.luwrain.core.Log;

class ElementListImpl implements ElementList
{
    // javascript window's property names for using in executeScrypt
    static final String GET_NODE_TEXT="get_node_text";

    // link to page for listed elements
    WebPage page;
    // current index in dom structure
    // FIXME: move to similar element when RescanDOM called
    int pos=0;
    // current element WebPage.NodeInfo
WebPage.NodeInfo current;

    /*
	// list of string arrays, each - splitetd lines of elements, it is a cache of element text
	private SplittedLine[][] splittedLines=new SplittedLine[0][];
    */

    ElementListImpl(WebPage page)
	{
		this.page=page;
	}

@Override public int getPos()
{
return pos;
}
    
    @Override public String getType()
    {
	if(current.node.getClass()==com.sun.webkit.dom.TextImpl.class)
	{
	    return "text";
	} else 
	    if(current.node instanceof HTMLInputElement)
	    {
		try {
return "input "+current.node.getAttributes().getNamedItem("type").getNodeValue();
		}
		catch(Exception e) 
{
return "input";
}
	    } else
		if(current.node instanceof HTMLButtonElement)
		{
			return "button";
		} else
		    if(current.node instanceof HTMLAnchorElement)
		{
			return "link";
		} else
		if(current.node instanceof HTMLImageElement)
		{
			return "image";
		} else
		{
			return "other text";
		}
    }

    // return text for current element if not null
    @Override public String getText()
    {
	if(current.node.getClass()==com.sun.webkit.dom.TextImpl.class)
	{
	    return current.node.getNodeValue().trim();
	} else 
if(current.node.getClass()==com.sun.webkit.dom.HTMLInputElementImpl.class)
{
    //try {return current.node.getAttributes().getNamedItem("value").getNodeValue();} catch(Exception e) {return "";}
    return ((HTMLInputElementImpl)current.node).getValue();
} else
{
    return getComputedText();
}
    }

	@Override public boolean isEditable()
	{
		if(current.node.getClass()==com.sun.webkit.dom.HTMLInputElementImpl.class)
		{
		    if(((HTMLInputElementImpl)current.node).getType().equals("text")) return true;
		} else 
if(current.node.getClass()==com.sun.webkit.dom.HTMLTextAreaElementImpl.class)
		{
			return true;
		}
		return false;
	}

	@Override public void setText(String text)
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
		} else 
if(current.node.getClass()==com.sun.webkit.dom.HTMLTextAreaElementImpl.class)
		{
			// fixme:!!!
		}
	}

	@Override public String getLink()
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

	@Override public String getAttributeProperty(String name)
	{
		if(!current.node.hasAttributes()) 
return null;
		final Node attr=current.node.getAttributes().getNamedItem(name);
		return attr.getNodeValue();
	}
	
	@Override public String getComputedText()
	{
		Callable<String> task=new Callable<String>(){
		@Override public String call()
		{
			page.htmlWnd.setMember(GET_NODE_TEXT, current.node);
			try{
return page.webEngine.executeScript("(function(){var x=window."+GET_NODE_TEXT+";return x.innerText===undefined?x.nodeValue:x.innerText})()").toString();
}
			catch(Exception e)
{
e.printStackTrace();
return "";
}
		}};
		if(Platform.isFxApplicationThread()) 
try{
return task.call();}
catch
(Exception e)
{
return null;
}
		FutureTask<String> query=new FutureTask<String>(task);
		Platform.runLater(query);
		try{
return query.get();
}
catch(Exception e)
{
return null;
}
		// FIXME: make better error handling
	}
	
    @Override public String getComputedStyleProperty(final String name)
    {
	Callable<String> task=new Callable<String>(){
		@Override public String call()
		{
			CSSStyleDeclaration style = page.htmlWnd.getComputedStyle((HTMLElement)current.node, "");
			return style.getPropertyValue(name);
		}};
		if(Platform.isFxApplicationThread()) 
try{
return task.call();}
catch(Exception e)
{
return null;
}
		FutureTask<String> query=new FutureTask<String>(task);
		Platform.runLater(query);
		try{
return query.get();}
catch(Exception e)
{
return null;
}
		// FIXME: make better error handling
	}

	@Override public String getComputedStyleAll()
	{
		Callable<String> task=new Callable<String>(){
		@Override public String call()
		{
			CSSStyleDeclaration style = page.htmlWnd.getComputedStyle((HTMLElement)current.node, "");
			return style.getCssText();
		}};
		if(Platform.isFxApplicationThread()) try
{
return task.call();
}
catch(Exception e)
{
return null;
}
		FutureTask<String> query=new FutureTask<String>(task);
		Platform.runLater(query);
		try{
return query.get();
}
catch(Exception e)
{
return null;
}
		// FIXME: make better error handling
	}

	@Override public void clickEmulate()
	{
		// click can be done only for non text nodes
		// FIXME: emulate click for text nodex via parent node
		if(current.node instanceof HTMLInputElement)
		{
			if(((HTMLInputElement)current.node).getType().equals("submit"))
			{ // submit button
				Platform.runLater(new Runnable() {
					@Override public void run()
					{
						page.htmlWnd.setMember(GET_NODE_TEXT, current.node);
						try{
page.webEngine.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.form.submit();})()");
}
						catch(Exception e)
{
} // can't click - no reaction
					}
				});
				return;
			};
		}
		Platform.runLater(new Runnable() {
			@Override public void run()
			{
				page.htmlWnd.setMember(GET_NODE_TEXT, current.node);
				try{
page.webEngine.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.click();})()");
}
				catch(Exception e)
{
} // can't click - no reaction
			}
		});
	}
    /*
	@Override public SplittedLine[][] getSplittedLines()
{
return splittedLines;
}

	// count of all splited lines in list
	private int splittedCount=0;

	@Override public int getSplittedCount()
{
return splittedCount;
}

	@Override public SplittedLine getSplittedLineByIndex(int index)
	{
		int i=0;
		for(SplittedLine[] split:splittedLines)
		{
			if(i+split.length>index)
				return split[index-i]; 
			i+=split.length;
		}
		return null;
	}

	// return splited lines for element pos
	@Override public SplittedLine[] getSplittedLineByPos(int pos)
	{
		for(SplittedLine[] split: splittedLines)
		{
			if(split[0].pos==pos) return split; 
		}
		return null;
	}
    */
    /* scan all elements via selector and call getText for each of them and split into lines, store in cache, accessed via getCachedText
     * it change current position to end
     */
	/*
    @Override public void splitAllElementsTextToLines(int width,ElementList.Selector selector)
    {
	final Vector<SplittedLine[]> result=new Vector<SplittedLine[]>();
	splittedCount=0;
	int index=0;
	if(selector.first(this))
	{
	    do {
		String type=this.getType();
		String text=this.getText();
		String[] lines = SplittedLineProc.splitTextForScreen(width,text);
		final Vector<SplittedLine> splitted=new Vector<SplittedLine>();
				for(String line:lines) 
				    splitted.add(new SplittedLine(type,line,pos,index++));
				result.add(splitted.toArray(new SplittedLine[splitted.size()]));
				splittedCount+=splitted.size();
	    } while(selector.next(this));
	}
	splittedLines=result.toArray(new SplittedLine[result.size()][]);
    }
    
    // update split for current element text, used to update info in split text cache 
    void updateSplitForElementText(int width)
    {
	String type=this.getType();
		String text=this.getText();
		String[] lines = SplittedLineProc.splitTextForScreen(width,text);
		if(splittedLines.length<pos) return; // FIXME: make better error handling, out of bound, cache size invalid
		Vector<SplittedLine> splited=new Vector<SplittedLine>();
		int index=0;
		for(String line:lines)
		{
			splited.add(new SplittedLine(type,line,pos,index));
			index++;
		}
		splittedCount-=splittedLines[pos].length;
		splittedLines[pos]=splited.toArray(new SplittedLine[splited.size()]);
		splittedCount+=splited.size();
	}
*/
    }
