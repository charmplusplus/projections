package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import projections.misc.*;
import projections.analysis.*;

public class ProfileWindow extends Frame
   implements ActionListener, AdjustmentListener
{ 
   private MainWindow             mainWindow;
//   private TimelineWindow         timelineWindow;
   
   private NoUpdatePanel        mainPanel, displayPanel;
   private Panel                labelCanvas2, titlePanel;
   private ProfileLabelCanvas   labelCanvas;
   private ProfileAxisCanvas    axisCanvas;
   private ProfileDisplayCanvas displayCanvas;
   private ProfileColorWindow   colorWindow;
   private Label lTitle, lTitle2;
   private Scrollbar HSB, VSB;
   private ProfileData data;
   private FloatTextField xScaleField, yScaleField;
   private Button bDecreaseX, bIncreaseX, bResetX;
   private Button bDecreaseY, bIncreaseY, bResetY;
   private Button bColors;
   private Button bPieChart;
   private Color[] colors;
   private ProfileObject[][] poArray;
   private float xscale=1, yscale=1;
   private float[][] avg;
   private float thresh;
   private int avgSize;
   private long begintime, endtime;
   
   private ProfileDialog2 dialog;
   private PieChartWindow pieChartWindow;
   
   class NoUpdatePanel extends Panel
   {
	  public void update(Graphics g)
	  {
		 paint(g);
	  }
   }

// since anyway timelineWindow variable is not being used, removing it
//   public ProfileWindow(MainWindow mainWindow, TimelineWindow timelineWindow)
   public ProfileWindow(MainWindow mainWindow)
   {

	  this.mainWindow = mainWindow;
//	  this.timelineWindow = timelineWindow;
	  
	  addWindowListener(new WindowAdapter()
	  {                    
		 public void windowClosing(WindowEvent e)
		 {
			Close();
		 }
	  });
	  
	  addComponentListener(new ComponentAdapter()
	  {
		 public void componentResized(ComponentEvent e)
		 {
 			if(displayCanvas != null)
			{
			   setCursor(new Cursor(Cursor.WAIT_CURSOR));
			   setSizes();
			   setScales();
			   labelCanvas.makeNewImage();
			   axisCanvas.makeNewImage();
			   labelCanvas2.invalidate();
			   mainPanel.validate();
			   setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}   
		 }
	  });
	  
	  setBackground(Color.lightGray);
	  
	  data = new ProfileData(this);
	  
	  setTitle("Projections Usage Profile");
	  
	  CreateMenus();
	  CreateLayout();
	  
	  pack();
	  setVisible(true);
	  
	  ShowDialog(); 
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  if(evt.getSource() instanceof Button)
	  {
		 setCursor(new Cursor(Cursor.WAIT_CURSOR));
		 
		 Button b = (Button)evt.getSource();
		 
		 if(b == bDecreaseX || b == bIncreaseX || b == bResetX)
		 {
			if(b == bDecreaseX)
			{
			   xscale = (float)((int)(xscale * 4)-1)/4;
			   if(xscale < 1.0)
			   xscale = (float)1.0;
			}
			else if(b == bIncreaseX)
			{
			   xscale = (float)((int)(xscale * 4)+1)/4;
			}
			else if(b == bResetX)
			{
			   xscale = (float)1.0;
			}
			
			xScaleField.setText("" + xscale);
			setScales();
			labelCanvas.makeNewImage();
		 }
		 else if(b == bDecreaseY || b == bIncreaseY || b == bResetY)
		 {
			if(b == bDecreaseY)
			{
			   yscale = (float)((int)(yscale * 4)-1)/4;
			   if(yscale < 1.0)
				  yscale = (float)1.0;
			}
			else if(b == bIncreaseY)
			{
			   yscale = (float)((int)(yscale * 4)+1)/4;
			}
			else if(b == bResetY)
			{
			   yscale = (float)1.0;
			}
			
			yScaleField.setText("" + yscale);
			setScales(); 
			axisCanvas.makeNewImage(); 
		 }
                 else if(b == bPieChart)
                 {
                        System.out.println("bPieChart was clicked");
						pieChartWindow = new PieChartWindow(mainWindow, avg[0], avg[0].length, thresh, colors);			
                 }
		 
		 setCursor(new Cursor(Cursor.DEFAULT_CURSOR));   
	  }
	  else if(evt.getSource() instanceof MenuItem)
	  {
		 String arg = ((MenuItem)evt.getSource()).getLabel();
		 if(arg.equals("Close"))
			Close();
		 else if(arg.equals("Select Processors"))
			ShowDialog();
		 else if(arg.equals("Print Profile"))
			PrintProfile();   
		 /* Useless, should remove.
		 else if(arg.equals("Index"))
			ShowHelpWindow();
		 else if(arg.equals("About"))
			ShowAboutDialog();       
		 */
	  }
	  else if(evt.getSource() instanceof FloatTextField)
	  {
		 setCursor(new Cursor(Cursor.WAIT_CURSOR));
		 FloatTextField ftf = (FloatTextField)evt.getSource();
		 
		 if(ftf == xScaleField)
		 {
			xscale = xScaleField.getValue();
			setScales();
			labelCanvas.makeNewImage();
		 }
		 else if(ftf == yScaleField)
		 {
			yscale = yScaleField.getValue();
			setScales();
			axisCanvas.makeNewImage();
		 } 
		 setCursor(new Cursor(Cursor.DEFAULT_CURSOR));  
	  }                  
   }   
   public void adjustmentValueChanged(AdjustmentEvent evt)
   {
	  Scrollbar sb = (Scrollbar)evt.getSource();
	  displayCanvas.setLocation(-HSB.getValue(), -VSB.getValue());
			
	  if(sb == HSB)
	  {
		 labelCanvas.repaint();
	  }
	  else if(sb == VSB)
	  {
	  axisCanvas.repaint();
	  }  
   }   
   public void CancelDialog()
   {
	  dialog.setVisible(false);
	  dialog.dispose();
	  dialog = null;
   }   
   private void Close()
   {
	  setVisible(false);
	  mainWindow.closeChildWindow(this);
	  dispose();
   }   
   public void CloseDialog()
   {
	  dialog.setVisible(false);
	  dialog.dispose();
	  dialog = null;
   
	  new Thread(new Runnable() {public void run() {
		MakePOArray(data.begintime, data.endtime);
	  }}).start();
   }      
   private void CreateLayout()
   {
	  //// MAIN PANEL
	  
	   mainPanel    = new NoUpdatePanel();
	  displayPanel = new NoUpdatePanel();
	  
	  labelCanvas    = new ProfileLabelCanvas(data);
	  labelCanvas2   = new NoUpdatePanel();
	  axisCanvas     = new ProfileAxisCanvas(data);
	  displayCanvas  = new ProfileDisplayCanvas(data);
	  
	  HSB = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 1);
	  VSB = new Scrollbar(Scrollbar.VERTICAL, 0, 1, 0, 1);
	  
	  mainPanel.setLayout(null);
	  mainPanel.setBackground(Color.black);
	  mainPanel.add(labelCanvas);
	  mainPanel.add(labelCanvas2);
	  mainPanel.add(axisCanvas);
	  mainPanel.add(displayPanel);
	  mainPanel.add(HSB);
	  mainPanel.add(VSB);
	  
	  displayPanel.setLayout(null);
	  displayPanel.add(displayCanvas);
	  
	  HSB.setBackground(Color.lightGray);
	  HSB.addAdjustmentListener(this);
	  
	  VSB.setBackground(Color.lightGray);
	  VSB.addAdjustmentListener(this);
	  
	  //// BUTTON PANEL
	  
	  bColors   = new Button("Change Colors");
	  bDecreaseY   = new Button("<<");
	  bIncreaseY   = new Button(">>");
	  bResetY      = new Button("Reset");
	  bDecreaseX   = new Button("<<");
	  bIncreaseX   = new Button(">>");
	  bResetX      = new Button("Reset");
	  bPieChart    = new Button("Pie Chart");
	  
	  bColors.addActionListener(this);
	  bDecreaseY.addActionListener(this);
	  bIncreaseY.addActionListener(this);
	  bResetY.addActionListener(this);
	  bDecreaseX.addActionListener(this);
	  bIncreaseX.addActionListener(this);
	  bResetX.addActionListener(this);
          bPieChart.addActionListener(this);
	
	   
	  Label lXScale = new Label("X-SCALE: ", Label.CENTER);
	  xScaleField   = new FloatTextField(xscale, 5);
	  xScaleField.addActionListener(this);
	  
	  Label lYScale = new Label("Y-SCALE: ", Label.CENTER);
	  yScaleField   = new FloatTextField(yscale, 5);
	  yScaleField.addActionListener(this);
	 
	  
	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  
	  Panel buttonPanel = new Panel();
	  buttonPanel.setLayout(gbl);
	  
	  Util.gblAdd(buttonPanel, bDecreaseY,   gbc, 0,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, lYScale,      gbc, 1,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, yScaleField,  gbc, 2,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, bIncreaseY,   gbc, 3,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, bResetY,      gbc, 4,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, new Label("  "), gbc, 5,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, bDecreaseX,   gbc, 6,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, lXScale,      gbc, 7,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, xScaleField, gbc, 8,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, bIncreaseX,   gbc, 9,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, bResetX,      gbc, 10,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, bPieChart,    gbc, 11,0, 1,1, 1,1);
	  
	  //// WINDOW
	  
	  Panel yLabelPanel = new Panel();
	  yLabelPanel.setBackground(Color.black);
	  yLabelPanel.setForeground(Color.white);
	  Label yLabel = new Label("%", Label.CENTER);
	  yLabelPanel.setLayout(gbl);
	  Util.gblAdd(yLabelPanel, yLabel, gbc, 0,0, 1,1, 1,1);
	  
	  titlePanel = new Panel();
	  titlePanel.setBackground(Color.black);
	  titlePanel.setForeground(Color.white);
	  lTitle = new Label("", Label.CENTER);
	  lTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
	  lTitle2 = new Label("", Label.CENTER);
	  lTitle2.setFont(new Font("SansSerif", Font.BOLD, 16));
	  titlePanel.setLayout(gbl);
	  Util.gblAdd(titlePanel, lTitle, gbc, 0,0, 1,1, 1,1);
	  Util.gblAdd(titlePanel, lTitle2, gbc, 0,1, 1,1, 1,1);
	   
	  setLayout(gbl);
	  Util.gblAdd(this, yLabelPanel, gbc, 0,0, 1,2, 0,1);
	  Util.gblAdd(this, titlePanel,  gbc, 1,0, 1,1, 1,0);
	  Util.gblAdd(this, mainPanel,   gbc, 1,1, 1,1, 1,1);
	  Util.gblAdd(this, buttonPanel, gbc, 0,2, 2,1, 1,0);
   }   
   private void CreateMenus()
   {
	  MenuBar mbar = new MenuBar();
	  
	  mbar.add(Util.makeMenu("File", new Object[]
	  {
		 "Select Processors",
		 "Print Profile",
		 null,
		 "Close"
	  },
	  this));                   
	  
	  Menu helpMenu = new Menu("Help");
	  mbar.add(Util.makeMenu(helpMenu, new Object[]
	  {
		 "Index",
		 "About"
	  },
	  this)); 
	  
	  mbar.setHelpMenu(helpMenu);
	  setMenuBar(mbar);                                                     
   }   
   public int getHSBValue()
   {
	  return HSB.getValue();
   }   
   public int getVSBValue()
   {
	  return VSB.getValue();
   }   
	public void MakePOArray(long bt, long et)
	{
	    // the reason for 5 extra colors is because they are non-ep
	    // colors?
	    int numEPs = Analysis.getNumUserEntries();
	  colors = new Color[numEPs+5];
	  Color[] newUserColors = new Color[numEPs];

	  for(int i=0; i<numEPs; i++)
	  {
	      // if the data is an entry method whose color is found in
	      // analysis, then use it.
	      colors[i] = Analysis.getEntryColor(i);
	  }   
	  // Idle time is White and is placed at the top
	  // Pack time is black (to see the first division between entry
	  // data and non entry data).
	  // Unpack time is orange (to provide the constrast).
	  colors[numEPs] = Color.black;
	  colors[numEPs+1] = Color.orange;
	  colors[numEPs+2] = Color.white;
		
		displayCanvas.removeAll();

		// extra column is that of the average data.
		data.numPs = data.plist.size()+1;
		poArray = new ProfileObject[data.numPs][];

		int numUserEntries = Analysis.getNumUserEntries();

		int curPe;
		// why +4 now and not +5?
		// the first row is for entry method execution time the second is for 
		//time spent sending messages in that entry method
		
		avg=new float[2][numUserEntries+4];
		double avgScale=1.0/data.plist.size();

		int poNo=1;
		ProgressDialog bar=new ProgressDialog("Computing entry point usage...");

		// Why +2 instead of +1?
		int nEl=data.plist.size()+2;

		// **CW** Hack for colors to work - Profile really should be cleanly rewritten.
		// split the original loop:
		// Phase 1a - compute average work
		// Phase 1b - assign colors based on average work
		// Phase 2 - create profile objects
		/* **** OLD CODE ****
		data.plist.reset();
		while (data.plist.hasMoreElements())
		{
			if (!bar.progress(poNo,nEl,poNo+" of "+nEl))
				break;
			curPe = data.plist.currentElement();
			data.plist.nextElement();
			float cur[]=Analysis.GetUsageData(curPe,bt,et, data.phaselist);
			for (int i=0;i<avg.length && i<cur.length;i++) {
				avg[i]+=(float)(cur[i]*avgScale);
			}
			usage2po(cur,curPe,poNo++,colors);
		}
		bar.progress(poNo,nEl,"averaging");
		*/
		// *CW* *** New code ****
		// Phase 1a
		data.plist.reset();
		for(int i =0;i<avg[0].length;i++){
			avg[0][i] = 0.0f;
			avg[1][i] = 0.0f;
		}
		while (data.plist.hasMoreElements()) {
		    if (!bar.progress(poNo,nEl,poNo+" of "+nEl))
			break;
		    curPe = data.plist.currentElement();
		    data.plist.nextElement();
		// the first row is for entry method execution time the second is for 
		//time spent sending messages in that entry method
		    float cur[][]=Analysis.GetUsageData(curPe,bt,et,data.phaselist);
		    for (int i=0;i<avg[0].length && i<cur[0].length;i++) {
			avg[0][i]+=(float)(cur[0][i]*avgScale);
			avg[1][i]+=(float)(cur[1][i]*avgScale);
		    }		    
		}
		// Phase 1b
		Vector sigElements = new Vector();
		// we only wish to compute for EPs
		for (int i=0; i<numEPs; i++) {
		    // anything greater than 5% is "significant"
		    if (avg[0][i]+avg[1][i] > 1.0) {
			sigElements.add(new Integer(i));
		    }
		}
		// copy to an array for Color assignment (maybe that should be
		// a new interface for color assignment).
		int sigIndices[] = new int[sigElements.size()];
		for (int i=0; i<sigIndices.length; i++) {
		    sigIndices[i] = ((Integer)sigElements.elementAt(i)).intValue();
		}
		newUserColors = Analysis.createColorMap(numEPs, sigIndices);
		// copy the new Colors into our color array. (and let the system colors
		// remain as they are)
		for (int i=0; i<newUserColors.length; i++) {
		    colors[i] = newUserColors[i];
		}

		// Phase 2
		data.plist.reset();
		while (data.plist.hasMoreElements())
		{
		    curPe = data.plist.currentElement();
		    data.plist.nextElement();
		    float cur[][]=Analysis.GetUsageData(curPe,bt,et,data.phaselist);
		    usage2po(cur,curPe,poNo++,colors);
		}
		usage2po(avg,-1,0,colors);
		bar.done();

		String sTitle = "Profile of Usage for Processor";
		if(data.plist.size() > 1)
			sTitle += "s";

		sTitle += " " + data.plist.listToString();
		lTitle.setText(sTitle);
		lTitle.invalidate();

		sTitle = "(Time " + U.t(bt) + " - " + U.t(et) + ")";
		lTitle2.setText(sTitle);
		lTitle2.invalidate();
	
		titlePanel.validate();

		setSizes();
		setScales();
		labelCanvas.makeNewImage();
		axisCanvas.makeNewImage();
		labelCanvas2.invalidate();
		mainPanel.validate();      		
	}
   private void PrintProfile()
   {
	   PrintJob pjob = getToolkit().getPrintJob(this, "Print Profile", null);
	  
	  if(pjob == null)
		 return;
		 
	  Dimension d = pjob.getPageDimension();
		  
	  int marginLeft;
	  int marginTop;
	  if(d.width < d.height)
	  {  
		 marginLeft = (int)(0.6 * d.width / 8.5);    
		 marginTop  = (int)(0.6 * d.height / 11.0);
	  }
	  else
	  {
		 marginLeft = (int)(0.6 * d.width / 11.0);
		 marginTop  = (int)(0.6 * d.height / 8.5);
	  }      
	  
	  int printWidth  = d.width  - 2*marginLeft;
	  int printHeight = d.height - 2*marginTop;
	  
	  
	  // Determine what processor range we're going to print
	  int hsbval = HSB.getValue();
	  
	  float pwidth = (float)(data.dcw - 2*data.offset)/data.numPs;

	  int minp = (int)Math.ceil((hsbval - data.offset - 0.5*pwidth)/pwidth);
	  if(minp < 0) minp = 0;
	  
	  int maxp = (int)Math.floor((hsbval + data.vpw - data.offset - 0.5*pwidth)/pwidth);
	  if(maxp > data.numPs-1) maxp = data.numPs-1;
	  if(minp > maxp) minp = maxp;
			
	  System.out.println("Want to print from Processor " + minp + " to Processor " + maxp);
	  
	  
	  // Determine what percentage range we're going to print
	  int vsbval = VSB.getValue();
	  
	  float yheight = (float)((data.dch - data.offset) / 100.0);
	  
	  int maxy = 100 - (int)Math.floor((vsbval - data.offset)/yheight);
	  if(maxy > 100) maxy = 100;
	  
	  int miny = 100 - (int)Math.ceil((vsbval + data.vph - data.offset)/yheight);
	  if(miny < 0) miny = 0;
	  
	  System.out.println("Want to print from " + miny + "% to " + maxy + "%");
	  
	  
	  // Get our first page
	  Graphics pg = pjob.getGraphics();
	  pg.setColor(Color.white);
	  pg.fillRect(0, 0, d.width, d.height);
	  
	  Font normalfont = new Font("SansSerif", Font.PLAIN, 10);
	  pg.setFont(normalfont);
	  
	  pg.translate(marginLeft, marginTop);
	  FontMetrics pfm = pg.getFontMetrics(normalfont);
	  
		   
	  // Figure out the scales to print to the page.
	  
	  int textheight = pfm.getHeight();
	  int axiswidth = pfm.stringWidth("100%") + 20;
	  int labelheight = 2*textheight + 15;
	  int titleheight = 3*textheight + 15;
	  
	  int canvaswidth = printWidth - axiswidth;
	  int canvasheight = printHeight - labelheight - titleheight; 

	  int curheight = 0;
	  
	  // DRAW THE TITLE     
	  String title = "PROJECTIONS USAGE PROFILE FOR " + Analysis.getFilename();
	  pg.setColor(Color.black);
	  curheight += textheight;
	  pg.drawString(title, (printWidth - pfm.stringWidth(title))/2, curheight);
	  
	  title = "Profile of Usage for Processor";
	  if(data.plist.size() > 1)
		 title += "s";
	  title += " " + data.plist.listToString();
	  curheight += 5 + textheight;
	  pg.drawString(title, (printWidth - pfm.stringWidth(title))/2, curheight);
	  
	  title = "(Time " + U.t(data.begintime) + " - " + U.t(data.endtime);
	  curheight += 5 + textheight;
	  pg.drawString(title, (printWidth - pfm.stringWidth(title))/2, curheight);
	  
	  // DRAW THE PROCESSOR LABELS
	  curheight = canvasheight + titleheight;
	  
	  int numprocs = maxp - minp + 1;
	  float width = ((float)canvaswidth/numprocs);

	  int maxwidth = pfm.stringWidth("" + Analysis.getNumProcessors()) + 20; 
	  

	  int labelincrement = (int)(Math.ceil((double)maxwidth/width));
	  labelincrement = Util.getBestIncrement(labelincrement);
	  int longlineht  = 10;
	  int shortlineht = 5;

	  data.plist.reset();
	  for(int p=0; p<minp; p++)
		 data.plist.nextElement();
		 
	  for(int p=0; p<numprocs; p++)
	  {   
		 String tmp = "" + data.plist.nextElement();
		 int xloc = axiswidth + (int)((p+0.5)*width);
		 if((p % labelincrement) == 0)
		 {
			pg.drawLine(xloc, curheight, xloc, curheight + longlineht);
			xloc -= (int)(0.5 * pfm.stringWidth(tmp));
			pg.drawString(tmp, xloc, curheight + longlineht + textheight + 5);
		 }
		 else
		 {
			pg.drawLine(xloc, curheight, xloc, curheight + shortlineht);
		 }      
	  }
	  
	  curheight += longlineht + textheight*2 + 10;
	  String tmp = "Processor Number";
	  pg.drawString(tmp, (canvaswidth - pfm.stringWidth(tmp))/2 + axiswidth, curheight);
	  
	  // DRAW THE PERCENTAGE AXIS AND LABELS

	  pg.drawLine(axiswidth, titleheight, axiswidth, titleheight + canvasheight);
	 
	  float deltay = ((float)canvasheight / (maxy - miny));
	  labelincrement = (int)(Math.ceil(textheight / deltay));
	  labelincrement = Util.getBestIncrement(labelincrement);
	  
	  for(int y=0; y<maxy-miny+1; y++)
	  {
		 curheight = titleheight + canvasheight - (int)(y * deltay); 
		 
		 if((y+miny) % labelincrement == 0)
		 {  
			tmp = "" + (y+miny);
			pg.drawLine(axiswidth-10, curheight, axiswidth, curheight);
			curheight += (int)(0.5*textheight); 
			pg.drawString(tmp, axiswidth-15-pfm.stringWidth(tmp), curheight);
		 }
		 else
		 {
			pg.drawLine(axiswidth-7, curheight, axiswidth, curheight);
		 }
	  }
	  
	  curheight = (canvasheight + textheight)/2 + titleheight;
	  tmp = "%";
	  pg.drawString(tmp, 0, curheight);
	  
	  
	  // DRAW THE BARS
	  
	  int curx, cury;
	  for(int p=minp; p<=maxp; p++)
	  {
		 curx = axiswidth + (int)(p*width) + (int)(0.125 * width);
		 cury = titleheight + canvasheight;
		 
		 float usage = 0;
		 int item = -1;
		 while(usage < miny)
		 {
			item++;
			usage += poArray[p][item].getUsage();
		 }   
		 
		 float firstusage = 0;
		 if(item == -1)
		 {
			item = 0;
			firstusage = poArray[p][item].getUsage();
		 }   
		 else
		 {
			firstusage = usage - miny;
			usage = miny;
		 }
		 
		 
		 double rem = 0.0;
		 int w = (int)(0.75 * width);
		 for(int i=item; i<poArray[p].length; i++)
		 {
			String name = poArray[p][i].getName();
			Color c;
			if(name.equals("MESSAGE PACKING"))
			   c = Color.pink;
			else if(name.equals("OVERHEAD"))
			   c = Color.black;
			else if(name.equals("IDLE"))
			   c = Color.black;
			else         
			   c = poArray[p][i].getForeground();
			
			pg.setColor(c);
					   
			float curusage;           
			if(firstusage >= 0)
			{
			   curusage = firstusage;
			   firstusage = -1;
			}   
			else
			   curusage = poArray[p][i].getUsage();
			
			usage += curusage;
			if(usage > maxy)
			   curusage = maxy - usage + curusage;     
			   
			double objhtD = (deltay*curusage);
			
			objhtD += rem;
				  
			int   objht;
			if(objhtD - (int)objhtD >= 0.5)
			   objht = (int)objhtD + 1;
			else
			   objht = (int)objhtD;
			rem = objhtD - objht;        
				  
			cury -= objht;
	  
			Rectangle r = pg.getClipBounds();
			pg.setClip(curx, cury, w, objht);
	  
			pg.fillRect(curx, cury, w, objht);
	  
			if(name.equals("OVERHEAD"))
			{
			   pg.setColor(Color.white);
			   for(int n=0; n<objht + w; n+=4)
			   {
				  pg.drawLine(curx+n, cury, curx, cury+n);
				  pg.drawLine(curx+n+1, cury, curx, cury+n+1);
			   }
			}
			pg.setClip(r);
			
			if(usage > maxy) break;
		 }   
	  }
	  
	  // PRINT THE PAGE    
	  pg.dispose();   
	  pjob.end();
   }   
   private void setScales()
   {
	   data.dcw = (int)(xscale * data.vpw);
	  data.dch = (int)(yscale * data.vph);
	  
	  if(xscale > 1)
		 HSB.setVisible(true);
	  else
		 HSB.setVisible(false);
	  
	  if(yscale > 1)
		 VSB.setVisible(true);
	  else
		 VSB.setVisible(false);
		 
	  HSB.setMaximum(data.dcw);
	  VSB.setMaximum(data.dch);  
	  HSB.setVisibleAmount(data.vpw);
	  VSB.setVisibleAmount(data.vph); 
	  HSB.setBlockIncrement(data.vpw);
	  VSB.setBlockIncrement(data.vph);
	  
	  displayCanvas.setBounds(0, 0, data.dcw, data.dch);

	  double hscale = (double)(data.dch - data.offset)/100;
	  float width   = (float)(data.dcw - 2*data.offset)/data.numPs;
	  int w = (int)(Math.ceil(0.75*width));
	  int ow = (int)((width - w)/2);
	  
	  if(poArray != null)
	  {
		 for(int p=0; p<data.numPs; p++)
		 {
			int h = data.dch;
			double rem = 0.0;
			if(poArray[p] != null)
			{
			   for(int i=0; i<poArray[p].length; i++)
			   {
				  if(poArray[p][i] != null)
				  {
					 double objhtD = (hscale*poArray[p][i].getUsage());
					 objhtD += rem;
				  
					 int   objht;
					 if(objhtD - (int)objhtD >= 0.5)
						objht = (int)objhtD + 1;
					 else
						objht = (int)objhtD;
					 rem = objhtD - objht;      
				
					 poArray[p][i].setBounds((int)(width*p+data.offset+ow), h - objht, w, objht);   
				  
					 h -= objht;
				  }
				  else
				  {
					 System.out.println("POARRAY[" + p + "][" + i + "} IS NULL");
				  }      
			   }
			}
		 }
	  } 
	  
	  displayCanvas.makeNewImage();           
   }   
   private void setSizes()
   {
	  int acw, lch, sbh, sbw, mpw, mph, lch2;
	  
	  mpw = mainPanel.getSize().width;
	  mph = mainPanel.getSize().height;
	  
	  acw  = 50;
	  lch  = 30;
	  lch2 = 30;
	  sbh  = 20;
	  sbw  = 20;
	  
	  data.vpw = mpw - acw - sbw;
	  data.vph = mph - lch - lch2 - sbh;
	  
	  data.dcw = (int)(xscale * data.vpw);
	  data.dch = (int)(yscale * data.vph);
	  
	  if(xscale > 1)
		 HSB.setVisible(true);
	  else
		 HSB.setVisible(false);
	  
	  if(yscale > 1)
		 VSB.setVisible(true);
	  else
		 VSB.setVisible(false);
		 
	  HSB.setMaximum(data.dcw);
	  VSB.setMaximum(data.dch);   
	  HSB.setBlockIncrement(data.vpw);
	  VSB.setBlockIncrement(data.vph);            
	  
	  axisCanvas.setBounds   (0,       0,       acw, data.vph);
	  displayPanel.setBounds (acw,     0,       data.vpw, data.vph);
	  displayCanvas.setBounds(0, 0, data.dcw, data.dch);
	  labelCanvas.setBounds  (acw,     data.vph,     data.vpw, lch);
	  labelCanvas2.setBounds (acw, data.vph+lch,     data.vpw, lch2);
	  
	  VSB.setBounds          (mpw-sbw, 0,       sbw, data.vph);
	  HSB.setBounds          (acw,     mph-sbh, data.vpw, sbh);
   }   
    /* deprecated. To be removed.
   private void ShowAboutDialog()
   {
	  mainWindow.ShowAboutDialog((Frame)this);   
   }  
    */ 
   private void ShowDialog()
   {
	  if(dialog == null)
		 dialog = new ProfileDialog2(this, data);
	  dialog.setVisible(true);

   }   

    /* deprecated. To be removed.
   private void ShowHelpWindow()
   {
	  mainWindow.ShowHelpWindow();   
   }   
    */
	//Convert a usage profile (0..numUserEntries+4-1) to a po
	private void usage2po(float usg[][],int curPe,int poNo,Color[] colors)
	{
		int numUserEntries = Analysis.getNumUserEntries();
		String[][] names = Analysis.getUserEntryNames();
		int i,poindex=0,poLen=0;

		float thresh=0.01f;//Percent below which to ignore
		for(i=0;i<usg[0].length;i++){
			if (usg[0][i]>thresh) 
				poLen++;
			if (usg[1][i]>thresh) 
				poLen++;
			
		}
		//Drawing the entry point execution time
		poArray[poNo]=new ProfileObject[poLen];
		for(i=0; i<usg[0].length; i++)
		{
			float usage = usg[0][i];
			if (usage<=thresh) continue; //Skip this one-- it's tiny
			int   entry = i;
			String name;
			if(entry < numUserEntries)
				name = names[entry][1] + "::" + names[entry][0];
			else if(entry == numUserEntries+2)
				name = "IDLE";
			else if(entry == numUserEntries)
				name = "PACKING";
			else if(entry == numUserEntries+1)
				name = "UNPACKING";
			else 
				break;
			poArray[poNo][poindex] = new ProfileObject(usage, name, curPe);
			displayCanvas.add(poArray[poNo][poindex]);
			poArray[poNo][poindex].setForeground(colors[entry]);
			poindex++;
		}
		if(Analysis.getVersion() > 4.9){
		//Drawing the entry point message sendTime
		for(i=0; i<usg[1].length; i++)
		{
			float usage = usg[1][i];
			if (usage<=thresh) continue; //Skip this one-- it's tiny
			int   entry = i;
			String name;
			if(entry < numUserEntries)
				name = "Message Send Time: "+names[entry][1] + "::" + names[entry][0];
			else if(entry == numUserEntries+2)
				name = "Message Send Time: "+"IDLE";
			else if(entry == numUserEntries)
				name = "Message Send Time: "+"PACKING";
			else if(entry == numUserEntries+1)
				name = "Message Send Time: "+"UNPACKING";
			else 
				break;
			poArray[poNo][poindex] = new ProfileObject(usage, name, curPe);
			displayCanvas.add(poArray[poNo][poindex]);
			poArray[poNo][poindex].setForeground(colors[entry]);
			poindex++;
		}
		}
	}
}
