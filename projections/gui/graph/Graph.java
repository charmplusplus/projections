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
    private static final Color BACKGROUND = Color.black;
    private static final Color FOREGROUND = Color.white;
    
    private static final double PI = 3.142;

    private Font font = null;
    private FontMetrics fm = null;
    private Color labelColor;
    
    private JLabel yLabel;	// to print y axis title vertically
    private int w,h,tickincrementX,tickincrementY;
    double pixelincrementX,pixelincrementY;

    private double[][] stackArray;

    private int maxSumY;
    private int barWidth, width;

    // **CW** tentative hack
    private int baseWidth;

    private boolean fitToScreen = false;
	
    private Bubble bubble;

    public Graph()
    {
	setPreferredSize(new Dimension(400,300));	
	baseWidth = getSize().width;
	
	GraphType = BAR;	   // default GraphType is BAR
	BarGraphType = STACKED;    // default BarGraphType is STACKED
	labelColor = FOREGROUND;
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
	w = getSize().width;
	h = getSize().height;

	drawDisplay(g);
    }

    public void print(Graphics pg)
    {
	if (font == null) {
	    font = new Font("Times New Roman",Font.BOLD,FONT_SIZE);
	    pg.setFont(font);
	    fm = pg.getFontMetrics(font);
	}
	w = getSize().width;
	h = getSize().height;

	setBackground(Color.white);
	setForeground(Color.black);
	drawDisplay(pg);
	setBackground(BACKGROUND);
	setForeground(FOREGROUND);
    }

    public void mouseMoved(MouseEvent e) {
	//System.out.println("Mouse moved"+ e);
	int x = e.getX();
    	int y = e.getY();

	int index,valNo;
	double value;
	
	int xVal = getXValue(x);
	int yVal = getYValue(xVal, y);
	// System.out.println("(" + xVal +"," +yVal +")");
	
	if((xVal > -1) && (yVal > -1)) {
	    showPopup(xVal, yVal, x, y);
	} else if (bubble != null) {
	    //System.out.println("Bubble is not null");
	    bubble.setVisible(false);
	    bubble.dispose();
	    bubble = null;
	}
    }

    public void showPopup(int xVal, int yVal, int xPos, int yPos){
	//System.out.println("graph.showPopup()");
	//System.out.println(xVal +", " + yVal);
	String text[] = dataSource.getPopup(xVal, yVal);
	if (text == null) {
	    return;
	}
	// else display ballon
	int bX, bY;
	// I'm doing these calculations, i probably should see if I
	// can avoid it
	if (bubble == null) {
	    if (BarGraphType== STACKED) {
		// bX = originX + (int)(xVal*pixelincrementX) + 
		// (int)(pixelincrementX/2) + barWidth + 10;
		// bY = (int) (originY - 
		// (int)(stackArray[xVal][yVal]*pixelincrementY) +15);	
		bubble = new Bubble(this, text);
		bubble.setLocation(xPos+20, yPos+20);//(bX, bY);
		bubble.setVisible(true);
		System.out.println(xPos +", " + yPos);	
	    } else {
		System.out.println("not Stacked");
	    }
	}
    }	

    public void mouseDragged(MouseEvent e) {
    }

    private void drawDisplay(Graphics _g)
    {
	Graphics2D g = (Graphics2D)_g;
	g.setBackground(BACKGROUND);
	g.setColor(FOREGROUND);
	
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

	String yTitle = yAxis.getTitle();
	String temp   = "";
	
	for (int i=0; i < yTitle.length(); i++) {
	    temp += yTitle.charAt(i)+"\n";
	}
	yLabel = new JLabel(temp);
		
	originX = fm.getHeight()*4;
	// originX = (30 + fm.stringWidth(yAxis.getTitle()) + 
	// fm.stringWidth(""+yAxis.getMax()));			
	originY = h - (30 + 2 * fm.getHeight());
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
	g.setColor(FOREGROUND);	  

	// width available for drawing the graph
    	width = (int)((baseWidth-30-originX)*xscale);

	// total number of x values
	int maxvalue = dataSource.getIndexCount();
	int sw = fm.stringWidth("" + (maxvalue*xAxis.getMultiplier()));

	// *NOTE* tickincrement = # of values per tick.
	if (fitToScreen) {
	    tickincrementX = (int)Math.ceil(5/((double)(width)/maxvalue));
	    tickincrementX = Util.getBestIncrement(tickincrementX);
	} else {
	    tickincrementX = 1;
	}
	int numintervalsX = (int)Math.ceil((double)maxvalue/tickincrementX);

	// *NOTE* pixelincrementX = # pixels per tick.
	pixelincrementX = (double)(width)/numintervalsX;
	// make sure the number of pixels per tick is not too small.
	if (pixelincrementX < 4.0) {
	    pixelincrementX = 4.0;
	}
	// expand the panel width to accomodate the graph even for the "fit
	// to screen" case where rounding errors *will* sometimes cause it
	// to actually be larger than the screen.
	if (width < (int)(pixelincrementX*numintervalsX)) {
	    width = (int)(pixelincrementX*numintervalsX);
	    baseWidth = (int)((width/xscale)+30+originX);
	    w = width+30+originX;
	    setPreferredSize(new Dimension(w,h));
	}

	int labelincrementX = (int)Math.ceil((sw + 20) / pixelincrementX);
	labelincrementX = Util.getBestIncrement(labelincrementX);
	// draw xAxis
	g.drawLine(originX, originY, (int)width+originX, originY);
	// draw yAxis
	g.drawLine(originX, originY, originX , 30);
	
      	int mini = 0;
	int maxi = maxvalue;

	if (maxi > numintervalsX) {
	    maxi = numintervalsX;
	}

	int curx, cury;
	String s;

	//drawing xAxis divisions
	for (int i=mini;i<maxi; i++) {
	    curx = originX + (int)(i*pixelincrementX);
	    curx += (int)(pixelincrementX / 2);
	    if (i % labelincrementX == 0) {
         	g.drawLine(curx, originY+5, curx, originY-5);
		// can set multiplier? 
		s = "" + (int)xAxis.getIndex(i);
		g.drawString(s, curx-fm.stringWidth(s)/2, originY + 10 + 
			     fm.getHeight());
	    } else {
         	g.drawLine(curx, originY+2, curx, originY-2);
	    }
	}

	// get max y Value
	double maxvalueY;
	if (((GraphType == BAR) && (BarGraphType == STACKED)) || 
	    (GraphType == AREA)) {
	    maxvalueY = maxSumY;
	} else {
	    maxvalueY = yAxis.getMax();                               
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
	int x1, x2;
	// if the number of pixels between ticks is small, use a 1 or 2 pixel
	// separator depending on an odd or even number of pixels. Otherwise,
	// it is a simple percentage of the tick space.
	if (pixelincrementX < 6.0) {
	    if ((int)(pixelincrementX)%2 == 0) {
		barWidth = (int)(pixelincrementX-1);
	    } else {
		barWidth = (int)(pixelincrementX-2);
	    }
	} else {
	    barWidth = (int)(pixelincrementX*0.75);
	}

	for (int i=0; i<numX; i++) {
	    dataSource.getValues(i, data);
	    // calculate X value
	    x1 = originX + (int)(i*pixelincrementX) + 
		(int)(pixelincrementX/2);
	    if (BarGraphType == STACKED) {
		int y = 0;
		for (int k=0; k<numY; k++) {
		    // calculating lowerbound of box, which StackArray
		    // allready contains
		    y = originY - (int)(stackArray[i][k]*pixelincrementY);
		    g.setColor(dataSource.getColor(k));
		    // using data[i] to get the heigh of this bar
		    g.fillRect(x1-(barWidth/2),y, barWidth, 
			       (int)(data[k]*pixelincrementY));
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
		    g.fillRect(x1-(barWidth/2),y,barWidth,
			       (int)(temp[k][1]*pixelincrementY));
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
		g.fillRect(x1-(barWidth/2),y,barWidth,
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

