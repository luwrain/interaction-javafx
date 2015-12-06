
package org.luwrain.interaction.javafx;

class NodeInfo
{
    org.w3c.dom.Node node;
    java.awt.Rectangle rect;
    boolean forTEXT;
    int hash;
    long hashTime=0;

    public boolean isVisible()
    {
	return rect.width>0&&rect.height>0;
    }

    void calcHash(String text)
    {
	hash=text.hashCode();
	hashTime=new java.util.Date().getTime();
    }
}
