
package org.luwrain.graphical.browser;

import java.util.*;

import com.sun.webkit.dom.DOMWindowImpl;
import org.w3c.dom.Node;

import org.luwrain.core.*;

final class DomScanResult
{
    final DOMWindowImpl window;
    final List<NodeInfo> dom = new ArrayList();
    final Map<Node, Integer> domMap = new HashMap();

    DomScanResult(DOMWindowImpl window)
    {
	NullCheck.notNull(window, "window");
	this.window = window;
    }
}
