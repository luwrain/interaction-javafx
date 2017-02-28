/*
   Copyright 2015-2016 Roman Volovodov <gr.rPman@gmail.com>
   Copyright 2012-2016 Michael Pozhidaev <michael.pozhidaev@gmail.com>

   This file is part of the LUWRAIN.

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

import javafx.application.Platform;
import javafx.scene.paint.Color;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.luwrain.base.InteractionParamColor;

class Utils
{
    static Color InteractionParamColorToFx(InteractionParamColor ipc)
    {
	if(ipc.getPredefined()==null)
	    return new Color(ipc.getRed()/256,ipc.getGreen()/256,ipc.getBlue()/256,1);
	switch(ipc.getPredefined())
	{
	case BLACK:		return Color.BLACK;
	case BLUE:		return Color.BLUE;
	case CYAN:		return Color.CYAN;
	case DARK_GRAY:	return Color.DARKGRAY;
	case GRAY:		return Color.GRAY;
	case GREEN:		return Color.GREEN;
	case LIGHT_GRAY:return Color.LIGHTGRAY;
	case MAGENTA:	return Color.MAGENTA;
	case ORANGE:	return Color.ORANGE;
	case PINK:		return Color.PINK;
	case RED:		return Color.RED;
	case WHITE:		return Color.WHITE;
	case YELLOW:	return Color.YELLOW;	
	    // WARN: not predefined colors have opacity = 1
	default: 		return new Color(ipc.getRed()/256,ipc.getGreen()/256,ipc.getBlue()/256,1);
	}
    }
}
