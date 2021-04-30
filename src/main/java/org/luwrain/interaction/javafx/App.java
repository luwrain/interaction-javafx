/*
   Copyright 2012-2020 Michael Pozhidaev <msp@luwrain.org>
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

import java.io.*;

import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.stage.*;
import javafx.scene.canvas.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import org.luwrain.core.*;
import org.luwrain.graphical.*;

public final class App extends Application
{
    static private final String LOG_COMPONENT = JavaFxInteraction.LOG_COMPONENT;

    static private final int
	MIN_TABLE_WIDTH = 16,
	MIN_TABLE_HEIGHT = 8;

    private StackPane rootPane = null;
    private Stage stage = null;
    private ResizableCanvas textCanvas = null;
    private GraphicsContext gc = null;
    private Bounds charBounds = null;
    private Font font = null;
    private     Font font2 = null;
    private Color fontColor = Color.GREY;
    private Color font2Color = Color.WHITE;
    private Color bkgColor = Color.BLACK;
    private Color bkgColor2 = Color.BLACK;
    private Color splitterColor = Color.GRAY;
    private double canvasWidth;
    private double canvasHeight;

    private int hotPointX = -1;
    private int hotPointY = -1;
    private int marginLeft = 0;
    private int marginTop = 0;
    private int marginRight = 0;
    private int marginBottom = 0;
    private int tableWidth = 0;
    private int tableHeight = 0;
    private char[][] table;
    private boolean[][] tableFont2;
    private OnScreenLineTracker[] vertLines;
    private OnScreenLineTracker[] horizLines;

    @Override public void start(Stage stage) throws Exception
    {
	NullCheck.notNull(stage, "stage");
	this.stage = stage;
	stage.setResizable(true);
	stage.setTitle("LUWRAIN");
	this.rootPane = new StackPane();
	this.rootPane.resize(1024, 768);
        this.textCanvas = new ResizableCanvas();
        this.rootPane.getChildren().add(textCanvas);
        stage.setScene(new Scene(rootPane));
	this.textCanvas.bindWidthAndHeight(rootPane);
        this.gc = textCanvas.getGraphicsContext2D();
        this.rootPane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
	ThreadControl.appStarted(this);
    }

    @SuppressWarnings("deprecation") 
    void setInteractionFont(Font font, Font font2)
    {
	NullCheck.notNull(font, "font");
	NullCheck.notNull(font2, "font2");
    	this.font = font;
    	this.font2 = font2;
	final Text text = new Text("A");
	text.setFont(font);
        this.charBounds = text.getLayoutBounds();
    }

    synchronized boolean initTable()
    {
	FxThread.ensure();
	double width = canvasWidth;
	double height = canvasHeight;
	if (width < marginLeft + marginRight)
	{
	    Log.error(LOG_COMPONENT, "table initialization failure: left + right margins are greater than window width (" + marginLeft + "+" + marginRight + "<" + width + ")");
	    return false;
	}
	if (height < marginTop + marginBottom)
	{
	    Log.error(LOG_COMPONENT, "table initialization failure: top + bottom margins are greater than window height (" + marginTop + "+" + marginBottom + "<" + height + ")");
	    return false;
	}
	width -= (marginLeft + marginRight);
	height -= (marginTop + marginBottom);
	final int width_=(int)Math.floor(width/charBounds.getWidth());
	final int height_=(int)Math.floor(height/charBounds.getHeight());
	if (width_ < MIN_TABLE_WIDTH || height_ < MIN_TABLE_HEIGHT)
	{
	    Log.error(LOG_COMPONENT, "too small table for initialization:" + width_ + "x" + height_);
	    return false;
	}
	tableWidth = width_;
	tableHeight = height_;
	table = new char[tableWidth][];
	tableFont2=new boolean[tableWidth][];
	for(int i = 0;i < tableWidth;i++)
	{
	    table[i] = new char[tableHeight];
	    tableFont2[i]=new boolean[tableHeight];
	}
	for(int i = 0;i < tableWidth;i++)
	    for(int j = 0;j < tableHeight;j++)
	    {
		table[i][j] = ' ';
		tableFont2[i][j]=false;
	    }
	vertLines = new OnScreenLineTracker[tableWidth];
	for(int i = 0;i < tableWidth;i++)
	    vertLines[i] = new OnScreenLineTracker();
	horizLines = new OnScreenLineTracker[tableHeight];
	for(int i = 0;i < tableHeight;i++)
	    horizLines[i] = new OnScreenLineTracker();
	return true;
    }

    void setColors(Color fontColor, Color font2color,
		   Color bkgColor, Color splitterColor)
    {
	NullCheck.notNull(fontColor, "fontColor");
	NullCheck.notNull(font2Color, "font2Color");
	NullCheck.notNull(bkgColor, "bkgColor");
	NullCheck.notNull(splitterColor, "splitterColor");
	this.fontColor = fontColor;
	this.font2Color = font2Color;
	this.bkgColor = bkgColor;
	this.splitterColor = splitterColor;
    }

    void setMargin(int marginLeft,int marginTop,int marginRight,int marginBottom)
    {
	//FIXME:May not be negative
	this.marginLeft = marginLeft;
	this.marginTop = marginTop;
	this.marginRight = marginRight;
	this.marginBottom = marginBottom;
    }

    void setSizeAndShow(int width, int height)
    {
	FxThread.ensure();
    	this.canvasWidth = width;
    	this.canvasHeight = height;
    	this.rootPane.resize(width, height);
    	this.stage.sizeToScene();
    	this.stage.show();
    }

    void setUndecoratedSizeAndShow(double width, double height)
    {
	FxThread.ensure();
    	this.canvasWidth = width;
    	this.canvasHeight = height;
    	this.rootPane.resize(width, height);
    	//canvas.resize(width, height);
    	this.stage.initStyle(StageStyle.UNDECORATED); // WARN: can't change style after first window show
    	this.stage.setWidth(width);
    	this.stage.setHeight(height);
    	this.stage.setResizable(false);
    	this.stage.show();
    }

    void setHotPoint(int x, int y)
    {
	if (x < 0 || y < 0)
	{
	    hotPointX = -1;
	    hotPointY = -1;
	    return;
	}
	hotPointX = x;
	hotPointY = y;
    }

    synchronized void putString(int x, int y, String text, boolean withFont2)
    {
	NullCheck.notNull(text, "text");
	if (table == null || x >= tableWidth || y >= tableHeight ||
	    x >= table.length || y >= table[x].length)
	    return;
	final int bound = x + text.length() <= tableWidth?text.length():tableWidth - x;
	for(int i = 0;i < bound;i++)
	{
	    table[x + i][y] = text.charAt(i) != '\0'?text.charAt(i):' ';
	    tableFont2[x + i][y] = withFont2;
	}
    }

    synchronized void clearRect(int left,int top,int right,int bottom)
    {
	if (table == null || tableWidth <= 0 || tableHeight <= 0) return;
	final int l = left >= 0?left:0;
	final int t = top >= 0?top:0;
	final int r = right < tableWidth?right:(tableWidth - 1);
	final int b = bottom < tableHeight?bottom:(tableHeight - 1);
	if (l > r || t > b) return;
	for(int i = l;i <= r;i++)
	    for(int j = t;j <= b;j++)
	    {
		table[i][j] = ' ';
		tableFont2[i][j]=false;
	    }
	if (vertLines != null)
	    for(int i = l;i <= r;i++)
		vertLines[i].uncover(t, b);
	if (horizLines != null)
	    for(int i = t;i <= b;i++)
		horizLines[i].uncover(l, r);
    }

    synchronized void paint()
    {
	FxThread.ensure();
	final double fontWidth = charBounds.getWidth();
	final double fontHeight = charBounds.getHeight();
	if(table==null)
	    return;
	gc.setTextBaseline(VPos.TOP);
	// canvas is transparent, so background color was filled rect before
	gc.setFill(bkgColor);
	gc.fillRect(0, 0, this.stage.getWidth() - 1, this.stage.getHeight()-1);
	gc.setFont(font);
	gc.setFill(fontColor);
	char[] chars=new char[tableWidth];
	for(int i=0;i<tableHeight;i++)
	{
	    for(int j=0;j<tableWidth;j++)
		chars[j]=tableFont2[j][i]?' ':table[j][i];
	    gc.fillText(new String(chars),marginLeft,(i*fontHeight)+marginTop);
	}
	gc.setFont(font2);
	gc.setFill(font2Color);
	for(int i=0;i<tableHeight;i++)
	{
	    for(int j=0;j<tableWidth;j++)
		if(tableFont2[j][i])
		    gc.fillText(""+table[j][i],marginLeft+j*fontWidth,(i*fontHeight)+marginTop);
	}
	gc.setFill(splitterColor);
	// Vertical lines;
	if(vertLines!=null) 
	    for(int i=0;i<vertLines.length;i++)
		if(vertLines[i]!=null)
		{
		    OnScreenLine[] lines=vertLines[i].getLines();
		    for(int k=0;k<lines.length;k++)
		    {
			gc.fillRect(marginLeft+(i*fontWidth)+(fontWidth/2)-(fontWidth/6),marginTop+(lines[k].pos1*fontHeight),(fontWidth/3),
				    (lines[k].pos2-lines[k].pos1+1)*fontHeight);
		    }
		}
	// Horizontal lines;
	if(horizLines!=null) 
	    for(int i=0;i<horizLines.length;i++)
		if(horizLines[i]!=null)
		{
		    OnScreenLine[] lines=horizLines[i].getLines();
		    for(int k=0;k<lines.length;k++)
		    {
			gc.fillRect(marginLeft+(lines[k].pos1*fontWidth),marginTop+(i*fontHeight)+(fontHeight/2)-(fontWidth/6),
				    (lines[k].pos2-lines[k].pos1+1)*fontWidth,fontWidth/3);
		    }
		}
	// Hot point;
	if(hotPointX>=0&&hotPointY>=0&&hotPointX<tableWidth&&hotPointY<tableHeight)
	{
	    gc.setFill(fontColor);
	    gc.fillRect(
			(hotPointX*fontWidth)+marginLeft,
			(hotPointY*fontHeight)+marginTop,
			fontWidth,
			fontHeight);
	    gc.setFill(bkgColor);
	    String str=new String();
	    str+=table[hotPointX][hotPointY];
	    gc.fillText(str,
			(hotPointX*fontWidth)+marginLeft,
			(hotPointY*fontHeight)+marginTop);
	}
    }

    synchronized void drawVerticalLine(int top,int bottom,int x)
    {
	if (vertLines == null) return;
	if (x >= vertLines.length)
	{
	    Log.warning(LOG_COMPONENT, "unable to draw vertical line at column " + x + ", max vertical line is allowed at " + (vertLines.length - 1));
	    return;
	}
	if (vertLines[x] != null)
	    vertLines[x].cover(top, bottom);
    }

    synchronized void drawHorizontalLine(int left,int right,int y)
    {
	if (horizLines == null)
	    return;
	if (y >= horizLines.length)
	{
	    Log.warning(LOG_COMPONENT, "unable to draw horizontal line at row " + y + ", max horizontal line is allowed at " + (horizLines.length - 1));
	    return;
	}
	if (horizLines[y] != null)
	    horizLines[y].cover(left, right);
    }

    void putNew(Node node)
    {
	NullCheck.notNull(node, "node");
	rootPane.getChildren().add(node);
	if (node instanceof ResizableCanvas)
	    ((ResizableCanvas)node).bindWidthAndHeight(rootPane);
    }

    void remove(Node node)
    {
	NullCheck.notNull(node, "node");
	rootPane.getChildren().remove(node);
    }

    Font getInteractionFont()
    {
	return 	font;
    }

    Font getInteractionFont2()
    {
	return 	font;
    }

    int getTableWidth()
    {
	return tableWidth;
    }

    int getTableHeight()
    {
	return tableHeight;
    }

    Stage getStage()
    {
	return this.stage;
    }
}
