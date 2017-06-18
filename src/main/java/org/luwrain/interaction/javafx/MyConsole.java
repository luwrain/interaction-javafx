
package org.luwrain.interaction.javafx;

import org.luwrain.core.*;

class MyConsole
{
    static private final String LOG_COMPONENT = JavaFxInteraction.LOG_COMPONENT;

    void log(Object text)
    {
	Log.info(LOG_COMPONENT, text.toString());
    }
}
