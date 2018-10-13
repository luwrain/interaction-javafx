/*
   Copyright 2012-2018 Michael Pozhidaev <michael.pozhidaev@gmail.com>
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

package org.luwrain.interaction.javafx;

import java.awt.Rectangle;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import java.awt.image.BufferedImage;
import javafx.scene.image.Image;
import javafx.scene.canvas.GraphicsContext;
import javafx.embed.swing.*;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.input.KeyEvent;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

final class PdfPreview implements org.luwrain.interaction.graphical.Pdf
{
    static final String LOG_COMPONENT = "pdf";

    private final JavaFxInteraction interaction;
    private ResizableCanvas canvas = null;
    private final org.luwrain.interaction.graphical.Pdf.Listener listener;

        private final PDDocument doc;
    private final PDFRenderer rend;
    private Image image = null;

    PdfPreview(JavaFxInteraction interaction, org.luwrain.interaction.graphical.Pdf.Listener listener, File file) throws Exception
    {
	NullCheck.notNull(interaction, "interaction");
	NullCheck.notNull(listener, "listener");
	this.interaction = interaction;
	this.listener = listener;
	this.doc = PDDocument.load(file);
	Log.debug(LOG_COMPONENT, "PDF file " + file.getAbsolutePath() + " loaded");
	this.rend = new PDFRenderer(doc);
	Log.debug(LOG_COMPONENT, "PDF renderer created");
    }

    @Override public boolean init()
    {
	Utils.runInFxThreadSync(()->{
	try {
	    this.image = makeImage(1, 3);
	    this.canvas = new ResizableCanvas();
	    this.canvas.setOnKeyReleased((event)->onKeyReleased(event));
	    this.canvas.setVisible(false);
	    interaction.registerCanvas(this.canvas);
	    canvas.setVisible(true);
	    interaction.enableGraphicalMode();
	    this.canvas.requestFocus();
	    draw();
	}
	catch(Throwable e)
	{
	    Log.error(LOG_COMPONENT, "unable to initialize the PDF preview:" + e.getClass().getName() + ":" + e.getMessage());
	    this.canvas = null;
	}
	    });
	return true;
    }

    @Override public void close()
    {
	Utils.runInFxThreadSync(()->{
		interaction.closeCanvas(this.canvas);
		interaction.disableGraphicalMode();
	    });
    }

    @Override public boolean showPage(int index)
    {
	return false;
    }

    @Override public int getPageCount()
    {
	return 0;
    }

    @Override public int getCurrentPageNum()
    {
	return 0;
    }

    private void draw()
    {
		InvalidThreadException.checkThread("PdfPreview.draw()");
		NullCheck.notNull(canvas, "canvas");
		NullCheck.notNull(image, "image");

final GraphicsContext gc = canvas.getGraphicsContext2D();
gc.drawImage(image, 0, 0);

    }

    private Image makeImage(int pageNum, float scale)
    {
	InvalidThreadException.checkThread("PdfPreview.makeImage");
	        final BufferedImage pageImage;
        try {
            pageImage = rend.renderImage(pageNum, scale);
	    Log.debug(LOG_COMPONENT, "image rendered");
        }
	catch (IOException e)
	{
	    Log.error(LOG_COMPONENT, "unable to render a PDf page:" + e.getClass().getName() + ":" + e.getMessage());
	    return null;
        }
return SwingFXUtils.toFXImage(pageImage, null);
    }

    private void onKeyReleased(KeyEvent event)
    {
	NullCheck.notNull(event, "event");
	switch(event.getCode())
	{
	case ESCAPE:
	    listener.onInputEvent(new KeyboardEvent(KeyboardEvent.Special.ESCAPE));
	    break;
	default:break;
	}
    }
}
