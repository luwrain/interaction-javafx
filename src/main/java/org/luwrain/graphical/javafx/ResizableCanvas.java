

package org.luwrain.graphical.javafx;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;

import org.luwrain.core.*;

public final class ResizableCanvas extends Canvas
{
    public ResizableCanvas()
    {
	this(1024, 768);
    }

    public ResizableCanvas(int width, int height)
    {
	super(width, height);
    }


    @Override public boolean isResizable() 
    {
	return true;
    }

    @Override public double prefWidth(double height) 
    {
	return getWidth();
    }

    @Override public double prefHeight(double width) 
    {
	return getHeight();
    }

    public void bindWidthAndHeight(StackPane pane)
    {
	NullCheck.notNull(pane, "pane");
	widthProperty().bind(pane.widthProperty());
	heightProperty().bind(pane.heightProperty());
    }
}
