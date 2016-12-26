
package org.luwrain.interaction.javafx;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.input.KeyEvent;
import netscape.javascript.JSObject;

import org.w3c.dom.Node;
import org.w3c.dom.html.*;
import org.w3c.dom.views.DocumentView;
import com.sun.webkit.dom.DOMWindowImpl;

import org.luwrain.core.*;
import org.luwrain.browser.*;
import org.luwrain.browser.Events.WebState;

class BrowserImpl implements Browser
{
	/** lastModifiedTime rescan interval in milliseconds */
	static final int LAST_MODIFIED_SCAN_INTERVAL=100;
    // javascript window's property names for using in executeScrypt
	static final String LUWRAIN_NODE_TEXT="luwrain_node_text";
	
    private final JavaFxInteraction interaction;
    private WebView webView = null;
    private WebEngine webEngine = null;
    private boolean busy = false;
    private Vector<NodeInfo> dom=new Vector<NodeInfo>();
    private LinkedHashMap<Node,Integer> domMap = new LinkedHashMap<Node, Integer>();
    private HTMLDocument htmlDoc = null;
    DOMWindowImpl htmlWnd = null;//FIXME:
    private JSObject window = null;
    private boolean userStops = false;

    private JSObject luwrainJSobject=null;
	private long lastModifiedTime;

    /** return current browser's list of nodes, WARNING, use w3c node only in Browser's thread */
    @Override public Vector<NodeInfo> getDOMList()
    {
    	return dom;
    }
    /** return reverse index HashMap for accessing NodeInfo index in dom list by w3c Node */
    @Override public LinkedHashMap<Node,Integer> getDOMmap()
    {
    	return domMap;
    }
    
    public BrowserImpl(JavaFxInteraction interaction)
    {
	NullCheck.notNull(interaction, "interaction");
	this.interaction = interaction;
    }

    @Override public void init(org.luwrain.browser.Events events)
    {
	final BrowserImpl browser = this;
	final boolean emptyList = interaction.browsers.isEmpty();
	interaction.browsers.add(this);
	Platform.runLater(()->{
		webView = new WebView();
		webEngine = webView.getEngine();
		webView.setOnKeyReleased((event)->onKeyReleased(event));
		webEngine.getLoadWorker().stateProperty().addListener((ov,oldState,newState)->onStateChange(events, ov, oldState, newState));
		webEngine.getLoadWorker().progressProperty().addListener((ov,o,n)->events.onProgress(n));
		webEngine.setOnAlert((event)->events.onAlert(event.getData()));
		webEngine.setPromptHandler((event)->events.onPrompt(event.getMessage(),event.getDefaultValue()));
		webEngine.setConfirmHandler((param)->events.onConfirm(param));
		webEngine.setOnError((event)->events.onError(event.getMessage()));
		webView.setVisible(false);
		interaction.addWebViewControl(webView);
		if(emptyList) 
		    interaction.setCurrentBrowser(browser);
	    });
    // start changes detection
	Timer timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask()
    {
        @Override public void run()
        {
        	Platform.runLater(()->{
				{
					if(luwrainJSobject==null) return;
					long time=(long)(double)luwrainJSobject.getMember("domLastTime");
					if(time==lastModifiedTime) return;
					//System.out.println("modified");
					// does not call changed event first time page loaded
					if(lastModifiedTime!=0)
						events.onPageChanged();
					lastModifiedTime=time;
				}});
        }
    }, 0, LAST_MODIFIED_SCAN_INTERVAL);

    }

    @Override public void doFastUpdate()
    {
    	Platform.runLater(()->
    	{
        	// check if injected object success
        	if(luwrainJSobject==null||"_luwrain_".equals(luwrainJSobject.getMember("name")))
        		return;
    		window.setMember(LUWRAIN_NODE_TEXT,luwrainJSobject);
    		webEngine.executeScript(LUWRAIN_NODE_TEXT+".doUpdate();");
    	});
    }
    
    @Override public void RescanDOM()
    {
    	busy=true;
    	final Callable<Integer> task = ()->{
    	// check if injected object success
    	if(luwrainJSobject==null||"_luwrain_".equals(luwrainJSobject.getMember("name")))
    		return null;
	    // prepare some  objects document and window
    	htmlDoc = (HTMLDocument)webEngine.getDocument();
	    if(htmlDoc == null)
		return null;
	    htmlWnd = (DOMWindowImpl)((DocumentView)htmlDoc).getDefaultView();
	    dom = new Vector<NodeInfo>();
	    domMap = new LinkedHashMap<Node, Integer>();
	    //
    	lastModifiedTime=jsLong(luwrainJSobject.getMember("domLastTime"));
    	Log.debug("javafx-dom","modified at: "+(int)(lastModifiedTime/1000)+", scanned:"+luwrainJSobject.getMember("scanLT")+"ms, watch:"+((JSObject)luwrainJSobject.getMember("watch")).getMember("length")+" size");
    	final JSObject js = (JSObject)luwrainJSobject.getMember("dom");
    	//JSObject watchArray=(JSObject)webEngine.executeScript("[]");
    	//int j=0;

	    Object o;
	    for(int i=0;!(o=js.getSlot(i)).getClass().equals(String.class);i++)
	    {
		final JSObject rect=(JSObject)((JSObject)o).getMember("r");
		final Node n=(Node)((JSObject)o).getMember("n");
	    int x = 0;
	    int y = 0;
	    int width = 0;
	    int height = 0;
		if(rect != null)
		{
		    x = (int)jsLong(rect.getMember("left"));
		    y = (int)jsLong(rect.getMember("top"));
		    width=(int)jsLong(rect.getMember("width"));
		    height=(int)jsLong(rect.getMember("height"));
		}
		boolean forText = !n.hasChildNodes();
		// make decision about TEXT nodes by class
		if(n instanceof HTMLAnchorElement
		   ||n instanceof HTMLButtonElement
		   ||n instanceof HTMLInputElement
		   //||n.getClass() == com.sun.webkit.dom.HTMLPreElementImpl.class
		   ||n instanceof HTMLSelectElement
		   ||n instanceof HTMLTextAreaElement
		   ||n instanceof HTMLSelectElement)
		    forText = true;
		final boolean ignore = checkNodeForIgnoreChildren(n);
		if(ignore) 
		    forText = false;
		final NodeInfo info=new NodeInfo(n,x,y,width,height,forText);
		domMap.put(n, i);
		dom.add(info);
		//Log.debug("javafx-dom", i+": "+info.descr());
	    }


	    for(NodeInfo info: dom)
	    {
		final Node parent = info.getNode().getParentNode();
		if(domMap.containsKey(parent))
		    info.setParent(domMap.get(parent));
	    }
	    window = (JSObject)webEngine.executeScript("window");
	    return null;
	};
	FutureTask<Integer> query=new FutureTask<Integer>(task){};
	if(Platform.isFxApplicationThread())
	{ // direct call
	    try {
		task.call();
	    }
	    catch(Exception e) 
	    {
		e.printStackTrace();
	    }
	} else
	{
	    Platform.runLater(query);
	    try {
		query.get();
	    }
	    catch(InterruptedException e)
	    {
		Thread.currentThread().interrupt();
	    }
	    catch(ExecutionException e) 
	    {
		e.printStackTrace();
	    }
	}
	busy=false;
    }

    @Override public void setWatchNodes(Iterable<Integer> indexes)
    {
	Platform.runLater(()->
	{
		// check if injected object success
		if(luwrainJSobject==null||"_luwrain_".equals(luwrainJSobject.getMember("name")))
			return;
		// fill javascript array
		final JSObject js = (JSObject)luwrainJSobject.getMember("dom");
		JSObject watchArray=(JSObject)webEngine.executeScript("[]"); // FIXME: found correct method to create js array
		int j=0;
		for(int i:indexes)
			watchArray.setSlot(j++,i);
		// set watch member
	    luwrainJSobject.setMember("watch",watchArray);
	});
    }
    
    @Override public boolean isBusy()
    {
	return busy;
    }

    @Override public void Remove()
    {
	final int pos = interaction.browsers.indexOf(this);
	final boolean success = interaction.browsers.remove(this);
	if(!success) 
	    Log.warning("web","Can't found WebPage to remove it from WebEngineInteraction");
	setVisibility(false);
	if(pos!=-1)
	{
	    if(pos < interaction.browsers.size())
	    {
		interaction.setCurrentBrowser(interaction.browsers.get(pos));
	    }
	} else
	{
	    if(interaction.browsers.isEmpty()) 
		interaction.setCurrentBrowser(null); else 
	    	interaction.setCurrentBrowser(interaction.browsers.lastElement());
	}
    }

    @Override public void setVisibility(boolean enable)
    {
	if(enable)
	{
	    interaction.disablePaint();
	    Platform.runLater(()->{
		    webView.setVisible(true);
		    webView.requestFocus();
		});
	} else
	{
	    Platform.runLater(()->{
		    interaction.enablePaint();
		    webView.setVisible(false);
		});
	}
    }

    @Override public boolean getVisibility()
    {
	return webView.isVisible();
    }

    private boolean checkNodeForIgnoreChildren(Node node)
    {
    	if(node == null) 
	    return false;
    	final Node parent = node.getParentNode();
    	if(parent == null) 
	    return false;
    	if(parent instanceof HTMLAnchorElement)
	    return true;
    	return checkNodeForIgnoreChildren(parent);
    }

    @Override public void load(String url)
    {
	NullCheck.notNull(url, "url");
    	Platform.runLater(()->webEngine.load(url));
    }

    @Override public void loadContent(String text)
    {
	NullCheck.notNull(text, "text");
	Platform.runLater(()->webEngine.loadContent(text));
    }

    @Override public void stop()
    {
	Platform.runLater(()->webEngine.getLoadWorker().cancel());
    }

    @Override public String getTitle()
    {
		if(webEngine == null)
			return "";
		return webEngine.titleProperty().get();
    }

    @Override public String getUrl()
    {
		if(webEngine == null)
		    return "";
		return webEngine.getLocation();
    }

    @Override public Object executeScript(String script)
    {
	NullCheck.notNull(script, "script");
	if(webEngine == null)
	    return null;
	final Callable<Object> task = ()->{
		return webEngine.executeScript(script);
	};
	FutureTask<Object> query=new FutureTask<Object>(task){};
    final ExecutorService executor = Executors.newSingleThreadExecutor();
	if(Platform.isFxApplicationThread())
	{ // direct call
	    try {
	    	return task.call();
	    }
	    catch(Exception e) 
	    {
	    	e.printStackTrace();
	    	return null;
	    }
	} else
	{
	    Platform.runLater(query);
	    try {
	    	return query.get();
	    }
	    catch(InterruptedException e)
	    {
		Thread.currentThread().interrupt();
	    }
	    catch(ExecutionException e) 
	    {
		e.printStackTrace();
	    }
	    return null;
	}
    }

    @Override public ElementIterator iterator()
    {
	return new ElementIteratorImpl(this);
    }

    @Override public int numElements()
    {
	return domMap.size();
    }

    private void onStateChange(org.luwrain.browser.Events events, ObservableValue<? extends State> ov,
			       State oldState, State newState)
    {
	Log.debug("javafx","browser state changed to: "+newState.name()+", "+webEngine.getLoadWorker().getState().toString()+", url:"+webEngine.getLocation());
	if(newState == State.CANCELLED)
	{ // if canceled not by user, so that is a file downloads
	    if(!userStops)
	    { // if it not by user
		if(events.onDownloadStart(webEngine.getLocation())) 
		    return;
	    }
	}
	final WebState state;
	switch(newState)
	{
	case CANCELLED:
	    state=WebState.CANCELLED;
	    break;
	case FAILED:	
	    state=WebState.FAILED;
	    break;
	case READY:		
	    state=WebState.READY;
	    break;
	case RUNNING:	
	    state=WebState.RUNNING;
	    break;
	case SCHEDULED:	
	    state=WebState.SCHEDULED;
	    break;
	case SUCCEEDED:	
	    state=WebState.SUCCEEDED;
	    break;
	default:
	    state = WebState.CANCELLED;
	}
	//
	switch(newState)
	{
		//case READY:
		case SUCCEEDED:
	    	luwrainJSobject=(JSObject)webEngine.executeScript(luwrainJS);
	    	// FIXME: check that luwrain object exists
    	break;
		default:
			luwrainJSobject=null;
	}
	events.onChangeState(state);
    }

    private void onKeyReleased(KeyEvent event)
    {
	//Log.debug("web","KeyReleased: "+event.toString());
	switch(event.getCode())
	{
	case ESCAPE:
	    interaction.setCurrentBrowserVisibility(false);break;
	default:break;
	}
    }

    
	static public long jsLong(Object o)
	{
		if(o==null) return 0;
		if(o instanceof Double) return (long)(double)o;
		if(o instanceof Integer) return (long)(int)o;
		//throw new Exception("js have unknown number type: "+o.getClass().getName());
		// FIXME: it can be happened or not?
		return (long)Double.parseDouble(o.toString());
	}
    static ClassLoader cl=ClassLoader.getSystemClassLoader();
	static public String luwrainJS;
    /** load resource text file as javascript and replace to luwrain member */
    static String getJSResource(String path)
    {
    	try
    	{
    		InputStream inputStream=cl.getResourceAsStream(path);
	    	ByteArrayOutputStream result = new ByteArrayOutputStream();
	    	byte[] buffer = new byte[1024];
	    	int length;
	    	while ((length = inputStream.read(buffer)) != -1) {
	    	    result.write(buffer, 0, length);
	    	}
   			String txt=result.toString("UTF-8");
   			Log.warning("javafx-dom","Loaded resource js: "+path+" "+txt.length()+" bytes");
   			return txt;
    	}
    	catch(Exception e)
    	{
    		Log.error("javafx-dom","Loading resource error: "+path+" not loaded");
    		e.printStackTrace();
    		return null;
    	}
    }
	static
	{
   		luwrainJS=getJSResource("resources/luwrainJS.js");
	}

	@Override public long getLastTimeChanged()
	{
		return lastModifiedTime;
	}
}
