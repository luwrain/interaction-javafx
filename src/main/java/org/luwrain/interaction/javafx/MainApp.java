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

import org.luwrain.core.Log;
import org.luwrain.core.NullCheck;

import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.stage.*;
import javafx.scene.canvas.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

public final class MainApp extends Application
{
    static private final int MIN_TABLE_WIDTH = 16;
    static private final int MIN_TABLE_HEIGHT = 8;

    StackPane root;
    boolean doPaint=true;
    Stage primary;

    private ResizableCanvas canvas;
    private GraphicsContext gc;
    private Bounds bounds;
    private Font font;
    private     Font font2;
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

    //For synchronizing
    private final Object tableSync = new Object();
    private final Object vertSync = new Object();
    private final Object horizSync = new Object();

    @Override public void start(final Stage primary) throws Exception
    {
	NullCheck.notNull(primary, "primary");
	this.primary=primary;
	primary.setResizable(true);
	root=new StackPane();
	root.resize(1024, 768);
        canvas = new ResizableCanvas();
        root.getChildren().add(canvas);
        primary.setScene(new Scene(root));
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());
        gc = canvas.getGraphicsContext2D();
        root.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
		ThreadControl.appStarted(this);
    }

    @SuppressWarnings("deprecation") 
    void setInteractionFont(Font font, Font font2)
    {
	NullCheck.notNull(font, "font");
	NullCheck.notNull(font2, "font2");
    	this.font=font;
    	this.font2=font2;
        bounds = TextBuilder.create().text("A").font(font).build().getLayoutBounds();
    }

    Font getInteractionFont()
    {
	return 	font;
    }

    Font getInteractionFont2()
    {
	return 	font;
    }

    boolean initTable()
    {
	double width = canvasWidth;
	double height = canvasHeight;
	if (width < marginLeft + marginRight)
	{
	    Log.error("javafx", "table initialization failure: left + right margins are greater than window width (" + marginLeft + "+" + marginRight + "<" + width + ")");
	    return false;
	}
	if (height < marginTop + marginBottom)
	{
	    Log.error("javafx", "table initialization failure: top + bottom margins are greater than window height (" + marginTop + "+" + marginBottom + "<" + height + ")");
	    return false;
	}
	width -= (marginLeft + marginRight);
	height -= (marginTop + marginBottom);
	final int width_=(int)Math.floor(width/bounds.getWidth());
	final int height_=(int)Math.floor(height/bounds.getHeight());
	if (width_ < MIN_TABLE_WIDTH || height_ < MIN_TABLE_HEIGHT)
	{
	    Log.error("javafx", "too small table for initialization:" + width_ + "x" + height_);
	    return false;
	}
	tableWidth = width_;
	tableHeight = height_;
	synchronized(tableSync)
	{
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
	}
	synchronized(vertSync)
	{
	    vertLines = new OnScreenLineTracker[tableWidth];
	    for(int i = 0;i < tableWidth;i++)
		vertLines[i] = new OnScreenLineTracker();
	}
	synchronized(horizSync)
	{
	    horizLines = new OnScreenLineTracker[tableHeight];
	    for(int i = 0;i < tableHeight;i++)
		horizLines[i] = new OnScreenLineTracker();
	}
	return true;
    }

    int getTableWidth()
    {
	return tableWidth;
    }

    int getTableHeight()
    {
	return tableHeight;
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
	//FIXME:May not be negative;
	this.marginLeft = marginLeft;
	this.marginTop = marginTop;
	this.marginRight = marginRight;
	this.marginBottom = marginBottom;
    }

    void setSizeAndShow(int width,int height)
    {
    	canvasWidth=width;
    	canvasHeight=height;
    	root.resize(width,height);
    	primary.sizeToScene();
    	primary.show();
    }

    void setUndecoratedSizeAndShow(double width,double height)
    {
    	canvasWidth=width;
    	canvasHeight=height;
    	root.resize(width,height);
    	//canvas.resize(width, height);
    	primary.initStyle(StageStyle.UNDECORATED); // WARN: can't change style after first window show
    	primary.setWidth(width);
    	primary.setHeight(height);
    	primary.setResizable(false);
    	primary.show();
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

    void putString(int x, int y, String text, boolean withFont2)
    {
	NullCheck.notNull(text, "text");
	synchronized(tableSync)
	{
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
    }

    void clearRect(int left,int top,int right,int bottom)
    {
	if (table == null || tableWidth <= 0 || tableHeight <= 0) return;
	final int l = left >= 0?left:0;
	final int t = top >= 0?top:0;
	final int r = right < tableWidth?right:(tableWidth - 1);
	final int b = bottom < tableHeight?bottom:(tableHeight - 1);
    	synchronized(tableSync)
    	{
	    if (l > r || t > b) return;
	    for(int i = l;i <= r;i++)
	    	for(int j = t;j <= b;j++)
		{
		    table[i][j] = ' ';
		    tableFont2[i][j]=false;
    		}
    	}
    	synchronized(vertSync)
    	{
	    if (vertLines != null)
		for(int i = l;i <= r;i++)
		    vertLines[i].uncover(t, b);
    	}
    	synchronized(horizSync)
    	{
	    if (horizLines != null)
		for(int i = t;i <= b;i++)
		    horizLines[i].uncover(l, r);
    	}
    }

    void paint()
    {
	final double fontWidth=bounds.getWidth();
	final double fontHeight=bounds.getHeight();
	if(table==null||!doPaint)
	    return;
	synchronized(tableSync)
	{
	    gc.setTextBaseline(VPos.TOP);
	    // canvas is transparent, so background color was filled rect before
	    gc.setFill(bkgColor);
	    gc.fillRect(0,0,primary.getWidth()-1,primary.getHeight()-1);
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
	}
	synchronized(vertSync)
	{
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
	}
	synchronized(horizSync)
	{
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
	}
	//synchronized(hotPointX)
	{
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
    }

    void drawVerticalLine(int top,int bottom,int x)
    {
	synchronized(vertSync)
	{
	    if (vertLines == null) return;
	    if (x >= vertLines.length)
	    {
		Log.warning("javafx", "unable to draw vertical line at column " + x + ", max vertical line is allowed at " + (vertLines.length - 1));
		return;
	    }
	    if (vertLines[x] != null)
		vertLines[x].cover(top, bottom);
	}
    }

    void drawHorizontalLine(int left,int right,int y)
    {
	synchronized(horizSync)
	{
	    if (horizLines == null)
		return;
	    if (y >= horizLines.length)
	    {
		Log.warning("javafx", "unable to draw horizontal line at row " + y + ", max horizontal line is allowed at " + (horizLines.length - 1));
		return;
	    }
	    if (horizLines[y] != null)
		horizLines[y].cover(left, right);
	}
    }

    //Making a resizable canvas
    static private final class ResizableCanvas extends Canvas
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
    }
}
