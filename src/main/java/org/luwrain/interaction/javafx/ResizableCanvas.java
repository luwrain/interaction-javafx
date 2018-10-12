

package org.luwrain.interaction.javafx;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;

import org.luwrain.core.*;

final class ResizableCanvas extends Canvas
{
    ResizableCanvas()
    {
	super(1024, 768);
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

    void bindWidthAndHeight(StackPane pane)
    {
	NullCheck.notNull(pane, "pane");
	widthProperty().bind(pane.widthProperty());
	heightProperty().bind(pane.heightProperty());
    }
}
