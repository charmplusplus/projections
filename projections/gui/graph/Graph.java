package projections.gui.graph;

import projections.gui.*; 

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
 
public class Graph extends JPanel 
    implements MouseMotionListener
{
    public static final int STACKED   = 0;  // type of the bar graph
    public static final int UNSTACKED = 1;  // single, multiple or stacked
    public static final int AREA      = 2;  // Area graph (stacked)
    public static final int SINGLE    = 3;  // take the average of all y-values
    public static final int BAR       = 4;  // Graph type, bar graph
    public static final int LINE      = 5;  // or line graph
    
    private int GraphType;
    private int BarGraphType;
    private DataSource dataSource;
    private XAxis xAxis;
    private YAxis yAxis;
    private int originX;
    private int originY;
    private double xscale;
    
    private static final int FONT_SIZE = 12;   
    
    private static final double PI = 3.142;

    private Font font = null;
    private FontMetrics fm = null;
    private Color labelColor;
    
    // to print y axis title vertically
    private JLabel yLabel;	

    // number of pixels per value
    double pixelincrementX, pixelincrementY;
    // number of pixels per tick
    double tickIncrementX, tickIncrementY;

    // "best" values to be derived from pixelincrements
    long valuesPerTick, valuesPerLabel;

    private double barWidth;

    // for stacked data
    private double[][] stackArray;
    private int maxSumY;

    private int width;
    private int w,h;

    private int baseWidth = -1;

    private Bubble bubble;
    private int bubbleXVal;
    private int bubbleYVal;

    public Graph()
    {
	setPreferredSize(new Dimension(400,300));	
	
	GraphType = BAR;	   // default GraphType is BAR
	BarGraphType = STACKED;    // default BarGraphType is STACKED
	labelColor = Analysis.foreground;
	stackArray = null;
	dataSource = null;
	
	xscale = 1.0;
	
	addMouseMotionListener(this);
    }
    
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
	    ; //unknown graph type.. do nothing ; draw a bargraph
	}
    }

    public int getGraphType()
    {
	return GraphType;
    }

    public void setBarGraphType(int type)
    {
	if ((type == STACKED) || (type == UNSTACKED) || (type == SINGLE)) {
	    BarGraphType = type;
	} else {
	    ; //unknown bar graph type.. do nothing ; draw STACKED bar graph
	}
	repaint();
    }

    private String getBarGraphType()
    {
	if(BarGraphType == SINGLE)
	    return("avg");
	if(BarGraphType == UNSTACKED)
	    return("unstacked");
	return("stacked");
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

    public void setLabelColor(Color c) {
	labelColor = c;
	repaint();
    }

    public void setScale(double val) {
	xscale = val;
	setPreferredSize(new Dimension((int)(baseWidth*xscale),getHeight()));
	repaint();
	revalidate();
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
		if (tempMax < stackArray[k][numY-1]) {
		    tempMax = stackArray[k][numY-1];
		}
	    }
	    maxSumY = (int)(Math.ceil(tempMax));
	} else {
	    stackArray = null;
	}
    }

    // getXValue(x)
    // 	returns the x value of the bar graph the mouse is currently over
    // 	if the mouse is not over any bar data, then return -1
    private int getXValue(int xPos) {
   	if( (xPos > originX) && (xPos < (int)width+originX)) {
	    int numX = dataSource.getIndexCount();
	    int x1;
	    for (int k=0; k<numX; k++) {
		x1 = originX + (int)(k*pixelincrementX) + 
		    (int)(pixelincrementX/2);
		if ((xPos > x1-(barWidth/2)) && (xPos < x1+(barWidth/2))) {
		    return k;
		}
	    }
	}
	return -1;
    }
	
    private int getYValue(int xVal, int yPos) {
	if ((xVal >= 0) && (yPos < originY) && (yPos > 30) && 
	    (stackArray != null)) {
	    int numY = dataSource.getValueCount();
	    int y;
	    for (int k=0; k<numY; k++) {
		y = (int) (originY - (int)stackArray[xVal][k]*pixelincrementY);
		if (yPos > y) {
		    return k;
		}
	    }
	}	
	return -1;
    }

    // ***** Painting Routines *****
    
    public void paint(Graphics g)
    {
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
	setBackground(Color.white);
	setForeground(Color.black);
	drawDisplay(pg);
	setBackground(Analysis.background);
	setForeground(Analysis.foreground);
    }

    public void mouseMoved(MouseEvent e) {
	int x = e.getX();
    	int y = e.getY();
	
	int index,valNo;
	double value;
	
	int xVal = getXValue(x);
	int yVal = getYValue(xVal, y);
	
	if((xVal > -1) && (yVal > -1)) {
	    showPopup(xVal, yVal, x, y);
	} else if (bubble != null) {
	    bubble.setVisible(false);
	    bubble.dispose();
	    bubble = null;
	}
    }

    public void showPopup(int xVal, int yVal, int xPos, int yPos){
	String text[] = dataSource.getPopup(xVal, yVal);
	if (text == null) {
	    return;
	}
	// else display ballon
	int bX, bY;
	// I'm doing these calculations, i probably should see if I
	// can avoid it
		
	// old popup still exists, but mouse has moved over a new 
	// section that has its own popup
	if (bubble != null && (bubbleXVal != xVal || bubbleYVal != yVal)){
	    bubble.setVisible(false);
	    bubble.dispose();
	    bubble = null;
	}
	
	if (bubble == null) {
	    if (BarGraphType== STACKED) {
		bubble = new Bubble(this, text);
		bubble.setLocation(xPos+20, yPos+20);//(bX, bY);
		bubble.setVisible(true);
		bubbleXVal = xVal;
		bubbleYVal = yVal;
		//System.out.println(bubbleXVal +", " + bubbleYVal);	
	    } else {
		//System.out.println("not Stacked");
	    }
	}
    }	

    public void mouseDragged(MouseEvent e) {
    }

    private void drawDisplay(Graphics _g)
    {
	Graphics2D g = (Graphics2D)_g;
	
	g.setBackground(Analysis.background);
	g.setColor(Analysis.foreground);

	w = getWidth();
	h = getHeight();
	g.clearRect(0, 0, w, h);
	
	if ((xAxis != null) && (yAxis != null)) {
	    drawAxes(g);	
	    if (GraphType == BAR) {
		drawBarGraph(g);
	    } else if (GraphType == AREA) {
		drawAreaGraph(g);
	    } else {
		drawLineGraph(g);
	    }
	}
    }

    private void drawAxes(Graphics2D g) {

	// if there's nothing to draw, don't draw anything!!!
	if (dataSource == null) {
	    return;
	}

	String yTitle = yAxis.getTitle();
	String temp   = "";
	
	for (int i=0; i < yTitle.length(); i++) {
	    temp += yTitle.charAt(i)+"\n";
	}
	yLabel = new JLabel(temp);
	
	// get max y Value
	double maxvalueY;
	if (((GraphType == BAR) && (BarGraphType == STACKED)) || 
	    (GraphType == AREA)) {
	    maxvalueY = maxSumY;
	} else {
	    maxvalueY = yAxis.getMax();                               
	}

	// baseWidth is whatever the width of the parent window at the time.
	// xscale will control just how much more of the graph one can see.
	baseWidth = getParent().getWidth();

	originX = fm.getHeight()*2 + fm.stringWidth(String.valueOf(maxvalueY));
	originY = h - (30 + fm.getHeight()*2);

	// i.e. find the left and the lower margins
	g.setColor(labelColor);
	String title = xAxis.getTitle();
	//+" ("+getBarGraphType()+")";
	   
	// display xAxis title
	g.drawString(title,(w-fm.stringWidth(title))/2, h - 10);
	title = dataSource.getTitle();
	// display Graph title
	g.drawString(title,(w-fm.stringWidth(title))/2, 10 + fm.getHeight());
	title = yAxis.getTitle();
	g.rotate(-PI/2);
	// display yAxis title
	g.drawString(title, -(h+fm.stringWidth(title))/2, 
		     fm.getHeight());
	g.rotate(PI/2);
	g.setColor(Analysis.foreground);	  

	// width available for drawing the graph
    	width = (int)((baseWidth-30-originX)*xscale);

	// total number of x values
	int maxvalue = dataSource.getIndexCount();
	int sw = fm.stringWidth("" + (maxvalue*xAxis.getMultiplier()));

	// *NOTE* pixelincrementX = # pixels per value.
	pixelincrementX = ((double)width)/maxvalue;
	setBestIncrements(pixelincrementX, maxvalue);

	// draw xAxis
	g.drawLine(originX, originY, (int)width+originX, originY);
	// draw yAxis
	g.drawLine(originX, originY, originX , 30);
	
      	int mini = 0;
	int maxi = maxvalue;

	int curx, cury;
	String s;

	// drawing xAxis divisions
	// based on the prior algorithm, it is guaranteed that the number of
	// iterations is finite since the number of pixels per tick will
	// never be allowed to be too small.
	for (int i=mini;i<maxi; i+=valuesPerTick) {
	    curx = originX + (int)(i*pixelincrementX);
	    curx += (int)(tickIncrementX / 2);
	    // labels have higher lines.
	    if (i % valuesPerLabel == 0) {
         	g.drawLine(curx, originY+5, curx, originY-5);
		s = xAxis.getIndexName(i);
		g.drawString(s, curx-fm.stringWidth(s)/2, originY + 10 + 
			     fm.getHeight());
	    } else {
         	g.drawLine(curx, originY+2, curx, originY-2);
	    }
	}

	// adjust so that the max axis value is in the multiples of 10
	maxvalueY += (10 - maxvalueY%10);
	pixelincrementY = (double)(originY-30) / maxvalueY;
	sw = fm.getHeight();
      
	int labelincrementY = (int)Math.ceil((sw + 20) / pixelincrementY);
	// according to chee wai getBestIncrement is very inefficient
	labelincrementY = Util.getBestIncrement(labelincrementY);
	// same functionality could probably be incorporated in a simpler
	// function

	int subincrement = labelincrementY/10;
	if (subincrement < 1) subincrement = 1;

	// drawing yAxis divisions
      	for (int i=0; i<=maxvalueY; i+=subincrement) {
	    cury = originY - (int)(i*pixelincrementY);
            if (i % labelincrementY == 0) {
		g.drawLine(originX+5, cury, originX-5,cury);
		s = "" + (int)(i); 
		g.drawString(s, originX-fm.stringWidth(s)-5, cury + sw/2);
	    } else {
            	g.drawLine(originX+2, cury, originX-2, cury);
	    }
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
	if ((valuesPerTick == 1) && (tickIncrementX >= 5.0)) {
	    barWidth = 0.8*tickIncrementX;
	} else {
	    barWidth = 1.0;
	}

	// NO OPTIMIZATION simple draw. Every value gets drawn on screen.
	for (int i=0; i<numX; i++) {
	    dataSource.getValues(i, data);
	    if (BarGraphType == STACKED) {
		int y = 0;
		for (int k=0; k<numY; k++) {
		    // calculating lowerbound of box, which StackArray
		    // allready contains
		    y = originY - (int)(stackArray[i][k]*pixelincrementY);
		    g.setColor(dataSource.getColor(k));
		    // using data[i] to get the height of this bar
		    if (valuesPerTick == 1) {
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
	    } else if (BarGraphType == UNSTACKED) {
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
		    y = (int)(originY-(int)(temp[k][1]*pixelincrementY));
		    if (valuesPerTick == 1) {
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
	}
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
	    dataSource.getValues(i,data);
	    //calculate x value
	    x2 = originX + (int)(i*pixelincrementX) + 
		(int)(pixelincrementX/2);    
	 
	    for (int j=0; j<yValues; j++) {
		g.setColor(dataSource.getColor(j));
		y2[j] = (int) (originY - (int)(data[j]*pixelincrementY));
		//is there any other condition that needs to be checked?
		if(x1 != -1)	
		    g.drawLine(x1,y1[j],x2,y2[j]);
		y1[j] = y2[j];		
	    }
	    x1 = x2;
	}
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
	    	yPixel = (int) (originY - (int)(data[layer]*pixelincrementY));

		// if first point, add the baseline point before adding
		// the first point.
		if (idx == 0) {
		    // just so it does not overwrite the Axis line.
		    polygon.addPoint(xPixel, originY-1);
		}
		// add new point to polygon
		polygon.addPoint(xPixel, yPixel);
		// if last point, add the baseline point after adding
		// the last point.
		if (idx == xValues-1) {
		    // just so it does not overwrite the Axis line.
		    polygon.addPoint(xPixel, originY-1);
		}
		
	    }
	    // draw the filled polygon.
	    g.setColor(dataSource.getColor(layer));
	    g.fill(polygon);
	    // draw a black outline.
	    g.setColor(Color.black);
	    g.draw(polygon);
	}
    }

    // in-place computation of the prefix sum
    private void prefixSum(double data[]) {
	// computation is offset by 1
	for (int i=0; i<data.length-1; i++) {
	    data[i+1] = data[i+1]+data[i];
	}
    }

    private double findMaxOfSums() {
	double maxValue = 0.0;

	int xValues = dataSource.getIndexCount();
	int yValues = dataSource.getValueCount(); // no. of y values for each x
	double data[] = new double[yValues];

	for (int i=0; i<xValues; i++) {
	    dataSource.getValues(i, data);
	    prefixSum(data);
	    if (maxValue < data[yValues-1]) {
		maxValue = data[yValues-1];
	    }
	}
	return maxValue;
    }

    /**
     *  Determines "best" values for tickIncrements and valuesPerTick and
     *  valuesPerLabel.
     *
     *  Sets global (yucks!) variables to reflect the "best" number of pixels
     *  for Labels (5, 10, 50, 100, 500, 1000 etc ...) and the number of pixels
     *  for each tick (1, 10, 100, 1000 etc ...) based on the "best" label
     *  pixel number.
     */
    private void setBestIncrements(double pixelsPerValue, long maxValue) {
	long index = 0;
	long labelValue = getNextLabelValue(index);
	long tickValue = getNextTickValue(index++);

	while (true) {
	    // is the number of pixels to display a label too small?
	    if (fm.stringWidth(String.valueOf(maxValue)) >
		(pixelsPerValue*labelValue*0.8)) {
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
		    tickIncrementX = tickValue*pixelsPerValue;
		    valuesPerTick = tickValue;
		    valuesPerLabel = labelValue;
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
	g.setBarGraphType(Graph.UNSTACKED);
	*/
	/* **TESTCASE - STACKED BAR**
	g.setGraphType(Graph.BAR);
	g.setBarGraphType(Graph.STACKED);
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
}
