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

	// list of string arrays, each - splitetd lines of elements, it is a cache of element text
	private SplitedLine[][] splitedLines=new SplitedLine[0][];

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

	@Override public SplitedLine[][] getSplitedLines()
{
return splitedLines;
}

	// count of all splited lines in list
	private int splitedCount=0;

	@Override public int getSplitedCount()
{
return splitedCount;
}

	@Override public SplitedLine getSplitedLineByIndex(int index)
	{
		int i=0;
		for(SplitedLine[] split:splitedLines)
		{
			if(i+split.length>index)
				return split[index-i]; 
			i+=split.length;
		}
		return null;
	}

	// return splited lines for element pos
	@Override public SplitedLine[] getSplitedLineByPos(int pos)
	{
		for(SplitedLine[] split:splitedLines)
		{
			if(split[0].pos==pos) return split; 
		}
		return null;
	}
	
	/* scan all elements via selector and call getText for each of them and split into lines, store in cache, accessed via getCachedText
	 * it change current position to end
	 */
	@Override public void splitAllElementsTextToLines(int width,ElementList.Selector selector)
	{
		Vector<SplitedLine[]> result=new Vector<SplitedLine[]>();
		splitedCount=0;
		int index=0;
		if(selector.first(this))
		{
			do {
				String type=this.getType();
				String text=this.getText();
				String[] lines=splitTextForScreen(width,text);
				Vector<SplitedLine> splited=new Vector<SplitedLine>();
				for(String line:lines) splited.add(new SplitedLine(type,line,pos,index++));
				result.add(splited.toArray(new SplitedLine[splited.size()]));
				splitedCount+=splited.size();
			} while(selector.next(this));
		}
		splitedLines=result.toArray(new SplitedLine[result.size()][]);
	}
	/* update split for current element text, used to update info in split text cache */
	void updateSplitForElementText(int width)
	{
		String type=this.getType();
		String text=this.getText();
		String[] lines=splitTextForScreen(width,text);
		if(splitedLines.length<pos) return; // FIXME: make better error handling, out of bound, cache size invalid
		Vector<SplitedLine> splited=new Vector<SplitedLine>();
		int index=0;
		for(String line:lines)
		{
			splited.add(new SplitedLine(type,line,pos,index));
			index++;
		}
		splitedCount-=splitedLines[pos].length;
		splitedLines[pos]=splited.toArray(new SplitedLine[splited.size()]);
		splitedCount+=splited.size();
	}

    public static String[] splitTextForScreen(int width,String string)
    {
    	Vector<String> text=new Vector<String>();
    	if(string==null||string.isEmpty())
    	{
    		text.add("");
    		return text.toArray(new String[(text.size())]);
    	}
    	int i=0;
    	while(i<string.length())
    	{
		    String line;
		    if(i+width>=string.length())
		    { // last part of string fit to the screen
		    	line=string.substring(i);
		    } else
		    { // too long part
				line=string.substring(i,i+width-1);
				// walk to first stopword char at end of line
			    int sw=line.lastIndexOf(' ');
			    if(sw!=-1)
			    { // have stop char, cut line to it (but include)
			    	line=line.substring(0,sw);
				}
		    }
			// check for new line char
			int nl=line.indexOf('\n');
			if(nl!=-1)
			{ // have new line char, cut line to it
			    line=line.substring(0,nl);
			    i++; // skip new line
			}
		    text.add(line);
		    i+=line.length();
    	}
    	return text.toArray(new String[(text.size())]);
    }
    
}
