package org.luwrain.interaction.javafx;

import org.luwrain.core.Log;
import org.luwrain.interaction.OnScreenLine;
import org.luwrain.interaction.OnScreenLineTracker;

import javafx.application.Application;
import javafx.beans.property.Property;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.stage.*;
import javafx.scene.canvas.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextBuilder;

public class MainJavafxApp extends Application
{
	final Boolean awaiting=new Boolean(false);
	static public MainJavafxApp that=null;
	
	private static final int MIN_TABLE_WIDTH = 16;
    private static final int MIN_TABLE_HEIGHT = 8;

    public StackPane root;
    ResizableCanvas canvas;
	GraphicsContext gc;

	Font font;
	Bounds bounds;
	
    private Color fontColor = Color.WHITE;
    private Color bkgColor = Color.BLACK;
    private Color splitterColor = Color.GRAY;

    private int hotPointX = -1, hotPointY = -1;
    private int marginLeft = 0, marginTop = 0, marginRight = 0, marginBottom = 0;
    private int tableWidth = 0;
    private int tableHeight = 0;
    private char[][] table;
    private OnScreenLineTracker[] vertLines;
    private OnScreenLineTracker[] horizLines;
	
    public boolean doPaint=true;

    public Stage primary;
    

	class ResizableCanvas extends Canvas
	{
		public ResizableCanvas()
		{
			super(1024,768);
			// Redraw canvas when size changes.
			//widthProperty().addListener(evt -> draw());
			//heightProperty().addListener(evt -> draw());
		}

		/*
		private void draw() {
			double width = getWidth();
			double height = getHeight();

			GraphicsContext gc = getGraphicsContext2D();
			gc.clearRect(0, 0, width, height);

			gc.setStroke(Color.RED);
			gc.strokeLine(0, 0, width, height);
			gc.strokeLine(0, height, width, 0);
		}
		*/

		@Override
		public boolean isResizable() {
			return true;
		}

		@Override
		public double prefWidth(double height) {
			return getWidth();
		}

		@Override
		public double prefHeight(double width) {
			return getHeight();
		}
	}
    
    
	public MainJavafxApp()
	{
		that=this;
	}
    @Override public void start(final Stage primary) throws Exception
	{
		synchronized(awaiting){awaiting.notifyAll();}
		
		this.primary=primary;
		primary.setResizable(true);

		root=new StackPane();
        canvas = new ResizableCanvas();

        root.getChildren().add(canvas);
        primary.setScene(new Scene(root));
        
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        //AnchorPane.setTopAnchor(canvas,0.0);
        //AnchorPane.setLeftAnchor(canvas,0.0);
        //AnchorPane.setRightAnchor(canvas,0.0);
        //AnchorPane.setBottomAnchor(canvas,0.0);
        
        gc = canvas.getGraphicsContext2D();
        
        root.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        //primary.show();        

	}
    @SuppressWarnings("deprecation") public void setInteractionFont(Font font)
	{
		this.font=font;
        bounds = TextBuilder.create().text("A").font(font).build().getLayoutBounds();
	}

	public boolean initTable()
    {
		double width = canvas.getWidth();
		double height = canvas.getHeight();//primary.getHeight();
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
        System.out.println("bound:"+width+","+height);
		int width_=(int)Math.floor(width/bounds.getWidth());
		int height_=(int)Math.floor(height/bounds.getHeight());
		if (width_ < MIN_TABLE_WIDTH || height_ < MIN_TABLE_HEIGHT)
		{
		    Log.error("javafx", "too small table for initialization:" + width_ + "x" + height_);
		    return false;
		}
		tableWidth = width_;
		tableHeight = height_;
		table = new char[tableWidth][];
		for(int i = 0;i < tableWidth;i++)
		    table[i] = new char[tableHeight];
		for(int i = 0;i < tableWidth;i++)
		    for(int j = 0;j < tableHeight;j++)
			table[i][j] = ' ';
		vertLines = new OnScreenLineTracker[tableWidth];
		for(int i = 0;i < tableWidth;i++)
		    vertLines[i] = new OnScreenLineTracker();
		horizLines = new OnScreenLineTracker[tableHeight];
		for(int i = 0;i < tableHeight;i++)
		    horizLines[i] = new OnScreenLineTracker();
		    Log.info("javafx", "table is initialized with size " + width + "x" + height);
		return true;
    }
	public int getTableWidth()
	{
		return tableWidth;
	}

	public int getTableHeight()
	{
		return tableHeight;
	}

    public void setColors(Color fontColor,Color bkgColor,Color splitterColor)
    {
		if (fontColor == null || bkgColor == null || splitterColor == null) return;
		this.fontColor = fontColor;
		this.bkgColor = bkgColor;
		this.splitterColor = splitterColor;
    }
    public void setMargin(int marginLeft,int marginTop,int marginRight,int marginBottom)
    {
		//FIXME:May not be negative;
		this.marginLeft = marginLeft;
		this.marginTop = marginTop;
		this.marginRight = marginRight;
		this.marginBottom = marginBottom;
    }
    public void setSize(int width,int height)
    {
    	primary.setWidth(width);
    	primary.setHeight(height);
    	//primary.sizeToScene();
    	//double sx=canvas.getWidth(),sy=canvas.getHeight();
    }
    public void setHotPoint(int x, int y)
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
    public void putString(int x, int y, String text)
    {
		if (table == null || x >= tableWidth || y >= tableHeight ||
		    x >= table.length || y >= table[x].length)
		    return;
		if (text == null)
		    return;
		final int bound = x + text.length() <= tableWidth?text.length():tableWidth - x;  
		for(int i = 0;i < bound;i++)
		    table[x + i][y] = text.charAt(i) != '\0'?text.charAt(i):' ';
    }
    public void clearRect(int left,int top,int right,int bottom)
    {
		if (table == null || tableWidth <= 0 || tableHeight <= 0) return;
		final int l = left >= 0?left:0;
		final int t = top >= 0?top:0;
		final int r = right < tableWidth?right:(tableWidth - 1);
		final int b = bottom < tableHeight?bottom:(tableHeight - 1);
		if (l > r || t > b) return;
		for(int i = l;i <= r;i++)
		    for(int j = t;j <= b;j++)
		    	table[i][j] = ' ';
		if (vertLines != null)
		    for(int i = l;i <= r;i++)
		    	vertLines[i].uncover(t, b);
		if (horizLines != null)
		    for(int i = t;i <= b;i++)
		    	horizLines[i].uncover(l, r);
  }


	public void paint()
    {
		final double fontWidth=bounds.getWidth();
		final double fontHeight=bounds.getHeight();

		if(table==null||!doPaint) return;
		// canvas is transparent, so background color was filled rect before
		gc.setFill(bkgColor);
		gc.fillRect(0,0,primary.getWidth()-1,primary.getHeight()-1);
		gc.setFont(font);
		gc.setFill(fontColor);
		char[] chars=new char[tableWidth];
		final double baseLine = fontHeight;
		for(int i=0;i<tableHeight;i++)
		{
			for(int j=0;j<tableWidth;j++)
				chars[j]=table[j][i];
			gc.fillText(new String(chars),marginLeft,(i*fontHeight)+baseLine+marginTop);
		}

		gc.setFill(splitterColor);
		// Vertical lines;
		if(vertLines!=null) for(int i=0;i<vertLines.length;i++)
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
		if(horizLines!=null) for(int i=0;i<horizLines.length;i++)
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
			gc.fillRect((hotPointX*fontWidth)+marginLeft,(hotPointY*fontHeight)+marginTop,fontWidth,fontHeight);
			gc.setFill(bkgColor);
			String str=new String();
			str+=table[hotPointX][hotPointY];
			gc.fillText(str,(hotPointX*fontWidth)+marginLeft,(hotPointY*fontHeight)+baseLine+marginTop);
		}

    }
    public void drawVerticalLine(int top,int bottom,int x)
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
		
	public void drawHorizontalLine(int left,int right,int y)
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
