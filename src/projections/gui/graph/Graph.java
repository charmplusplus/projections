package projections.gui.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

import projections.gui.Bubble;
import projections.gui.MainWindow;
 
public class Graph extends JPanel 
    implements MouseInputListener
{

	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
	private int myRun = 0;

//    public static final int STACKED   = 0;  // type of the bar graph
//    public static final int UNSTACKED = 1;  // single, multiple or stacked
    protected static final int AREA      = 2;  // Area graph (stacked)
//    public static final int SINGLE    = 3;  // take the average of all y-values
    protected static final int BAR       = 4;  // Graph type, bar graph
    protected static final int LINE      = 5;  // or line graph

    private static final int X_AXIS = 0;
    private static final int Y_AXIS = 1;
    
    private int GraphType;
    private boolean GraphStacked;
    private DataSource dataSource;
    private XAxis xAxis;
    private YAxis yAxis;
    private double xscale, yscale;
    
      
    private Font fontAxisTitles;
    private Font fontChartTitle;
    private Font fontLabels;
    private FontMetrics fmAxisTitles;
    private FontMetrics fmChartTitle;
    private FontMetrics fmLabels;

    // Computed width of largest y axis label. The largest value is not necessarily the longest string
    private int maxYLabelWidth = 0;

    // number of pixels per bar
    private double pixelincrementX(){
    	return availableWidth() / maxvalueX();    	
    }


    private double pixelincrementY(){
    	return (originY()-topMargin()) / maxvalueY();
    }
    
    private double maxvalueX(){
    	return dataSource.getIndexCount();
    }
    
    private double maxvalueY(){
    	// get max y Value
    	if (((GraphType == BAR) && GraphStacked) || 
    			((GraphType == LINE) && GraphStacked) || 
    			(GraphType == AREA)) {
    		return maxSumY;
    	} else {
    		return yAxis.getMax();                               
    	}    
    }
    
    
    // number of pixels per tick
    private double tickIncrementX;

    // "best" values to be derived from pixelincrements
    private long valuesPerTickX, valuesPerLabelX;
    private long valuesPerTickY, valuesPerLabelY;

    private double barWidth;

    // for stacked data
    private double[][] stackArray;
    private double maxSumY;
  
    
    private final int spaceBetweenYValuesAndAxis = 10;

    private Bubble bubble;
    private boolean showBubble = true;
    private int bubbleXVal;
    private int bubbleYVal;
    
    // Coefficients that describe a polynomial to plot on top of the chart data
    // y = polynomialToOverlay[2]*x^2 +  polynomialToOverlay[1]*x^1 +  polynomialToOverlay[0]*x^0
    // The y values are of the same units as the y values in the data source
    // The x values are measured in terms of the bin/interval indices
    private double[] polynomialToOverlay;

    // Markers to identify times where iterations or phases start/end
    private TreeMap<Double, String> phaseMarkers = new TreeMap<Double, String>();
    
    private boolean showMarkers = true;

    
    /** Special constructor. This can only be called from a projections tool!!! */
    public Graph()
    {
	setPreferredSize(new Dimension(400,300));	
	initFonts();

	GraphType = BAR;	   // default GraphType is BAR
	GraphStacked = true;    // default GraphType is STACKED
	stackArray = null;
	dataSource = null;
	
	xscale = 1.0;
	yscale = 1.0;
	
	addMouseMotionListener(this);
	addMouseListener(this);
    }
    
    /** Special constructor. This can only be called from a projections tool!!! */

    public Graph(DataSource d, XAxis x, YAxis  y)
    {
	// call default constructor
	this();
	initFonts();

	xAxis = x;
	yAxis = y;
	dataSource = d;
	createStackArray();
    }

    
    
    private void initFonts(){
    	// The font used for the Chart title 
    	fontChartTitle = new Font("SansSerif",Font.BOLD, 24);
    	
    	// The font used for the x and y axis titles:
    	fontAxisTitles = new Font("SansSerif",Font.BOLD, 16);

    	// The font used for the numbers on the x and y axes:
    	fontLabels = new Font("SansSerif",Font.PLAIN,12);
    }
    
    
    public Dimension getMinimumSize() {
	return new Dimension(400,300);
    }

    // ***** API Interface to the control panel *****

    public void setGraphType(int type)
    {
    	if (type == LINE || type == BAR || type == AREA) {
    		GraphType = type;      // set the Graphtype to LINE or BAR 
    	} else {
    		//unknown graph type.. do nothing ; draw a bargraph
    	}
    	repaint();
    }

    public int getGraphType()
    {
	return GraphType;
    }

    public void setStackGraph(boolean isSet)
    {
	GraphStacked = isSet;
	repaint();
    }

    public void setMarkers(TreeMap<Double, String> phaseMarkers){
    	this.phaseMarkers = phaseMarkers;
    	System.out.println("Graph: adding " + phaseMarkers.size() + " markers");
    }

    public void clearMarkers(){
    	phaseMarkers.clear();
    }
    
    public void setData(DataSource d, XAxis x, YAxis  y)
    {
    	xAxis = x;
    	yAxis = y;
    	dataSource = d;
    	createStackArray();
    	repaint();
    }

    public void setData(DataSource d)
    {
    	dataSource = d;
    	createStackArray();
    	repaint();
    }

    public void setScaleX(double val) {
    	xscale = val;
    	setPreferredSize(new Dimension((int)(baseWidth()*xscale),
    			(int)(baseHeight()*yscale)));
    	revalidate();
    	repaint();
    }

    public void setScaleY(double val) {
    	yscale = val;
    	setPreferredSize(new Dimension((int)(baseWidth()*xscale),
    			(int)(baseHeight()*yscale)));
    	revalidate();
    	repaint();
    }

    private void createStackArray() {
    	if (dataSource != null) {
    		double tempMax = 0;
    		int numY = dataSource.getValueCount();
    		stackArray = new double[dataSource.getIndexCount()][];
    		for (int k=0; k<dataSource.getIndexCount(); k++) {
    			stackArray[k] = new double[numY];

    			dataSource.getValues(k, stackArray[k]);
    			for (int j=1; j<numY; j++) {
    				stackArray[k][j] += stackArray[k][j-1];
    			}
    			if (    stackArray!=null && 
    					stackArray.length > k && 
    					stackArray[k]!=null && 
    					(numY-1) >= 0 &&
    					stackArray[k].length > (numY-1) && 
    					tempMax < stackArray[k][numY-1]     ) {
    				
    				tempMax = stackArray[k][numY-1];
    				
    			}
    		}
    		maxSumY = tempMax;
    	} else {
    		stackArray = null;
    	}
    }

    /**  
     *  getXValue
     *	returns the x value of the bar graph the mouse is currently over
     * 	if the mouse is not over any bar data, then return -1.
     *  It is not supposed to work with line data of any sort. It will
     *  work for bar graphs and area graphs, though the effects are less
     *  than ideal for the latter.
     */
    private int getXValue(int xPos) {
    	if( (xPos > originX()) && (xPos < availableWidth()+originX())) {
    		// get the expected value
    		int displacement = xPos - originX();
    		int whichBar = (int)(displacement/pixelincrementX());
    		double x1 = originX() + (whichBar*pixelincrementX()) + (pixelincrementX()/2.0);
    		if ((GraphType == BAR) || (GraphType == AREA)) {
            	long lowerPixelForBar = Math.round(x1-(barWidth/2.0));
            	long upperPixelForBar = Math.round(x1+(barWidth/2.0));
            	// The math here isn't quite right, so lets just account for that here.
            	if ( xPos < lowerPixelForBar){
            		return whichBar-1;
            	} else if ( xPos > upperPixelForBar) {
    				return whichBar+1;
    			} else{
    				return whichBar;
    			}
    		} 
    	}	
    	return -1;
    }
	
    /**
     *  getYValue returns the y value INDEX of the graph the mouse is
     *  currently over if the graph is stacked. Behaviour is currently
     *  undefined for unstacked graphs.
     *
     *  In the case of the Y Axis, the number of Y indices is expected to
     *  be relatively small, even though the range of Y can be very large.
     *  Hence, the array looping code is adequate for now.
     */
    private int getYValue(int xVal, int yPos) {
    	if ( (xVal >= 0)  &&
    			(yPos < originY()) && (yPos > topMargin()) && 
    			(stackArray != null) && 
    			(xVal < stackArray.length) ) {
    		int numY = dataSource.getValueCount();
    		int y;
    		for (int k=0; k<numY; k++) {
    			y = originY() - (int)(stackArray[xVal][k]*pixelincrementY());
    			if (yPos > y) {
    				return k;
    			}
    		}
    	}	
    	return -1;
    }

    // ***** Painting Routines *****
    public void paintComponent(Graphics g) {
	    super.paintComponent(g);    
    	fmLabels = g.getFontMetrics(fontLabels);
    	fmAxisTitles = g.getFontMetrics(fontAxisTitles);
    	fmChartTitle = g.getFontMetrics(fontChartTitle);
    	
    	drawDisplay((Graphics2D) g);
    }

   
    public void mouseEntered(MouseEvent e) {
    } 

    public void mouseExited(MouseEvent e) {
    } 

    public void mousePressed(MouseEvent e) {
    } 

    public void mouseReleased(MouseEvent e) {
    }


    public void mouseClicked(MouseEvent e) {
    	int x = e.getX();
    	int y = e.getY();

    	int xVal = getXValue(x);
    	int yVal = getYValue(xVal, y);

    	// if either x or y is available, support the click
    	// but the client tool is going to have to deal with
    	// it
    	if ((xVal > -1) || (yVal > -1)) {
        	dataSource.toolClickResponse(e, xVal, yVal);
    	}
    }

    public void mouseMoved(MouseEvent e) {
    	int x = e.getX();
    	int y = e.getY();
    	int xVal = getXValue(x);
    	int yVal = getYValue(xVal, y);

    	if((xVal > -1) && (yVal > -1)) {
    		showPopup(xVal, yVal, x, y);
    	} else {
    		disposeOfBubble();
    	}

    	if ((xVal > -1) || (yVal > -1)) {
    		dataSource.toolMouseMovedResponse(e, xVal, yVal);
    	}

    }

    public void mouseDragged(MouseEvent e) {
    }
    

    /**
     *  This is for the benefit of the bubble text placement. 
     *  We need to figure out the location of the graph's origin with
     *  respect to the screen (from which the bubble Window will attempt
     *  to set its location).
     *
     *  The algorithm will go through each parent of the graph and cumulate
     *  the offsets, finally tacking on the offset of the frame to the screen.
     */
    private Point getBubbleOffset() {
	Component c = this;
	int xOffset = c.getLocation().x;
	int yOffset = c.getLocation().y;

	while ((c = c.getParent()) != null) {
	    xOffset += c.getLocation().x;
	    yOffset += c.getLocation().y;
	}

	return new Point(xOffset,yOffset);
    }

    private void showPopup(int xVal, int yVal, int xPos, int yPos){

    	Point offset = getBubbleOffset();
    	String text[] = dataSource.getPopup(xVal, yVal);
    	if (text == null) {
    		return;
    	}
    	    	
    	// dispose of any old popup because mouse has moved away 
    	if (bubbleXVal != xVal || bubbleYVal != yVal){
    		disposeOfBubble();
    	}

    	if (bubble == null && showBubble) {
    		if (GraphStacked) {
    			bubble = new Bubble(this, text);
    			bubble.setLocation(xPos+offset.x, yPos+offset.y);
    			bubble.setVisible(true);
    			bubbleXVal = xVal;
    			bubbleYVal = yVal;
    		} else {
    			// do nothing
    		}
    	}
    }
    
    /** Dispose of displayed bubble if one exists */
    private void disposeOfBubble(){
    	if(bubble != null){
    		bubble.setVisible(false);
    		bubble.dispose();
    		bubble = null;
    	}
    }


    private void drawDisplay(Graphics2D g)
    {

    	Color background = MainWindow.runObject[myRun].background;
    	Color foreground = MainWindow.runObject[myRun].foreground;

    	g.setBackground(background);
    	g.setColor(foreground);

    	g.clearRect(0, 0, getWidth(), getHeight());

    	// if there's nothing to draw, don't draw anything!!!
    	if (dataSource == null) {
    		return;
    	}

    	
    	if ((xAxis != null) && (yAxis != null)) {
    		
    		// Compute various layout dimensions. MUST DO Y AXIS FIRST!!!! 
    		// Width of available chart area depends on y axis label width stored in maxLabelWidth
    		setBestIncrements(Y_AXIS, pixelincrementY(), (long)maxvalueY());
    		setBestIncrements(X_AXIS, pixelincrementX(), (int)maxvalueX());
        	
    		if (GraphType == BAR) {
    			drawBarGraph(g);
    		} else if (GraphType == AREA) {
    			drawAreaGraph(g);
    		} else {
    			drawLineGraph(g);
    		}
    		
    		drawXAxis(g);
    		drawYAxis(g);
    		
    		drawMarkers(g);
    	}

    	

    	// display Graph title
    	String graphTitle = dataSource.getTitle();
    	g.setFont(fontChartTitle);
    	g.drawString(graphTitle,
    			(getWidth()-fmChartTitle.stringWidth(graphTitle))/2, 
    			chartTitleBaseline() );

    	// display xAxis title 
    	// centered along x axis line
    	int centerX = (originX() + availableWidth()+originX() )/ 2;    	
    	String xTitle = xAxis.getTitle();
    	g.setFont(fontAxisTitles);
    	g.drawString(xTitle,
    			centerX - fmAxisTitles.stringWidth(xTitle)/2, 
    			xAxisTitleBaseline() );


    	// display yAxis title
    	String yTitle = yAxis.getTitle();
    	g.setFont(fontAxisTitles);
    	g.rotate(-Math.PI/2);
    	g.drawString(yTitle, 
    			-(getHeight()+fmAxisTitles.stringWidth(yTitle))/2, 
    			fmAxisTitles.getHeight() );
    	g.rotate(Math.PI/2);


    }

    
    public void showMarkers(boolean b){
    	showMarkers = b;
    	repaint();
    }

    private void drawMarkers(Graphics2D g) {
    	if(showMarkers){
    		final int extendPastGraph = 8;
    		Iterator<Double> iter = phaseMarkers.keySet().iterator();
    		while(iter.hasNext()){
    			int xval = originX() + (int)(iter.next()*pixelincrementX());

    			g.setColor(MainWindow.runObject[myRun].background);
    			g.setStroke(new BasicStroke(4f));
    			g.drawLine(xval, originY()+extendPastGraph, xval , topMargin()-extendPastGraph);
    			g.setColor(MainWindow.runObject[myRun].foreground);
    			g.setStroke(new BasicStroke(2f));
    			g.drawLine(xval, originY()+extendPastGraph, xval , topMargin()-extendPastGraph);

    		}
    	}    	
    }


	private void drawXAxis(Graphics2D g) {
    	g.setColor(MainWindow.runObject[myRun].foreground);

    	// draw xAxis
    	g.drawLine(originX(), originY(), availableWidth()+originX(), originY());

    	int mini = 0;
    	int maxi = (int)maxvalueX();

    	g.setFont(fontLabels);

    	// drawing xAxis divisions
    	// based on the prior algorithm, it is guaranteed that the number of
    	// iterations is finite since the number of pixels per tick will
    	// never be allowed to be too small.
    	for (int i=mini;i<maxi; i+=valuesPerTickX) {
    		int curx = originX() + (int)(i*pixelincrementX());
    		// don't attempt to adjust the tick positions midway unless we're
    		// on a one-to-one mapping.
    		if (valuesPerTickX == 1) {
    			curx += (int)(tickIncrementX / 2);
    		}
    		// labels have higher lines.
    		if (i % valuesPerLabelX == 0) {
    			g.drawLine(curx, originY()+5, curx, originY());
    			String s = xAxis.getIndexName(i);
    			g.drawString(s, curx-fmLabels.stringWidth(s)/2, xAxisValuesBaseline() );
    		} else {
    			g.drawLine(curx, originY()+2, curx, originY());
    		}
    	}
    }

    private void drawYAxis(Graphics2D g) {
    	g.setColor(MainWindow.runObject[myRun].foreground);

    	// draw yAxis
    	g.drawLine(originX()-1, originY(), originX()-1 , topMargin());

    	g.setFont(fontLabels);
        	
    	FontMetrics fm = g.getFontMetrics(fontLabels);
    	int sw = fm.getHeight();

    	// drawing yAxis divisions
    	for (long i=0; i<=maxvalueY(); i+=valuesPerTickY) {
    		int cury = originY() - (int)(i*pixelincrementY());
    		if (i % valuesPerLabelY == 0) {
    			g.drawLine(originX()-1, cury, originX()-5-1,cury);
    			String yLabel = yAxis.getValueName(i);
    	    	g.drawString(yLabel, originX()-1-fm.stringWidth(yLabel)-spaceBetweenYValuesAndAxis, 
    					cury + sw/2);
    		} else {
    			g.drawLine(originX()-1, cury, originX()-2-1, cury);
    		}
    	}
    }

    private void drawOverlayedPolynomial(Graphics2D g) {
    	// Plot the polynomial
    	if(polynomialToOverlay != null && polynomialToOverlay.length>0){

    		DecimalFormat format = new DecimalFormat();
    		format.setMinimumFractionDigits(0);
    		format.setMaximumFractionDigits(2);
    		
			g.setColor(Color.orange);
    		
    		String info = " ";
    		
    		if(polynomialToOverlay[polynomialToOverlay.length-1]<0.0)
    			info += "- ";

			for(int d=polynomialToOverlay.length-1; d>=0; d--){
				
				double val = polynomialToOverlay[d];
				if(val >= 0.0){
					info += format.format(val);
				} else {
					info += format.format(-val);
				}
				
				if(d==0)
					info += "";
				else if(d==1)
					info += " x";
				else
					info += " x^" + format.format(d);
					
				
				if(d>0){
					if(polynomialToOverlay[d-1]>=0.0)
						info += " + ";
					else
						info += " - ";
				}
			}
			
	    	FontMetrics fm = g.getFontMetrics(fontAxisTitles);
	    	g.setFont(fontAxisTitles);
	    	g.drawString(info, getWidth()-fm.stringWidth(info) - 50 ,  20 );

			g.setStroke(new BasicStroke(4f));
			g.setColor(Color.orange);
			Object oldHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
    		for(int x=0; x< getWidth(); x++){

    			//    bar i center pixel coordinate:	
    			//       	originX() + (int)(i*pixelincrementX)
    			//		
    			//    pixel x equates to bar:
    			//          (x-originX())/pixelincrementX

    			int x1_px = x;
    			double x1 = ((double)(x1_px-originX()))/pixelincrementX();

    			int x2_px = x+1;
    			double x2 = ((double)(x2_px-originX()))/pixelincrementX();

    			double y1 = 0.0;
    			double y2 = 0.0;
    			for(int d=0; d<polynomialToOverlay.length; d++){
    				y1 += polynomialToOverlay[d]*Math.pow(x1,d);
    				y2 += polynomialToOverlay[d]*Math.pow(x2,d);
    			}

    			int y1_px = originY() - (int)(y1*pixelincrementY());
    			int y2_px = originY() - (int)(y2*pixelincrementY());

    			if(  y1_px < (originY()+2) && 
    				 y2_px < (originY()+2) && 
    				 y1_px > (topMargin()-2) && 
    				 y2_px > (topMargin()-2) &&
    				 x1_px > originX()-2 &&
    				 x2_px > originX()-2 &&
    				 x1_px < (availableWidth()+originX()+2) &&
    				 x2_px < (availableWidth()+originX()+2)) {
    				g.drawLine(x1_px, y1_px, x2_px, y2_px);
    			}

    		}
    		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHint);
    	}


    }

    
    
    private void drawBarGraph(Graphics2D g) {
    	int numX = dataSource.getIndexCount();
    	int numY = dataSource.getValueCount();
    	double [] data = new double[numY];

    	// Determine barWidth from tickIncrementX. If each value can be
    	// represented by each tick AND there is enough pixel resolution
    	// for each bar, we set the width of the visual bar to be 80% of
    	// the tick distance.
    	if ((valuesPerTickX == 1) && (tickIncrementX >= 3.0)) {
    		barWidth = 0.8*tickIncrementX;
    	} else {
    		barWidth = 1.0;
    	}

    	for (int i=0; i<numX; i++) {
    		dataSource.getValues(i, data);
    		if (GraphStacked) {
    			int y = 0;
    			for (int k=0; k<numY; k++) {
    				// calculating lowerbound of box, which StackArray
    				// allready contains
    				y = originY() - (int)(stackArray[i][k]*pixelincrementY());
    				
    				g.setPaint(dataSource.getColor(k));
    				
    				// using data[i] to get the height of this bar
    				if (valuesPerTickX == 1) {
    					g.fillRect(originX() + (int)(i*pixelincrementX() +
    							tickIncrementX/2 -
    							barWidth/2), y,
    							(int)barWidth,
    							(int)(data[k]*pixelincrementY()));

    				} else {
    					g.fillRect(originX() + (int)(i*pixelincrementX()), y, 
    							(int)((i+1)*pixelincrementX()) - 
    							(int)(i*pixelincrementX()), 
    							(int)(data[k]*pixelincrementY()));
    				}
    			}
    		} else {
    			// unstacked.. sort the values and then display them
    			int maxIndex=0;
    			int y = 0;
    			double maxValue=0;
    			// one col to retain color (actual index) information
    			// and the other to hold the actual data
    			double [][] temp = new double[numY][2];
    			for (int k=0;k<numY;k++) {	
    				temp[k][0] = k;
    				temp[k][1] = data[k];
    			}
    			for (int k=0; k<numY; k++) {
    				maxValue = temp[k][1];
    				maxIndex = k;
    				for (int j=k;j<numY;j++) {
    					if (temp[j][1]>maxValue) {
    						maxIndex = j;
    						maxValue = temp[j][1];
    					}
    				}
    				int t = (int)temp[k][0];
    				double t2 = temp[k][1];

    				temp[k][0] = temp[maxIndex][0];
    				//swap the contents of maxValue with the ith value
    				temp[k][1] = maxValue;
    				temp[maxIndex][0] = t;
    				temp[maxIndex][1] = t2;
    			}
    			// now display the graph
    			for(int k=0; k<numY; k++) {
    				g.setPaint(dataSource.getColor((int)temp[k][0]));
    				y = (originY()-(int)(temp[k][1]*pixelincrementY()));
    				if (valuesPerTickX == 1) {
    					g.fillRect(originX() + (int)(i*pixelincrementX() +
    							tickIncrementX/2 -
    							barWidth/2), y,
    							(int)barWidth,
    							(int)(temp[k][1]*pixelincrementY()));
    				} else {				   
    					g.fillRect(originX() + (int)(i*pixelincrementX()), y,
    							(int)((i+1)*pixelincrementX()) -
    							(int)(i*pixelincrementX()),
    							(int)(temp[k][1]*pixelincrementY()));
    				}
    			}
    		}
    		/*  ** UNUSED for now **
		    whether it is single should be orthogonal to stacking.
	    } else {
		// single.. display average value
		double sum=0;
		int y = 0;
		for (int j=0; j<numY; j++) {
		    sum += data[j];
		}
		sum /= numY;
		y = (int)(originY() - (int)(sum*pixelincrementY));
		g.fillRect(originX() + (int)(i*pixelincrementX),y,
			   (int)((i+1)*pixelincrementX) -
			   (int)(i*pixelincrementX),
			   (int)(sum*pixelincrementY));
	    }		
    		 */
    	}
    	
    	drawOverlayedPolynomial(g);
    }
	
    private void drawLineGraph(Graphics2D g) {
	int xValues = dataSource.getIndexCount();
	int yValues = dataSource.getValueCount();
	double [] data = new double[yValues];

	// x1,y1 store previous values
	int x1 = -1;
	int [] y1 = new int[yValues];
	// x2, y2 to store present values so that line graph can be drawn
	int x2 = 0;		
	int [] y2 = new int[yValues];  

	for (int i=0; i<yValues; i++) {
	    y1[i] = -1;
	}
	// do only till the window is reached 
	for (int i=0; i < xValues; i++) {	
	    if (GraphStacked) {
		data = stackArray[i];
	    } else {
		dataSource.getValues(i,data);
	    }
	    //calculate x value
	    x2 = originX() + (int)(i*pixelincrementX()) + 
		(int)(pixelincrementX()/2);    
	 
	    for (int j=0; j<yValues; j++) {
		g.setPaint(dataSource.getColor(j));
		y2[j] = (originY() - (int)(data[j]*pixelincrementY()));
		//is there any other condition that needs to be checked?
		if(x1 != -1)	
		    g.drawLine(x1,y1[j],x2,y2[j]);
		y1[j] = y2[j];		
	    }
	    x1 = x2;
	}

	drawOverlayedPolynomial(g);
    }

    private void drawAreaGraph(Graphics2D g) {
	int xValues = dataSource.getIndexCount();
	int yValues = dataSource.getValueCount(); // no. of y values for each x
	double data[] = new double[yValues];

	Polygon polygon = new Polygon();

	// do only till the window is reached 
	// layers have to be drawn in reverse (highest first).
	for (int layer=yValues-1; layer>=0; layer--) {	

	    // construct the polygon for the values.
	    polygon = new Polygon();
	    // polygon.reset(); // 1.4.1 Java only

	    int xPixel; 
	    int yPixel;

	    for (int idx=0; idx<xValues; idx++) {
		// get the y values given the x value
		// **CW** NOTE to self: This is silly. Why do I have to
		// get the data each time when I only need to get it once!?
		// Fix once I have the time.
		dataSource.getValues(idx,data);

		//calculate x & y pixel values
		xPixel = originX() + (int)(idx*pixelincrementX()) + 
		    (int)(pixelincrementX()/2);    
		// **CW** NOTE will need some refactoring to prevent
		// recomputation of the prefix sum each time we go through
		// the Y values.
		prefixSum(data);
	    	yPixel = (originY() - (int)(data[layer]*pixelincrementY()) );

		// if first point, add the baseline point before adding
		// the first point.
		if (idx == 0) {
		    // just so it does not overwrite the Axis line.
		    polygon.addPoint(xPixel, originY());
		}
		// add new point to polygon
		polygon.addPoint(xPixel, yPixel);
		// if last point, add the baseline point after adding
		// the last point.
		if (idx == xValues-1) {
		    // just so it does not overwrite the Axis line.
		    polygon.addPoint(xPixel, originY());
		}
	    }
	    // draw the filled polygon.
	    g.setPaint(dataSource.getColor(layer));
	    g.fill(polygon);
	    // draw a black outline.
	    g.setColor(Color.black);
	    g.draw(polygon);
	}

	drawOverlayedPolynomial(g);
    }

    // in-place computation of the prefix sum
    private void prefixSum(double data[]) {
	// computation is offset by 1
	for (int i=0; i<data.length-1; i++) {
	    data[i+1] = data[i+1]+data[i];
	}
    }

    /**
     *  Determines "best" values for tickIncrementsX and valuesPerTickX and
     *  valuesPerLabelX OR tickIncrementsY and valuesPerTickY and 
     *  valuesPerLabelY
     *
     *  Sets global (yucks!) variables to reflect the "best" number of pixels
     *  for Labels (5, 10, 50, 100, 500, 1000 etc ...) and the number of pixels
     *  for each tick (1, 10, 100, 1000 etc ...) based on the "best" label
     *  pixel number.
     */
    private void setBestIncrements(int axis, double pixelsPerValue, long maxValue) {
    	long index = 0;
    	long labelValue = getNextLabelValue(index);
    	long tickValue = getNextTickValue(index++);

    	int labelWidth = 0;
    	while (true) {
    		
    		if (axis == X_AXIS) {
    			// Find largest x axis label:
        		labelWidth = 0;
        		for (int i=0; i<maxValue; i++) {
        			String xLabel = xAxis.getIndexName(i);
        			int w = fmLabels.stringWidth(xLabel);
        			if (w > labelWidth){
        				labelWidth = w;
        			}
        		}
    		} else {
    			labelWidth = fmLabels.getHeight();
    		}
    		// is the number of pixels to display a label too small?
    		if (labelWidth > (pixelsPerValue*labelValue*0.8)) {
    			labelValue = getNextLabelValue(index);
    			tickValue = getNextTickValue(index++);
    			continue;
    		} else {
    			// will my component ticks be too small?
    			if ((pixelsPerValue*tickValue) < 2.0) {
    				labelValue = getNextLabelValue(index);
    				tickValue = getNextTickValue(index++);
    				continue;
    			} else {
    				// everything is A OK. Set the global variables.
    				if (axis == X_AXIS) {
    					tickIncrementX = tickValue*pixelsPerValue;
    					valuesPerTickX = tickValue;
    					valuesPerLabelX = labelValue;
    				} else if (axis == Y_AXIS) {
    					//			tickIncrementY = tickValue*pixelsPerValue;
    					valuesPerTickY = tickValue;
    					valuesPerLabelY = labelValue;

    					// Determine the width of the y axis labels so we can later figure out the x origin
    					maxYLabelWidth = 0;
    					for (long i=0; i<=maxvalueY(); i+=tickValue) {
    						if (i % labelValue == 0) {
    							String yLabel = yAxis.getValueName(i);
    							int w = fmLabels.stringWidth(yLabel);
    							if (w > maxYLabelWidth){
    								maxYLabelWidth = w;
    							}
    						}
    					}

    				}
    				return;
    			}
    		}
    	}
    }

    /**
     *  Returns the next value in the series: 1, 5, 10, 50, 100, etc ...
     */
    private long getNextLabelValue(long prevIndex) {
	if (prevIndex == 0) {
	    return 1;
	}
	if (prevIndex%2 == 0) {
	    return (long)java.lang.Math.pow(10,prevIndex/2);
	} else {
	    return (long)(java.lang.Math.pow(10,(prevIndex+1)/2))/2;
	}
    }

    /**
     *  Given a label value, what is the appropriate tick value.
     *  Returns a value from 1, 10, 100, 1000 etc ...
     */
    private long getNextTickValue(long prevIndex) {
	if (prevIndex == 0) {
	    return 1; // special case
	}
	return (long)java.lang.Math.pow(10,(prevIndex-1)/2);
    }

    public static void main(String [] args){
	JFrame f = new JFrame();
	JPanel mainPanel = new JPanel();
	double data[][]={{20,21,49,3},{25,34,8,10},{23,20,54,3},{20,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4}};

	f.addWindowListener(new WindowAdapter()
          {
                 public void windowClosing(WindowEvent e)
                 {
                        System.exit(0);
                 }
          }); 
        
	DataSource ds=new DataSource2D("Histogram",data);
        XAxis xa=new XAxisFixed("Entry Point Execution Time","ms");
        YAxis ya=new YAxisAuto("Count","",ds);
 	Graph g=new Graph();
	/* **TESTCASE - UNSTACKED BAR** 
	g.setGraphType(Graph.BAR);
	g.setStackGraph(false);
	*/
	/* **TESTCASE - STACKED BAR**
	g.setGraphType(Graph.BAR);
	g.setStackGraph(true);
	*/
	/* **TESTCASE - AREA (STACKED)** */
	g.setGraphType(Graph.AREA);
	/* **TESTCASE - LINE (UNSTACKED)** 
	g.setGraphType(Graph.LINE);
	*/

        g.setData(ds,xa,ya);
        mainPanel.add(g);
	f.getContentPane().add(mainPanel);
	f.pack();
	f.setSize(500,400);
        f.setTitle("Projections");
        f.setVisible(true);
    }
    
    /** Add a polynomial that ought to be plotted on top of the other chart data.
     *  This probably only makes sense for a 2-D XY plot. 
     *   
     *   For example, if you want a quadratic function provide an array of length 3:
     *   y = polynomialToOverlay[2]*x^2 +  polynomialToOverlay[1]*x^1 +  polynomialToOverlay[0]*x^0
     *   
     *   The y values are treated the same as the y values in the data source
     *   The x values are measured in terms of the bin/interval indices
     */
    public void addPolynomial(double[] coefficients){
    	polynomialToOverlay = coefficients.clone();
    	repaint();
    }
    
    public void clearPolynomial(){
    	polynomialToOverlay = null;
    	repaint();
    }
    
    /** enable/disable displaying of bubbles */
    public void showBubble(boolean displayBubbles){
    	showBubble = displayBubbles;
    	if(! showBubble){
    		disposeOfBubble();
    	}
    }
    
    
	/** Return the width of the parent window. xscale will control just how much more of the graph one can see. */
    private int baseWidth(){
		return getParent().getWidth();
	}

	private int baseHeight(){
		return getParent().getHeight();
	}

	/** The x pixel coordinate of the bottom left intersection of the axis lines */
	private int originX(){
		return maxYLabelWidth + spaceBetweenYValuesAndAxis + fmLabels.getHeight()*2;
	}

	/** The y pixel coordinate of the bottom left intersection of the axis lines */
	private int	originY(){
		return xAxisTitleBaseline() - fmLabels.getHeight() - spaceBetweenXValuesAndLabel() - fmLabels.getHeight() - spaceBetweenAxisAndXValues();
	}
	
	private int availableWidth(){
		// width available for drawing the graph
		int rightMargin = 40;
		return (int)((baseWidth()-rightMargin-originX())*xscale);
	}

	/** Return the number of pixels at the top above the top of the y axis. The chart title is drawn in this region. */
	private int topMargin(){
		return 30 + fmChartTitle.getHeight();
	}
	
	/** The y pixel coordinate of the baseline for the chart title displayed at the top of the chart */
    private int chartTitleBaseline() {
		int pxAboveTopOfText = (topMargin() - fmChartTitle.getHeight()) / 2;
		return pxAboveTopOfText + fmChartTitle.getHeight();
	}

   private int xAxisTitleBaseline() {
		int pxBelowText = 5;
		return getHeight() - pxBelowText;  	   
   }

   private int xAxisValuesBaseline() {
	   return originY() + spaceBetweenAxisAndXValues() + fmLabels.getHeight();  	   
   }
   

   private int spaceBetweenAxisAndXValues(){
	   return 10;
   }

   private int spaceBetweenXValuesAndLabel(){
	   return 10;
   }


}
