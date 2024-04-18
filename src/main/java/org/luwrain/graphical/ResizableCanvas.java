/*
   Copyright 2012-2024 Michael Pozhidaev <msp@luwrain.org>
   Copyright 2015-2016 Roman Volovodov <gr.rPman@gmail.com>

   This file is part of LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.graphical;

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
