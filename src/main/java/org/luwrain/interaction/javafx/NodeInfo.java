
package org.luwrain.interaction.javafx;

class NodeInfo
{
final org.w3c.dom.Node node;
    Integer parent = null;
    java.awt.Rectangle rect;
    boolean forText;
    int hash;
    long hashTime=0;

    NodeInfo(org.w3c.dom.Node node)
    {
	this.node = node;
    }

    boolean isVisible()
    {
	return rect.width > 0 && rect.height > 0;
    }

    void calcHash(String text)
    {
	hash=text.hashCode();
	hashTime=new java.util.Date().getTime();
    }

    String descr()
    {
	//	return node.getClass().getName().substring(node.getClass().getName().lastIndexOf(".")) + " " + node.getNodeValue();
	return node.getNodeName() + " " + node.getNodeValue();
    }
}
