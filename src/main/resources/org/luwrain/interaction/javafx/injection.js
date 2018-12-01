
new function ()
{
	this.name='$LUWRAIN';
	/** list nodes indexes (in dom list) to watch for changes */
	this.watch=[];
	/** last result of scanDOM must be store here */
	this.domLast=[];
	/** dom structure, equal domLast but was replaced each time detected modification */
	this.dom=[];
	/** last date in milliseconds when dom was rescanned and found modifications, to detect changes we must store last value to compare with it later */
	this.domLastTime=0;
	/** count of visible elements for dom and domLast */
	this.countVisible=0;
	this.countVisibleLast=0;

	/** next interval in milliseconds to rescan */
	this.updateTimeout=3000;
	
	/** performance check, store timing for last method calls */
	this.domLT=0;
	this.domLastLT=0;
	
	/** recursive method to return nodes' array about children of specified node
	 * @param node target node */
	this.nodewalk = function(node, lvl)
	{
		var res=[];
		if(node)
		{
			node=node.firstChild;
			while(node!= null)
			{
				//if(node.nodeType!=3 || node.nodeValue.trim()!=='')
				{
					res[res.length]=node;
				}
				res=res.concat(this.nodewalk(node,lvl+1));
				node=node.nextSibling;
			}
		}
		//alert('nodewalk:'+node+" - "+res.length);
		return res;
	};
	/** scan full document structure and return planar array of node info as object:{n:node,r:rectangle or null,h:content_hash or null} */
	this.scanDOM=function()
	{
		this.countVisibleLast=0;
		var lst=this.nodewalk(document,0);
		var res=[];
		for(var i=0;i<lst.length;i++)
		{
			var n={
				n:lst[i],
				r:(
					lst[i].getBoundingClientRect?lst[i].getBoundingClientRect():
					((function(nnn)
					{
						try
						{
							var range=document.createRange();
							range.selectNodeContents(nnn);
							return range.getBoundingClientRect();
						}
						catch(e)
						{
							return null;
						};
					})(lst[i]))
				),
				h:this.getNodeHash(lst[i])
			};
			if(n.r!=null&&(n.r.width==0||n.r.height==0))
				this.countVisibleLast++;
			res.push(n);
		};
		//console.log('this.countVisibleLast='+this.countVisibleLast);
		return res;
	};
	/** set nodes indexes list to observe modification text or position 
	 * @param nodes array of nodes*/
	this.setObserve=function(nodes)
	{
		this.watch=nodes;
	}
	/** return node content hash number to detect value modification, except rectangle and visibility, scan is NOT recursive */
	this.getNodeHash=function(node)
	{ 
		var content='';
		switch(node.nodeType)
		{
			case 3:
				content=node.nodeValue;
			break;
			case 1:
				var name=node.nodeName.toLowerCase();
				switch(name)
				{
				case 'input':
					var type=node.getAttribute('type');
					switch(type)
					{
					case 'radio':
						content=''+node.checked;
					break;
					default:
						content=node.value;
					}
					
				break;
				default:
					content=node.nodeValue;
				}
			break;
		}
		return this.hash(content);
	}
	this.hash=function(str)
	{ // https://github.com/darkskyapp/string-hash/
		if(str==null) return 0; 
		var hash = 5381, i = str.length;
		while(i)
			hash = (hash * 33) ^ str.charCodeAt(--i);
		/* JavaScript does bitwise operations (like XOR, above) on 32-bit signed
	   	 integers. Since we want the results to be always positive, convert the
	   	 signed int to an unsigned by doing an unsigned bitshift. */
		return hash >>> 0;
	}
	
	this.onTimeout=function()
	{
		/**/var t=new Date().getTime();
		this.domLast=this.scanDOM();
		/**/this.domLastLT=(new Date().getTime())-t;
		// check for changes
		var modified=false;
		// dom size changed?
		if(this.domLast.length!=this.dom.length)
			modified=true;
		else
		// dom visible changed
		if(this.countVisibleLast!=this.countVisible)
			modified=true;
		else
		{
			// scan for watched nodes
			for(var i=0;i<this.watch.length;i++) if(this.watch[i] !== undefined)
			{
				var index=this.watch[i];
				if(this.dom[index] === undefined||this.domLast[index] === undefined)
				{ // dom or domLast element by index not exist
					modified=true;
					break;
				}
				if(this.dom[index].h!=this.domLast[index].h)
				{ // hash of elements not equal
					modified=true;
					break;
				}
				var r=this.dom[index].r;
				var u=this.domLast[index].r;
				if(r.top!=u.top||r.height!=u.height||r.left!=u.left||r.width!=u.width)
				{ // visibility or position changed
					modified=true;
					break;
				}
			}
		}
		//
		//
		if(modified)
		{
			this.dom=this.domLast;
			this.domLastTime=new Date().getTime();
			this.countVisible=this.countVisibleLast;
			/**/this.domLT=this.domLastLT;
			/**/this.scanLT=(new Date().getTime())-t;
		}
		// set next time for rescan
		this.timerid=setTimeout(function(that){that.onTimeout();},this.updateTimeout,this);
	};
	/** do update */
	this.doUpdate=function()
	{
		clearTimeout(this.timerid);
		this.timerid=setTimeout(function(that){that.onTimeout();},200,this);
	}
	// start auto scanning via setTimeout as fast as possible after class object created
	this.onTimeout();
	//setTimeout(function(that){that.onTimeout();},1000,this);
};
