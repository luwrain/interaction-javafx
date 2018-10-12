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

import javafx.embed.swing.*;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.input.KeyEvent;

import org.luwrain.core.*;

final class PdfPreview
{
    static final String LOG_COMPONENT = "browser";
    
    private final JavaFxInteraction interaction;
    private SwingNode node = null;

    PdfPreview(JavaFxInteraction interaction)
    {
	NullCheck.notNull(interaction, "interaction");
	this.interaction = interaction;
    }

void init()
    {
	Utils.ensureFxThread();
	try {
	    this.node = new SwingNode();
this.node.setOnKeyReleased((event)->onKeyReleased(event));
	    this.node.setVisible(false);
	    this.node.requestFocus();
	}
	catch(Throwable e)
	{
	    Log.error(LOG_COMPONENT, "unable to initialize the PDF preview:" + e.getClass().getName() + ":" + e.getMessage());
	    this.node = null;
	}
    }

void close()
    {
	//	Utils.runInFxThreadSync(()->interaction.closeBrowser(this));
    }

        private void onKeyReleased(KeyEvent event)
    {
	NullCheck.notNull(event, "event");
	switch(event.getCode())
	{
	case ESCAPE:
	    //	    setVisibility(false);
	    break;
	default:break;
	}
    }
}
