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
    private int hsbval;
    
    private static final int FONT_SIZE = 12;   
    private static final Color BACKGROUND = Color.black;
    private static final Color FOREGROUND = Color.white;
    
    private static final double PI = 3.142;


   private FontMetrics fm;
   private Color labelColor;
   private Image offscreen; 
   
   private JLabel yLabel;	// to print y axis title vertically
   private int w,h,tickincrementX,tickincrementY;
   double pixelincrementX,pixelincrementY;

   private double[][] stackArray;

   private int maxSumY;
	private int barWidth, width;

   public Graph()
   {

	setSize(getPreferredSize());	
	

	GraphType = BAR;			// default GraphType is BAR
	BarGraphType = STACKED;                 // default BarGraphType is STACKED
//	labelColor = Color.yellow;
	labelColor = FOREGROUND;
	stackArray = null;
	dataSource = null;

	xscale = 1.0;
	hsbval = 0;
	offscreen = null;
	
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

   
    //Make sure we aren't made too tiny
    public Dimension getMinimumSize() {return new Dimension(500,400);}
    public Dimension getPreferredSize() {return new Dimension(500,400);}
    
    // ***** API Interface to the control panel *****

    public void setGraphType(int type)
    {
	if ((type == LINE) || (type == BAR)) {
	    GraphType = type;      // set the Graphtype to LINE or BAR 
	} else if (type == AREA) {
	    GraphType = type;
	    
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
	if((type == STACKED) || (type == UNSTACKED) || (type == SINGLE))
	    BarGraphType = type;
	else
		; //unknown bar graph type.. do nothing ; draw STACKED bar graph
	
	if(type == STACKED){
//		createStackArray();
	}

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
   }

   public void setData(DataSource d)
   {
        dataSource = d;
		createStackArray();
   }

   public void setLabelColor(Color c){
	labelColor = c;
    }

    public void setHSBValue(int val){
	hsbval = val;
    }
    
    public void setScale(double val){
	xscale = val;
    }


   private void createStackArray(){
		if(dataSource != null){
		 maxSumY = 0;
		 int numY = dataSource.getValueCount();
		 stackArray = new double[dataSource.getIndexCount()][];
			for(int k=0; k<dataSource.getIndexCount(); k++){
			 stackArray[k] = new double[numY];

			 dataSource.getValues(k, stackArray[k]);

			 for(int j=1; j<numY; j++)
				 stackArray[k][j] += stackArray[k][j-1];

			 if(maxSumY < stackArray[k][numY-1])
				 maxSumY = (int) stackArray[k][numY-1] + 1;			// +1 to ensure maxSumY greater than all Y in stackArray
				 																	// while still keeping maxSumy an int
			}
		}else{
		 stackArray = null;
		}
   }

	// getXValue(x)
	// 	returns the x value of the bar graph the mouse is currently over
	// 	if the mouse is not over any bar data, then return -1
	private int getXValue(int xPos){
   	if( (xPos > originX) && (xPos < (int)width+originX)){
			int numX = dataSource.getIndexCount();
			int x1;
			for(int k=0; k<numX; k++){
				x1 = originX + (int)(k*pixelincrementX) + (int)(pixelincrementX/2);

				if( (xPos > x1-(barWidth/2)) && (xPos < x1+(barWidth/2)) )
					 return k;
			}
		}
				
		return -1;
   }
	
	private int getYValue(int xVal, int yPos){
		if( (xVal >= 0) && (yPos < originY) && (yPos > 30) && (stackArray != null)){
			int numY = dataSource.getValueCount();
			int y;
			for(int k=0; k<numY; k++){
				y = (int) (originY - (int)stackArray[xVal][k]*pixelincrementY);
				if(yPos > y )
					return k;
			}
		
		
		}	
	
		return -1;
	}


    // ***** Painting Routines *****
    

   public void paintComponent(Graphics g)
   {
	  super.paintComponent(g);
	  g.setFont(new Font("Times New Roman",Font.BOLD,FONT_SIZE));

	  w = getSize().width;
     h = getSize().height;

	  drawDisplay(g);
   }

    public void print(Graphics pg)
    {
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
		
		showPopup(getPopup(xVal, yVal), x, y);
   }
	
	// This should be inherited if a popup is wanted
	public String getPopup(int xVal, int yVal){
		return null;
	}
	
	public void showPopup(String text, int xPos, int yPos){
		if(text == null)
			return;
		
	}
	
	

   public void mouseDragged(MouseEvent e) {
   }

    /* **CW** I believe this is messing up Swing's double buffering support.
   public void setBounds(int x, int y, int w, int h)
   { 
	
	 // make offscreen image
          if(getSize().width != w || getSize().height != h)
          {
                 try
                 {
                        offscreen = createImage(w, h);
                 }
                 catch(OutOfMemoryError e)
                 {
                        System.out.println("NOT ENOUGH MEMORY!");
                 }
          }
          super.setBounds(x, y, w, h);
   }
    */
   private void drawDisplay(Graphics _g)
   {
       Graphics2D g = (Graphics2D)_g;
       g.setBackground(BACKGROUND);
       g.setColor(FOREGROUND);

          if(fm == null)
          {
                 fm = g.getFontMetrics(g.getFont());
          }
          w = getSize().width;
          h = getSize().height;

          g.clearRect(0, 0, w, h);
	  g.translate(-hsbval, 0); 

	  if((xAxis != null) && (yAxis != null))
	  {
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

   private void drawAxes(Graphics2D g){
		// set font
	   g.setFont(new Font("Times New Roman",Font.BOLD,FONT_SIZE));
	   fm = g.getFontMetrics(g.getFont()); 
          
	   w = getSize().width;
      h = getSize().height;

	   String yTitle = yAxis.getTitle();
	   String temp   = "";
	
	   for(int i=0; i < yTitle.length(); i++)
			temp += yTitle.charAt(i)+"\n";
 
      yLabel = new JLabel(temp);
		
	  originX = fm.getHeight()*4;
	  //	  originX = (30 + fm.stringWidth(yAxis.getTitle())+ fm.stringWidth(""+yAxis.getMax()));			// determine where to do draw the graph
																			// determine where to do draw the graph
	   originY = h - (30 + 2 * fm.getHeight());				// i.e. find the left and the lower margins

	   g.setColor(labelColor);
	   String title = xAxis.getTitle();							//+" ("+getBarGraphType()+")";
	   
		g.drawString(title,(w-fm.stringWidth(title))/2, h - 10);							// display xAxis title

	   title = dataSource.getTitle();
	   g.drawString(title,(w-fm.stringWidth(title))/2, 10 + fm.getHeight());		// display Graph title

	   title = yAxis.getTitle();
	   g.rotate(-PI/2);
	   g.drawString(title, -(h+fm.stringWidth(title))/2, 
		fm.getHeight());     																		// display yAxis title
	   g.rotate(PI/2);
	   g.setColor(FOREGROUND);	  

    	width = (int)((w-30-originX)*xscale);		      	// width available for drawing the graph
	   int maxvalue = dataSource.getIndexCount();         // total number of x values
	   int sw = fm.stringWidth("" + (maxvalue*xAxis.getMultiplier()));
	   tickincrementX = (int)Math.ceil(5/((double)(width)/maxvalue));
      tickincrementX = Util.getBestIncrement(tickincrementX);

      int numintervalsX = (int)Math.ceil((double)maxvalue/tickincrementX);
	   pixelincrementX = (double)(width) / numintervalsX;

      int labelincrementX = (int)Math.ceil((sw + 20) / pixelincrementX);
      labelincrementX = Util.getBestIncrement(labelincrementX);

	   g.drawLine(originX, originY, (int)width+originX, originY); 				// draw xAxis
	   g.drawLine(originX, originY, originX , 30); 									// draw yAxis

      	int mini = 0; 							//(int)Math.floor((hsbval - data.offset3)/pixelincrement);
         int maxi = maxvalue;							// (int)Math.ceil((hsbval - data.offset3 + w)/pixelincrement);
         if(mini < 0) mini = 0;
         if(maxi > numintervalsX) maxi = numintervalsX;

	   g.setFont(new Font("Times New Roman",Font.BOLD,FONT_SIZE));
		fm = g.getFontMetrics(g.getFont()); 
      int curx,cury;
      String s;
      for(int i=mini;i<maxi; i++){	//drawing xAxis divisions
			curx = originX + (int)(i*pixelincrementX);         //xscale);			//pixelincrementX);
 
         curx += (int)(pixelincrementX / 2);		//xscale/2);				
 
         if(i % labelincrementX == 0)
         {
         	g.drawLine(curx, originY+5, curx, originY-5);
            s = "" + (int)xAxis.getIndex(i);						// can set multiplier? 
            g.drawString(s, curx-fm.stringWidth(s)/2, originY + 10 + fm.getHeight());
         }
         else
         	g.drawLine(curx, originY+2, curx, originY-2);
      }

	   double maxvalueY;														// get max y Value
	   if(((GraphType == BAR) && (BarGraphType == STACKED)) || (GraphType == AREA) ){
	  		maxvalueY = maxSumY;
	   }else{
	  		maxvalueY = yAxis.getMax();                               
	   }
	   maxvalueY += (10 - maxvalueY%10);				  // adjust so that the max axis value is in the multiples of 10
	   pixelincrementY = (double)(originY-30) / maxvalueY;
//    pixelincrementY = (int)pixelincrementY;
	   sw = fm.getHeight();
      
		int labelincrementY = (int)Math.ceil((sw + 20) / pixelincrementY);
      labelincrementY = Util.getBestIncrement(labelincrementY);	// according to chee wai getBestIncrement is very inefficient
																						// same functionality could probably be incorporated in a simpler
																						// function

		int subincrement = labelincrementY/10;
		if (subincrement < 1) subincrement = 1;
         //for(int i=0; i<=maxvalueY; i++)			      			// drawing yAxis divisions
      	for(int i=0; i<=maxvalueY; i+=subincrement)			      // drawing yAxis divisions
         {

// for each i from 0 to maxY (even if it is something like a million), we are checking for the cond below & displaying it
// efficient of maxY is low (less than the height of the graph
// erroneous behavior if maxY > height of the graph

// rather than going thru each i (i=i+1) select a subincrement value and increment i by that value

         	cury = originY - (int)(i*pixelincrementY);
 
            if(i % labelincrementY == 0)
            {
            	g.drawLine(originX+5, cury, originX-5,cury);
               	s = "" + (int)(i); 
                  g.drawString(s, originX-fm.stringWidth(s)-5, cury + sw/2);
 
           	}
           	else
            	g.drawLine(originX+2, cury, originX-2, cury);
			}
	
   }


	private void _drawBarGraph(Graphics2D g){
		int numX = dataSource.getIndexCount();
		int numY = dataSource.getValueCount();
		double [] data = new double[numY];
		int x1, x2;
		int y;
		barWidth = (int)(pixelincrementX*(0.75));
		
		for(int i=0; i<numX; i++){
			dataSource.getValues(i, data);
			x1 = originX + (int)(i*pixelincrementX) + (int)(pixelincrementX/2);		// calculate X value
			
			if(BarGraphType == STACKED){
				for(int k=0; k<numY; k++){
					y = (int) (originY - (int)stackArray[i][k]*pixelincrementY);			// calculating lowerbound of box, which StackArray
																											   // allready contains
						
					g.setColor(dataSource.getColor(k));
					g.fillRect(x1-(barWidth/2),y, barWidth, (int)(data[k]*pixelincrementY));	// using data[i] to get the heigh of this bar	
				}
			}
			else if(BarGraphType == UNSTACKED)			// unstacked.. sort the values and then display them
			{
				int maxIndex=0;
				double maxValue=0;
				double [][] temp = new double[numY][2];      // one col to retain color (actual index) information
									// and the other to hold the actual data
				for(int k=0;k<numY;k++)
				{	
					temp[k][0] = k;
					temp[k][1] = data[k];
				}

				for(int k=0; k<numY; k++)
				{
					maxValue = temp[k][1];
					maxIndex = k;

					for(int j=k;j<numY;j++)
					{
						if(temp[j][1]>maxValue)
						{
							maxIndex = j;
							maxValue = temp[j][1];
						}
					}

					int t = (int)temp[k][0];
					double t2 = temp[k][1];

					temp[k][0] = temp[maxIndex][0];
					temp[k][1] = maxValue;		//swap the contents of maxValue with the ith value
					temp[maxIndex][0] = t;
					temp[maxIndex][1] = t2;
				}
				// now display the graph


				for(int k=0; k<numY; k++)
				{
					g.setColor(dataSource.getColor((int)temp[k][0]));
					y = (int)(originY-(int)(temp[k][1]*pixelincrementY));
					g.fillRect(x1-(barWidth/2),y,barWidth,(int)(temp[k][1]*pixelincrementY));
				}

			}
			else							// single.. display average value
			{
				double sum=0;
				for(int j=0; j<numY; j++)
					sum += data[j];
				sum /= numY;
				y = (int) (originY - (int)(sum*pixelincrementY));
				g.fillRect(x1-(barWidth/2),y,barWidth,(int)(sum*pixelincrementY));
			}		
		
		}
	
	}

	
    private void drawBarGraph(Graphics2D g) {
	int xValues = dataSource.getIndexCount();
	int yValues = dataSource.getValueCount();  //no. of y values for each x
	double [] data = new double[yValues];
	int x1,x2;
	int y;
   barWidth  = (int)(pixelincrementX*(0.75)) ;
        //if(barWidth<6) barWidth=6;
          
	for(int i=0; i < xValues; i++)
	{
	   dataSource.getValues(i,data);
	   x1 = originX + (int)(i*pixelincrementX) + (int)(pixelincrementX/2);    //calculate x value
 
           if(BarGraphType == STACKED)
		for(int j=0; j<yValues; j++)
		{
			y = (int) (originY - (int)(data[j]*pixelincrementY));
			for(int k=0; k<j;k++)			// stacked: so subtract all the previous y values
				y -= (int)(data[k]*pixelincrementY);

			g.setColor(dataSource.getColor(j));
			g.fillRect(x1-(barWidth/2),y,barWidth,(int)(data[j]*pixelincrementY));
		}
	   else if(BarGraphType == UNSTACKED)			// unstacked.. sort the values and then display them
	   {
		int maxIndex=0;
		double maxValue=0;
		double [][] temp = new double[yValues][2];      // one col to retain color (actual index) information
								// and the other to hold the actual data
		for(int k=0;k<yValues;k++)
		{	
		   temp[k][0] = k;
		   temp[k][1] = data[k];
		}

		for(int k=0; k<yValues; k++)
		{
		  maxValue = temp[k][1];
		  maxIndex = k;

		  for(int j=k;j<yValues;j++)
		  {
		    if(temp[j][1]>maxValue)
		    {
			maxIndex = j;
			maxValue = temp[j][1];
		    }
		   }
 
		  int t = (int)temp[k][0];
		  double t2 = temp[k][1];

		  temp[k][0] = temp[maxIndex][0];
		  temp[k][1] = maxValue;		//swap the contents of maxValue with the ith value
		  temp[maxIndex][0] = t;
		  temp[maxIndex][1] = t2;
		}
		// now display the graph
	
	
		for(int k=0; k<yValues; k++)
		{
			g.setColor(dataSource.getColor((int)temp[k][0]));
			y = (int)(originY-(int)(temp[k][1]*pixelincrementY));
			g.fillRect(x1-(barWidth/2),y,barWidth,(int)(temp[k][1]*pixelincrementY));
		}

	   }
	   else							// single.. display average value
	   {
		double sum=0;
		for(int j=0; j<yValues; j++)
			sum += data[j];
		sum /= yValues;
		y = (int) (originY - (int)(sum*pixelincrementY));
		g.fillRect(x1-(barWidth/2),y,barWidth,(int)(sum*pixelincrementY));
	   }
	}
   }

    public void drawLineGraph(Graphics2D g) {
	int xValues = dataSource.getIndexCount();
	int yValues = dataSource.getValueCount(); // no. of y values for each x
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

