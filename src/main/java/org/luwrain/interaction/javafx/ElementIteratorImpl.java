
package org.luwrain.interaction.javafx;

import java.awt.Rectangle;
import java.util.*;
import java.util.concurrent.*;

import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.html.*;
import org.w3c.dom.*;
import javafx.application.Platform;

import org.luwrain.browser.*;
import org.luwrain.core.*;

class ElementIteratorImpl implements ElementIterator
{
    // javascript window's property names for using in executeScrypt
        static final String GET_NODE_TEXT="get_node_text";

    final BrowserImpl browser;
    int pos=0;

    ElementIteratorImpl(BrowserImpl browser)
    {
	NullCheck.notNull(browser, "browser");
	this.browser = browser;
    }

    @Override public ElementIterator clone()
    {
    	ElementIteratorImpl result=new ElementIteratorImpl(browser);
    	result.pos=pos;
    	return result;
    }

    @Override public boolean isVisible()
    {
	return current().isVisible();
    }

    @Override public boolean forTEXT()
	{
	    return current().getForText();
	}

    @Override public int getPos()
    {
	return pos;
    }
	@Override public void setPos(int val)
	{
	pos=val;
	}


    @Override public String getType()
    {
    	if(current().getNode() instanceof org.w3c.dom.Text)
	    return "text"; else 
	    if(current().getNode() instanceof HTMLInputElement)
	    {
		try {
			String type=current().getNode().getAttributes().getNamedItem("type").getNodeValue();
			if(type.equals("button")) return "button";
		    return "input "+type;
		}
		catch(Exception e) 
		{
		    return "input";
		}
	    } else
		if(current().getNode() instanceof HTMLButtonElement)
		{
		    return "button";
		} else
		    if(current().getNode() instanceof HTMLAnchorElement)
	    {
		return "link";
	    } else
			if(current().getNode() instanceof HTMLImageElement)
		{
		    return "image";
		} else
			if(current().getNode() instanceof HTMLSelectElement)
		{
			return "select";
		} else
		    if(current().getNode() instanceof HTMLTableElement)
		{
		    return "table";
		} else
		    if(current().getNode() instanceof HTMLUListElement
		     ||current().getNode() instanceof HTMLOListElement)
		{
	    	return "list";
		} else
		{
		    return current().getNode().getNodeName().toLowerCase();
		}
    }

    @Override public String getText()
    {
    	String text="";
    	if(current().getNode() instanceof Text)
    	{
	    text = current().getNode().getNodeValue().trim();
    	} else 
	    if(current().getNode() instanceof HTMLInputElement)
	    { // input element
		final HTMLInputElement input=((HTMLInputElement)current().getNode());
			if(input.getType().equals("checkbox")
			 ||input.getType().equals("radio"))
			{
		    	text=input.getChecked()?"on":"off";
			} else
			{
		    	text=input.getValue();
			}
	    } else
		if(current().getNode() instanceof HTMLSelectElement)
	    {
	    	HTMLSelectElement select=(HTMLSelectElement)current().getNode();
	    	int idx=select.getSelectedIndex();
	    	text=select.getOptions().item(idx).getTextContent();
	    	// TODO: make multiselect support
	    } else
	    { // any other element
	    	text=getComputedText();
	    }
    	if(text==null) text="";
    	if(text.isEmpty())
    	{ // if text empty, try to add info from attributes
    		if(current().getNode() instanceof HTMLAnchorElement
    		 ||current().getNode() instanceof HTMLImageElement
    		 ||current().getNode() instanceof HTMLInputElement)
    		{ // title
    			if(current().getNode().hasAttributes())
    			{
    				Node title=current().getNode().getAttributes().getNamedItem("title");
    				if(title!=null)
    					text="title:"+title.getNodeValue();
    				Node alt=current().getNode().getAttributes().getNamedItem("alt");
    				if(alt!=null)
    					text=(!text.isEmpty()?" ":"")+"alt:"+alt.getNodeValue();
    			}
    		}
    	}
    	//
    	return text;
    }

    @Override public String[] getMultipleText()
    {
    	if(current().getNode() instanceof HTMLSelectElement)
    	{
	    HTMLSelectElement select=(HTMLSelectElement)current().getNode();
    		Vector<String> res=new Vector<String>();
    		for(int i=select.getLength();i>=0;i--)
 			{
 				Node option=select.getOptions().item(i);
 				if(option==null) continue; // so strange but happends
 				res.add(option.getTextContent());
 			}
    		return res.toArray(new String[res.size()]);
    	} else
    	{
    		return new String[]{getText()};
    	}
    }

    @Override public Rectangle getRect()
    {
    	if(!browser.getDOMmap().containsKey(current().getNode())) 
	    return null;
    	final int pos = browser.getDOMmap().get(current().getNode());
    	if(browser.getDOMList().size()<=pos) return null;
    	return browser.getDOMList().get(pos).getRect();
    }

    @Override public boolean isEditable()
    {
	if(current().getNode() instanceof HTMLInputElement)
	{
	    final String inputType = ((HTMLInputElement)current().getNode()).getType();
	    if(inputType.equals("button")
	     ||inputType.equals("inage")
	     ||inputType.equals("button")
	     ||inputType.equals("submit")) 
	   		return false;
	    // all other input types are editable
	    return true;
	} else
	    if(current().getNode() instanceof HTMLSelectElement)
	{
		return true;
	} else 
		if(current().getNode() instanceof HTMLTextAreaElement)
		return true; 
	return false;
    }

    @Override public void setText(String text)
    {
    	Log.debug("javafx","setText: "+current().getNode().getClass().getSimpleName()+", rect:" + browser.getDOMList().get(browser.getDOMmap().get(current().getNode())).getRect());
	if(current().getNode() instanceof HTMLInputElement)
		{
		    final HTMLInputElement input=((HTMLInputElement)current().getNode());
			if(input.getType().equals("checkbox")
			 ||input.getType().equals("radio"))
			{
				input.setChecked(text.isEmpty()||text.equals("0")||text.equals("off")?false:true);
			} else
			{
				input.setValue(text);
			}
		} else
	    if(current().getNode() instanceof HTMLSelectElement)
		{
		    HTMLSelectElement select=(HTMLSelectElement)current().getNode();
			for(int i=select.getLength();i>=0;i--)
			{
				Node option=select.getOptions().item(i);
				if(option==null) continue; // so strange but happends
				//Log.debug("web",option.getNodeValue()+"["+i+"]="+option.getTextContent());
				// FIXME: make method to work with select option by index not by text value (not unique)
				if(option.getTextContent().equals(text))
				{
					select.setSelectedIndex(i);
					return;
				}
			}
		}
	if(current().getNode() instanceof HTMLTextAreaElement)
	    {
	    	((HTMLTextAreaElement)current().getNode()).setTextContent(text);
	    }
    }

    @Override public String getLink()
    {
	if(current().getNode() instanceof HTMLAnchorElement)
	    return getAttributeProperty("href"); else
	    if(current().getNode() instanceof HTMLImageElement)
		return getAttributeProperty("src");
	return "";
    }

    @Override public String getAttributeProperty(String name)
    {
	if(!current().getNode().hasAttributes()) 
	    return null;
	final Node attr=current().getNode().getAttributes().getNamedItem(name);
	if(attr==null) return null;
	return attr.getNodeValue();
    }
    private String getComputedAttributeAll()
    {
    	if(!current().getNode().hasAttributes()) 
    	    return "";
    	String res="";
    	for(int i=current().getNode().getAttributes().getLength()-1;i>=0;i--)
    	{
	    Node node = current().getNode().getAttributes().item(i);
    		res+=node.getNodeName()+"="+node.getNodeValue()+";";
    	}
    	return res;
    }

    @Override public String getComputedText()
    {
	Callable<String> task=new Callable<String>(){
	    @Override public String call()
	    {
		browser.htmlWnd.setMember(GET_NODE_TEXT, current().getNode());
		if(!browser.getDOMmap().containsKey(current().getNode())) 
			return "";
		try{
		    return browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";return x.innerText===undefined?x.nodeValue:x.innerText})()").toString();
		}
		catch(Exception e)
		{
		    //e.printStackTrace();
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
	    	Node n=current().getNode();
	    	if(n instanceof com.sun.webkit.dom.HTMLDocumentImpl)
	    		return "";
	    	if(n instanceof Text)
	    	{ // we can't get style for simple text node, we need get it from parent tag
	    		n=n.getParentNode();
	    	}
			CSSStyleDeclaration style = browser.htmlWnd.getComputedStyle((HTMLElement)n,"");
			String css=style.getPropertyValue(name);
			return css;
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
    	try
    	{
    		Callable<String> task=new Callable<String>(){ @Override public String call()
		    {
		    	Node n=current().getNode();
		    	if(n instanceof com.sun.webkit.dom.HTMLDocumentImpl)
		    		return "";
		    	if(n instanceof Text)
		    	{ // we can't get style for simple text node, we need get it from parent tag
		    		n=n.getParentNode();
		    	}
				CSSStyleDeclaration style = browser.htmlWnd.getComputedStyle((HTMLElement)n, "");
				String css=style.getCssText();
				return css;
		    }};
		    if(Platform.isFxApplicationThread())
				 return task.call();
			FutureTask<String> query=new FutureTask<String>(task);
			Platform.runLater(query);
			return query.get();
    	} // FIXME: make better error handling
    	catch(Exception e)
    	{
    		//e.printStackTrace();
    		return "";
    	}
    }

    @Override public void clickEmulate()
    {
	// click can be done only for non text nodes
	// FIXME: emulate click for text nodex via parent node
	if(current().getNode() instanceof HTMLInputElement)
	{
	    if(((HTMLInputElement)current().getNode()).getType().equals("submit"))
	    { // submit button
		Platform.runLater(new Runnable() {
			@Override public void run()
			{
			    browser.htmlWnd.setMember(GET_NODE_TEXT, current().getNode());
			    try{
				browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.form.submit();})()");
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
			Node node=current().getNode();
			if(node.getNodeType()==Node.TEXT_NODE)
			{ // text node click sometimes does not work, move to parent
				node=node.getParentNode();
			}
		    browser.htmlWnd.setMember(GET_NODE_TEXT, node);
		    try{
			browser.executeScript("(function(){var x=window."+GET_NODE_TEXT+";x.click();})()");
		    }
		    catch(Exception e)
		    {
		    } // can't click - no reaction
		}
	    });
    }

    private String getHtml()
    {
    	if(current().getNode() instanceof Text)
    		return current().getNode().getNodeValue();

    	String xml="";
    	/*
    	try
    	{
    		Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(current().getNode());
			transformer.transform(source, result);
			xml=result.getWriter().toString();
    	} catch(TransformerException ex)
    	{
    		// FIXME:
    		ex.printStackTrace();
    	}
    	*/    	
    	xml=getText()+getComputedStyleAll()+getComputedAttributeAll();
    	//Log.debug("web","Node xml:"+xml);
    	return xml;
    }
    
    public boolean isChanged()
	{
	    final NodeInfo info = browser.getDOMList().get(pos);
		if(info.getHashTime()==0)
		{ // need to get hash
			info.calcHash(getHtml());
			// first time changes are undefined
			return false;
		}
		// last changes already scanned
		int oldHash=info.getHash();
		String html=getHtml();
		info.calcHash(html);
		//System.out.println("["+pos+":"+current().getNode().getClass().getSimpleName()+":"+info.hash+"]");
		//if(oldHash!=info.hash) System.out.println("changes detected");
		return oldHash!=info.getHash();
	}
/*
	@Override public boolean isChangedAround(Selector selector,int pos,int count)
	{
		final ElementIteratorImpl that=this;
		Callable<Boolean> task=new Callable<Boolean>(){
		    @Override public Boolean call()
		    {
		    	int cnt;
		    	selector.moveToPos(that, pos);
		    	cnt=count;
		    	while(selector.movePrev(that) && cnt-- > 0)
			    	if(that.isChanged())
			    		return true;
		    	selector.moveToPos(that, pos);
		    	cnt=count;
		    	while(selector.moveNext(that) && cnt-- > 0)
			    	if(that.isChanged())
			    		return true;
				return false;
		    }};
		if(Platform.isFxApplicationThread()) 
		    try
			{
		    	return task.call();
		    }
		    catch(Exception e)
		    {
		    	return false;
		    }
		FutureTask<Boolean> query=new FutureTask<Boolean>(task);
		Platform.runLater(query);
		try
		{
			boolean res=query.get();
		    return res;
	    }
		catch(Exception e)
		{
		    return false;
		}
		// FIXME: make better error handling
	}
*/
    NodeInfo current()
    {
	return browser.getDOMList().get(pos);
    }
	@Override public ElementIterator getParent()
	{
	    if(browser.getDOMList().get(pos).getParent()==null)
			return null;
		ElementIteratorImpl parent=new ElementIteratorImpl(browser);
		parent.pos = browser.getDOMList().get(pos).getParent();
		return parent;
	}
	/*
	@Override public SelectorChildren getChildren(boolean visible)
	{
		FutureTask<SelectorChildren> query=new FutureTask<SelectorChildren>(new Callable<SelectorChildren>()
		{
			@Override public SelectorChildren call() throws Exception
			{
				//System.out.println("CHILDS");
				Vector<Integer> childs=new Vector<Integer>();
				NodeInfo node=current();
				for(NodeInfo info: browser.getDOMList())
				{
					//System.out.println("CHILDS: test parents "+info.node+" for "+(info.parent==null?"null":page.dom.get(info.parent).node)+"=="+node.node+(info.parent!=null&&page.dom.get(info.parent).equals(node)));
				    if(info.parent!=null && browser.getDOMList().get(info.parent).equals(node))
					childs.add(browser.getDOMmap().get(info.node)); // it is ugly way to get index, but for loop have inaccessible index
				}
				return new SelectorChildrenImpl(visible,childs.toArray(new Integer[childs.size()]));
			}
		});
		Platform.runLater(query);
		try 
		{
			return query.get();
		}
		catch(InterruptedException|ExecutionException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	*/

	@Override public String getHtmlTagName()
	{
		return current().getNode().getNodeName();
	}

	@Override public Browser getBrowser()
	{
		return browser;
	}
}
