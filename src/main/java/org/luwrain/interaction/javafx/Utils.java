
package org.luwrain.interaction.javafx;

import javafx.scene.paint.Color;
import org.luwrain.core.InteractionParamColor;

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
