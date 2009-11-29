package projections.gui.graph;

import projections.gui.*; 

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
 
public class Graph extends JPanel 
    implements MouseInputListener
{

	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

    public static final int STACKED   = 0;  // type of the bar graph
    public static final int UNSTACKED = 1;  // single, multiple or stacked
    public static final int AREA      = 2;  // Area graph (stacked)
    public static final int SINGLE    = 3;  // take the average of all y-values
    public static final int BAR       = 4;  // Graph type, bar graph
    public static final int LINE      = 5;  // or line graph

    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    
    private int GraphType;
    private boolean GraphStacked;
    private DataSource dataSource;
    private XAxis xAxis;
    private YAxis yAxis;
    private int originX;
    private int originY;
    private double xscale, yscale;
    
    private static final int FONT_SIZE = 12;   
    
    private static final double PI = Math.PI;

    private Color foreground;
    private Color background;
    
    private Font font = null;
    private FontMetrics fm = null;
    
    // number of pixels per value
    double pixelincrementX, pixelincrementY;
    // number of pixels per tick
    double tickIncrementX, tickIncrementY;

    // "best" values to be derived from pixelincrements
    long valuesPerTickX, valuesPerLabelX;
    long valuesPerTickY, valuesPerLabelY;

    private double barWidth;

    // for stacked data
    private double[][] stackArray;
    private double maxSumY;

    private int width;
    private int w,h;

    private int baseWidth = -1;
    private int baseHeight = -1;
    
    private double maxvalueX;
    private double maxvalueY;

    private Bubble bubble;
    private boolean showBubble;
    private int bubbleXVal;
    private int bubbleYVal;
    
    // Coefficients that describe a polynomial to plot on top of the chart data
    // y = polynomialToOverlay[2]*x^2 +  polynomialToOverlay[1]*x^1 +  polynomialToOverlay[0]*x^0
    // The y values are of the same units as the y values in the data source
    // The x values are measured in terms of the bin/interval indices
    private double[] polynomialToOverlay;

    /** Special constructor. This can only be called from a projections tool!!! */
    public Graph()
    {
	setPreferredSize(new Dimension(400,300));	
	
	GraphType = BAR;	   // default GraphType is BAR
	GraphStacked = true;    // default GraphType is STACKED
	stackArray = null;
	dataSource = null;
	
	xscale = 1.0;
	yscale = 1.0;
	
	addMouseMotionListener(this);
	addMouseListener(this);
	
	background = MainWindow.runObject[myRun].background;
	foreground = MainWindow.runObject[myRun].foreground;

    }
    

    /** Generic constructor. This can only be called from things that are not projections tools!!! */
    public Graph(Color background, Color foreground)
    {
	setPreferredSize(new Dimension(400,300));	
	
	GraphType = BAR;	   // default GraphType is BAR
	GraphStacked = true;    // default GraphType is STACKED
	stackArray = null;
	dataSource = null;
	
	xscale = 1.0;
	yscale = 1.0;
	
	addMouseMotionListener(this);
	addMouseListener(this);
	
	this.background = background;
	this.foreground = foreground;

    }
    
    
    
    /** Special constructor. This can only be called from a projections tool!!! */

    public Graph(DataSource d, XAxis x, YAxis  y)
    {
	// call default constructor
	this();
	
	xAxis = x;
	yAxis = y;
	dataSource = d;
	createStackArray();
    }

    public Dimension getMinimumSize() {
	return new Dimension(400,300);
    }

    // ***** API Interface to the control panel *****

    public void setGraphType(int type)
    {
	if ((type == LINE) || (type == BAR)) {
	    GraphType = type;      // set the Graphtype to LINE or BAR 
	    repaint();
	} else if (type == AREA) {
	    GraphType = type;
	    repaint();
	} else {
	     //unknown graph type.. do nothing ; draw a bargraph
	}
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
	setPreferredSize(new Dimension((int)(baseWidth*xscale),
				       (int)(baseHeight*yscale)));
	revalidate();
	repaint();
    }

    public void setScaleY(double val) {
	yscale = val;
	setPreferredSize(new Dimension((int)(baseWidth*xscale),
				       (int)(baseHeight*yscale)));
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
    	if( (xPos > originX) && (xPos < width+originX)) {
    		// get the expected value
    		int displacement = xPos - originX;
    		int whichBar = (int)(displacement/pixelincrementX);
    		double x1 = originX + (whichBar*pixelincrementX) + (pixelincrementX/2.0);
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
    		 (yPos < originY) && (yPos > 30) && 
    		 (stackArray != null) && 
    		 (xVal < stackArray.length) ) {
    		int numY = dataSource.getValueCount();
    		int y;
    		for (int k=0; k<numY; k++) {
    			y = originY - (int)(stackArray[xVal][k]*pixelincrementY);
    			if (yPos > y) {
    				return k;
    			}
    		}
    	}	
    	return -1;
    }

    // ***** Painting Routines *****
    protected void paintComponent(Graphics g) {
	    // Let UI delegate paint first 
	    // (including background filling, if I'm opaque)
	    super.paintComponent(g); 
	    // paint my contents next....

	    if (font == null) {
    		font = new Font("Times New Roman",Font.BOLD,FONT_SIZE);
    		g.setFont(font);
    		fm = g.getFontMetrics(font);
    	}
    	drawDisplay(g);
    }

    public void print(Graphics pg)
    {
	if (font == null) {
	    font = new Font("Times New Roman",Font.BOLD,FONT_SIZE);
	    pg.setFont(font);
	    fm = pg.getFontMetrics(font);
	}
	Color oldBackground = MainWindow.runObject[myRun].background;
	Color oldForeground = MainWindow.runObject[myRun].foreground;
	MainWindow.runObject[myRun].background = Color.white;
	MainWindow.runObject[myRun].foreground = Color.black;
	drawDisplay(pg);
	MainWindow.runObject[myRun].background = oldBackground;
	MainWindow.runObject[myRun].foreground = oldForeground;
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

    public void showPopup(int xVal, int yVal, int xPos, int yPos){

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


    private void drawDisplay(Graphics _g)
    {
	Graphics2D g = (Graphics2D)_g;
	
	g.setBackground(background);
	g.setColor(foreground);

	w = getWidth();
	h = getHeight();
	g.clearRect(0, 0, w, h);
	
	// if there's nothing to draw, don't draw anything!!!
	if (dataSource == null) {
	    return;
	}
	
	// xOffset and yOffsets are determined by the scrollbars that *may*
	// be asked to control this drawing component.
	// **CW** not active as of now.
	int xOffset = 0;
	int yOffset = 0;

	// baseWidth is whatever the width of the parent window at the time.
	// xscale will control just how much more of the graph one can see.
	baseWidth = getParent().getWidth();
	baseHeight = getParent().getHeight();

	String title = xAxis.getTitle();
	g.drawString(title,
		     (w-fm.stringWidth(title))/2 + xOffset, 
		     h - 10 + yOffset);
    
	// display Graph title
	title = dataSource.getTitle();
	g.drawString(title,
		     (w-fm.stringWidth(title))/2 + xOffset, 
		     10 + fm.getHeight() + yOffset);

	// display yAxis title
	title = yAxis.getTitle();
	g.rotate(-PI/2);
	g.drawString(title, 
		     -(h+fm.stringWidth(title))/2 + yOffset, 
		     fm.getHeight() + xOffset);
	g.rotate(PI/2);

	// total number of x values
	maxvalueX = dataSource.getIndexCount();
	// get max y Value
	if (((GraphType == BAR) && GraphStacked) || 
	    ((GraphType == LINE) && GraphStacked) || 
	    (GraphType == AREA)) {
	    maxvalueY = maxSumY;
	} else {
	    maxvalueY = yAxis.getMax();                               
	}

	originX = fm.getHeight()*2 + 
	    fm.stringWidth(""+(long)maxvalueY);
	/*  This is silly, we never wanna draw non-marked y values.
	originX = fm.getHeight()*2 + 
	    fm.stringWidth(yAxis.getValueName(maxvalueY));
	*/
	originY = h - (30 + fm.getHeight()*2);

	if ((xAxis != null) && (yAxis != null)) {
	    drawXAxis(g);
	    drawYAxis(g);
	    if (GraphType == BAR) {
		drawBarGraph(g);
	    } else if (GraphType == AREA) {
		drawAreaGraph(g);
	    } else {
		drawLineGraph(g);
	    }
	}
	
	
    }

    private void drawXAxis(Graphics2D g) {

	// width available for drawing the graph
    	width = (int)((baseWidth-30-originX)*xscale);

	// *NOTE* pixelincrementX = # pixels per value.
	pixelincrementX = (width)/maxvalueX;
	setBestIncrements(X_AXIS, pixelincrementX, (int)maxvalueX);

	// draw xAxis
	g.drawLine(originX, originY, width+originX, originY);

      	int mini = 0;
	int maxi = (int)maxvalueX;

	int curx;
	String s;

	// drawing xAxis divisions
	// based on the prior algorithm, it is guaranteed that the number of
	// iterations is finite since the number of pixels per tick will
	// never be allowed to be too small.
	for (int i=mini;i<maxi; i+=valuesPerTickX) {
	    curx = originX + (int)(i*pixelincrementX);
	    // don't attempt to adjust the tick positions midway unless we're
	    // on a one-to-one mapping.
	    if (valuesPerTickX == 1) {
		curx += (int)(tickIncrementX / 2);
	    }
	    // labels have higher lines.
	    if (i % valuesPerLabelX == 0) {
         	g.drawLine(curx, originY+5, curx, originY-5);
		s = xAxis.getIndexName(i);
		g.drawString(s, curx-fm.stringWidth(s)/2, originY + 10 + 
			     fm.getHeight());
	    } else {
         	g.drawLine(curx, originY+2, curx, originY-2);
	    }
	}
    }

    private void drawYAxis(Graphics2D g) {

	// draw yAxis
	g.drawLine(originX, originY, originX , 30);
	
	pixelincrementY = (originY-30) / maxvalueY;
	setBestIncrements(Y_AXIS, pixelincrementY, (long)maxvalueY);
	int sw = fm.getHeight();
	int cury;
	String yLabel;
	// drawing yAxis divisions
      	for (long i=0; i<=maxvalueY; i+=valuesPerTickY) {
	    cury = originY - (int)(i*pixelincrementY);
            if (i % valuesPerLabelY == 0) {
		g.drawLine(originX+5, cury, originX-5,cury);
		yLabel = "" + i; 
		g.drawString(yLabel, originX-fm.stringWidth(yLabel)-5, 
			     cury + sw/2);
	    } else {
            	g.drawLine(originX+2, cury, originX-2, cury);
	    }
	}
    }

    private void drawOverlayedPolynomial(Graphics2D g) {
    	// Plot the polynomial
    	if(polynomialToOverlay != null && polynomialToOverlay.length>0){

			g.setColor(Color.orange);
    		
    		String info = " ";
			for(int d=polynomialToOverlay.length-1; d>=0; d--){
				info += polynomialToOverlay[d] ;
				
				if(d==0)
					info += "";
				else if(d==1)
					info += " x";
				else
					info += " x^" + d;
					
				
				if(d>0)
					info += " + ";
			}
			g.drawString(info, w-fm.stringWidth(info) - 50 ,  20 );

			g.setStroke(new BasicStroke(4f));
			g.setColor(Color.orange);
			Object oldHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
    		for(int x=0; x< getWidth(); x++){

    			//    bar i center pixel coordinate:	
    			//       	originX + (int)(i*pixelincrementX)
    			//		
    			//    pixel x equates to bar:
    			//          (x-originX)/pixelincrementX

    			int x1_px = x;
    			double x1 = ((double)(x1_px-originX))/pixelincrementX;

    			int x2_px = x+1;
    			double x2 = ((double)(x2_px-originX))/pixelincrementX;

    			double y1 = 0.0;
    			double y2 = 0.0;
    			for(int d=0; d<polynomialToOverlay.length; d++){
    				y1 += polynomialToOverlay[d]*Math.pow(x1,d);
    				y2 += polynomialToOverlay[d]*Math.pow(x2,d);
    			}

    			int y1_px = originY - (int)(y1*pixelincrementY);
    			int y2_px = originY - (int)(y2*pixelincrementY);

    			if(  y1_px < (originY+2) && 
    				 y2_px < (originY+2) && 
    				 y1_px > (30-2) && 
    				 y2_px > (30-2) &&
    				 x1_px > originX-2 &&
    				 x2_px > originX-2 &&
    				 x1_px < (width+originX+2) &&
    				 x2_px < (width+originX+2)) {
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

    	// NO OPTIMIZATION simple draw. Every value gets drawn on screen.
    	for (int i=0; i<numX; i++) {
    		dataSource.getValues(i, data);
    		if (GraphStacked) {
    			int y = 0;
    			for (int k=0; k<numY; k++) {
    				// calculating lowerbound of box, which StackArray
    				// allready contains
    				y = originY - (int)(stackArray[i][k]*pixelincrementY);
    				g.setColor(dataSource.getColor(k));
    				// using data[i] to get the height of this bar
    				if (valuesPerTickX == 1) {
    					g.fillRect(originX + (int)(i*pixelincrementX +
    							tickIncrementX/2 -
    							barWidth/2), y,
    							(int)barWidth,
    							(int)(data[k]*pixelincrementY));

    				} else {
    					g.fillRect(originX + (int)(i*pixelincrementX), y, 
    							(int)((i+1)*pixelincrementX) - 
    							(int)(i*pixelincrementX), 
    							(int)(data[k]*pixelincrementY));
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
    				g.setColor(dataSource.getColor((int)temp[k][0]));
    				y = (originY-(int)(temp[k][1]*pixelincrementY));
    				if (valuesPerTickX == 1) {
    					g.fillRect(originX + (int)(i*pixelincrementX +
    							tickIncrementX/2 -
    							barWidth/2), y,
    							(int)barWidth,
    							(int)(temp[k][1]*pixelincrementY));
    				} else {				   
    					g.fillRect(originX + (int)(i*pixelincrementX), y,
    							(int)((i+1)*pixelincrementX) -
    							(int)(i*pixelincrementX),
    							(int)(temp[k][1]*pixelincrementY));
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
		y = (int)(originY - (int)(sum*pixelincrementY));
		g.fillRect(originX + (int)(i*pixelincrementX),y,
			   (int)((i+1)*pixelincrementX) -
			   (int)(i*pixelincrementX),
			   (int)(sum*pixelincrementY));
	    }		
    		 */
    	}
    	
    	drawOverlayedPolynomial(g);
    }
	
    public void drawLineGraph(Graphics2D g) {
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
	    x2 = originX + (int)(i*pixelincrementX) + 
		(int)(pixelincrementX/2);    
	 
	    for (int j=0; j<yValues; j++) {
		g.setColor(dataSource.getColor(j));
		y2[j] = (originY - (int)(data[j]*pixelincrementY));
		//is there any other condition that needs to be checked?
		if(x1 != -1)	
		    g.drawLine(x1,y1[j],x2,y2[j]);
		y1[j] = y2[j];		
	    }
	    x1 = x2;
	}

	drawOverlayedPolynomial(g);
    }

    public void drawAreaGraph(Graphics2D g) {
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

	    // offsetY is meant to allow the area graph to not draw over
	    // the origin line.
	    int offsetY = -2;

	    for (int idx=0; idx<xValues; idx++) {
		// get the y values given the x value
		// **CW** NOTE to self: This is silly. Why do I have to
		// get the data each time when I only need to get it once!?
		// Fix once I have the time.
		dataSource.getValues(idx,data);

		//calculate x & y pixel values
		xPixel = originX + (int)(idx*pixelincrementX) + 
		    (int)(pixelincrementX/2);    
		// **CW** NOTE will need some refactoring to prevent
		// recomputation of the prefix sum each time we go through
		// the Y values.
		prefixSum(data);
	    	yPixel = (originY - (int)(data[layer]*pixelincrementY) +
				offsetY);

		// if first point, add the baseline point before adding
		// the first point.
		if (idx == 0) {
		    // just so it does not overwrite the Axis line.
		    polygon.addPoint(xPixel, originY+offsetY);
		}
		// add new point to polygon
		polygon.addPoint(xPixel, yPixel);
		// if last point, add the baseline point after adding
		// the last point.
		if (idx == xValues-1) {
		    // just so it does not overwrite the Axis line.
		    polygon.addPoint(xPixel, originY+offsetY);
		}
	    }
	    // draw the filled polygon.
	    g.setColor(dataSource.getColor(layer));
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
    private void setBestIncrements(int axis, double pixelsPerValue, 
				   long maxValue) {
	long index = 0;
	long labelValue = getNextLabelValue(index);
	long tickValue = getNextTickValue(index++);

	int labelWidth = 0;
	while (true) {
	    if (axis == X_AXIS) {
		labelWidth = 
		    fm.stringWidth(xAxis.getIndexName((int)(maxValue-1)));
	    } else {
		labelWidth = fm.getHeight();
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
			tickIncrementY = tickValue*pixelsPerValue;
			valuesPerTickY = tickValue;
			valuesPerLabelY = labelValue;
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
    
}
