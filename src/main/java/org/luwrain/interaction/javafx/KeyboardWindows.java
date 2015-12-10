package org.luwrain.interaction.javafx;

import org.luwrain.core.EventConsumer;
import org.luwrain.core.events.KeyboardEvent;
import org.luwrain.os.Keyboard;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class KeyboardWindows implements Keyboard
{
	EventConsumer eventConsumer;
    public boolean leftAltPressed = false;
    public boolean rightAltPressed = false;
    public boolean controlPressed = false;
    public boolean shiftPressed = false;

    @Override public void setEventConsumer(EventConsumer eventConsumer)
	{
		this.eventConsumer=eventConsumer;
	}

    @Override public void onKeyPressed(Object _event)
    {
    KeyEvent event=(KeyEvent)_event;
	controlPressed=event.isControlDown();
	shiftPressed=event.isShiftDown();
	leftAltPressed=event.isAltDown();
	if(eventConsumer==null)
	    return;
	int code;
	switch(event.getCode())
	{
	    // Functions keys
	case F1:code=KeyboardEvent.F1;break;
	case F2:code=KeyboardEvent.F2;break;
	case F3:code=KeyboardEvent.F3;break;
	case F4:code=KeyboardEvent.F4;break;
	case F5:code=KeyboardEvent.F5;break;
	case F6:code=KeyboardEvent.F6;break;
	case F7:code=KeyboardEvent.F7;break;
	case F8:code=KeyboardEvent.F8;break;
	case F9:code=KeyboardEvent.F9;break;
	case F10:code=KeyboardEvent.F10;break;
	case F11:code=KeyboardEvent.F11;break;
	case F12:code=KeyboardEvent.F12;break;
	    // Arrows;
	case LEFT:code=KeyboardEvent.ARROW_LEFT;break;
	case RIGHT:code=KeyboardEvent.ARROW_RIGHT;break;
	case UP:code=KeyboardEvent.ARROW_UP;break;
	case DOWN:code=KeyboardEvent.ARROW_DOWN;break;
	    // Jump keys;
	case HOME:code=KeyboardEvent.HOME;break;
	case END:code=KeyboardEvent.END;break;
	case INSERT:code=KeyboardEvent.INSERT;break;
	case PAGE_DOWN:code=KeyboardEvent.PAGE_DOWN;break;
	case PAGE_UP:code=KeyboardEvent.PAGE_UP;break;
	case WINDOWS:code=KeyboardEvent.WINDOWS;break;
	case CONTEXT_MENU:code=KeyboardEvent.CONTEXT_MENU;break;
	    // modificators
	case CONTROL:code=KeyboardEvent.CONTROL;break;
	case SHIFT:code=KeyboardEvent.SHIFT;break;
	case ALT:code=KeyboardEvent.LEFT_ALT;break;
	case ALT_GRAPH:
	    code=KeyboardEvent.RIGHT_ALT;
	    break;
	default:
	      final String ch=event.getText();
	      if((shiftPressed||leftAltPressed||rightAltPressed)&&!ch.isEmpty())
	      {
	      final KeyboardEvent emulated=new KeyboardEvent(false,0,ch.toLowerCase().charAt(0),shiftPressed,controlPressed,leftAltPressed,rightAltPressed);
	      eventConsumer.enqueueEvent(emulated);
	      }
	    return;
	}
	eventConsumer.enqueueEvent(new KeyboardEvent(true,code,' ',shiftPressed,controlPressed,leftAltPressed,rightAltPressed));
    }

    @Override public void onKeyReleased(KeyEvent event)
    {
	controlPressed=event.isControlDown();
	shiftPressed=event.isShiftDown();
	leftAltPressed=event.isAltDown();
    }

    @Override public void onKeyTyped(KeyEvent event)
    {
	controlPressed=event.isControlDown();
	shiftPressed=event.isShiftDown();
	leftAltPressed=event.isAltDown();
	if(eventConsumer==null) 
	    return;
	final String keychar=event.getCharacter();
	int code;
	if(keychar.equals(KeyCode.BACK_SPACE.impl_getChar()))
	    code=KeyboardEvent.BACKSPACE; else
	    if(keychar.equals(KeyCode.ENTER.impl_getChar())||keychar.equals("\n")||keychar.equals("\r")) 
		code=KeyboardEvent.ENTER; else 
		if(keychar.equals(KeyCode.ESCAPE.impl_getChar())) 
		    code=KeyboardEvent.ESCAPE; else
		    if(keychar.equals(KeyCode.DELETE.impl_getChar())) 
			code=KeyboardEvent.DELETE; else 
			if(keychar.equals(KeyCode.TAB.impl_getChar())) 
			    code=KeyboardEvent.TAB; else
			{
			    // FIXME: javafx characters return as String type we need a char (now return first symbol)
			    final KeyboardEvent emulated=new KeyboardEvent(false, 0,
									   event.getCharacter().charAt(0),
									   shiftPressed,controlPressed,leftAltPressed,rightAltPressed);
			    eventConsumer.enqueueEvent(emulated);
			    return;
			}
	//	final int _code=code;
	eventConsumer.enqueueEvent(new KeyboardEvent(true, code, ' ',
						     shiftPressed,controlPressed,leftAltPressed,rightAltPressed));
    }
}
