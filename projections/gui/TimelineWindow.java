package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.io.*;
import java.util.*;

import java.text.*;

public class TimelineWindow extends Frame
   implements ActionListener, AdjustmentListener, ItemListener
{ 
   MainWindow            mainWindow;

   private NoUpdatePanel         mainPanel, displayPanel;
   private TimelineLabelCanvas   labelCanvas;
   private TimelineAxisCanvas    axisTopCanvas, axisBotCanvas;
   public TimelineDisplayCanvas displayCanvas;
   private TimelineRangeDialog   rangeDialog;
   private TimelineColorWindow   colorWindow;
   
   private Scrollbar HSB, VSB;
   
   private TimelineData data;
   
   private FontMetrics fm;
   
  // basic zoom controls  
   private Button bSelectRange, bColors, bDecrease, bIncrease, bReset;
   private Button bZoomSelected, bLoadSelected;
   // jump to graphs
   private Button bJumpAnimation, bJumpProfile, bJumpGraph, bJumpHistogram,
   		  bJumpComm, bJumpStl;
   private TextField highlightTime, selectionBeginTime, selectionEndTime, selectionDiff;
   private DecimalFormat format;
   private FloatTextField scaleField;
   private Checkbox cbPacks, cbMsgs, cbIdle, cbUser;
//   public Checkbox colorbyObjectId;	
   
   private int maxLabelLen;
   private long oldEndTime;
   
   private TimelineMessageWindow messageWindow;

  private UserEventWindow userEventWindow;
  // process MouseEvents here
   private AxisMouseController mouseController;
  private class AxisMouseController {
    public MouseMotionAdapter mouseMotionAdapter = null;
    public MouseListener mouseListener = null;
    private TimelineDisplayCanvas canvas_;
    private TimelineAxisCanvas timeline_;
    private TimelineWindow window_;

    public boolean selected_ = false;
    AxisMouseController(
      TimelineWindow window, TimelineDisplayCanvas canvas, TimelineAxisCanvas timeline) 
    {
      window_ = window;
      canvas_ = canvas;
      timeline_ = timeline;
      mouseMotionAdapter = new MouseMotionAdapter() {
	public void mouseDragged(MouseEvent e) {
	  Point p = timeline_.screenToCanvas(e.getPoint());
	  canvas_.rubberBand.stretch(p);
	  window_.setHighlightTime(timeline_.canvasToTime(p.x));
	  canvas_.repaint();
	}
	public void mouseMoved(MouseEvent e) {
	  Point p = timeline_.screenToCanvas(e.getPoint());
	  canvas_.rubberBand.highlight(p);
	  window_.setHighlightTime(timeline_.canvasToTime(p.x));
	  canvas_.repaint();
	}
      };
      mouseListener = new MouseListener() {
	public void mouseClicked(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { 
	  canvas_.rubberBand.clearHighlight();
	  window_.unsetHighlightTime();
	  canvas_.rubberBand.anchor(timeline_.screenToCanvas(e.getPoint()));
	  canvas_.repaint();
	}
	public void mouseReleased(MouseEvent e) { 
	  canvas_.rubberBand.stretch(timeline_.screenToCanvas(e.getPoint()));
	  // canvas_.rubberBand.end(timeline_.screenToCanvas(e.getPoint()));
	  canvas_.repaint();
	  selected_ = true;
	  Rectangle rect = canvas_.rubberBand.bounds();
	  double startTime = timeline_.canvasToTime(rect.x);
	  double endTime = timeline_.canvasToTime(rect.x+rect.width);
	  window_.setSelectedTime(startTime, endTime);
	}
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { 
	  canvas_.rubberBand.clearHighlight();
	  unsetHighlightTime();
	  canvas_.repaint();
	}
      };
    }
  };

  class NoUpdatePanel extends Panel
  {
    public void update(Graphics g) { paint(g); }
  }      
   public TimelineWindow(MainWindow mainWindow)
   {
	  this.mainWindow = mainWindow;
	  
	  format = new DecimalFormat();
	  format.setGroupingUsed(true);
	  format.setMinimumFractionDigits(0);
	  format.setMaximumFractionDigits(0);
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
			if(displayCanvas != null && axisTopCanvas != null && axisBotCanvas != null)
			{
			   setCursor(new Cursor(Cursor.WAIT_CURSOR));
			   setAllSizes(false);
			   displayCanvas.makeNewImage();
			   axisTopCanvas.makeNewImage();
			   axisBotCanvas.makeNewImage();
			   setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}   
		 }
	  });
	  
	  setBackground(Color.lightGray);
	  
	  data = new TimelineData(this);
	  
	  setTitle("Projections Timeline");
	  CreateMenus();
	  CreateLayout();
	  pack();
	  ShowRangeDialog();
   }   

  private double calcLeftTime(
    double leftTime, double rightTime, double oldScale, double newScale) 
  {
    double timeShowing = (rightTime-leftTime)*oldScale/newScale;
    double timeDiff = Math.abs((rightTime-leftTime)-timeShowing);
    if (oldScale > newScale) { return leftTime - timeDiff/2; }
    else { return leftTime + timeDiff/2; }
  }

  public void setHighlightTime(double time) { 
    // System.out.println(time);
    highlightTime.setText(format.format(time)); 
  }
  public void unsetHighlightTime() { highlightTime.setText(""); }
  public void setSelectedTime(double time1, double time2) {
    // System.out.println(time1+" "+time2);
    selectionBeginTime.setText(format.format(time1));
    selectionEndTime.setText(format.format(time2));
    format.setMinimumFractionDigits(3);
    format.setMaximumFractionDigits(3);
    selectionDiff.setText(format.format((time2-time1)/1000)+" ms");
    format.setMinimumFractionDigits(0);
    format.setMaximumFractionDigits(0);
  }
  public void unsetSelectedTime() {
    selectionBeginTime.setText("");
    selectionEndTime.setText("");
    selectionDiff.setText("");
  }

  public void setZoom(double startTime, double endTime) {
    data.scale = 
      (float) ((data.endTime-data.beginTime)/(endTime-startTime));
    scaleField.setText("" + data.scale);
    setCursor(new Cursor(Cursor.WAIT_CURSOR));
    setAllSizes(true);
    displayCanvas.makeNewImage();
    axisTopCanvas.makeNewImage();
    axisBotCanvas.makeNewImage();
    if (data.scale != 1.0) {
      HSB.setValue(axisBotCanvas.calcHSBOffset(startTime));
      displayCanvas.setLocation(-HSB.getValue(), -VSB.getValue());
    }
    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
  }

  public void zoomSelected() { 
    if (mouseController.selected_) {
      Rectangle rect = displayCanvas.rubberBand.bounds();
      mouseController.selected_ = false;
      unsetSelectedTime();
      if (rect.width == 0) { return; }
      double currentZoomStart = axisBotCanvas.canvasToTime(rect.x);
      double currentZoomEnd = axisBotCanvas.canvasToTime(rect.x+rect.width);
      setZoom(currentZoomStart, currentZoomEnd);
    }
  }

  public void loadSelected() { 
    if (mouseController.selected_) {
      Rectangle rect = displayCanvas.rubberBand.bounds();
      mouseController.selected_ = false;
      unsetSelectedTime();
      if (rect.width == 0) { return; }
      double startTime = axisBotCanvas.canvasToTime(rect.x);
      double endTime = axisBotCanvas.canvasToTime(rect.x+rect.width);
      if (startTime < data.beginTime) { startTime = data.beginTime; }
      if (endTime > data.endTime) { endTime = data.endTime; }
      data.oldBT = data.beginTime;
      data.oldET = data.endTime;
      data.beginTime = (long)(startTime+0.5);
      data.endTime = (long)(endTime+0.5);
      data.scale = (float) 1.0;
      scaleField.setText(""+1.0);
      if (data.processorList == null) { data.oldplist = null; }
      else { data.oldplist = data.processorList.copyOf(); }
      procRangeDialog(true);
    }
  }

   public void jumpToGraph(String b) {
     Rectangle rect = displayCanvas.rubberBand.bounds();
     double jStart = axisBotCanvas.canvasToTime(rect.x);
     double jEnd = axisBotCanvas.canvasToTime(rect.x+rect.width);
      	       
     Analysis.setJTimeAvailable(true);
     if (rect.width == 0) {
	Analysis.setJTime((long)(0), Analysis.getTotalTime());
     } else Analysis.setJTime((long)(jStart+0.5), (long)(jEnd+0.5));
	
     if (b == "Animation") {
     	AnimationWindow animationWindow = new AnimationWindow();
     } else if (b == "Profile") {
     	ProfileWindow profileWindow = new ProfileWindow(new MainWindow());
     } else if (b == "Graph") {
     	GraphWindow graphWindow = new GraphWindow(new MainWindow());
     } else if (b == "Histogram") {
     	HistogramWindow histogramWindow = new HistogramWindow(new MainWindow());
     } else if (b == "Comm") {
     	CommWindow commWindow = new CommWindow();
     } else if (b == "Stl") {
     	StlWindow stlWindow = new StlWindow();
     }
		
     	     
	
    
     
     
   }
   
   public void actionPerformed(ActionEvent evt)
   {
     if (evt.getSource() instanceof Button) {
       Button b = (Button)evt.getSource();
       if (b == bSelectRange)        { ShowRangeDialog(); }
       else if (b == bColors)        { ShowColorWindow(); }
       else if (b == bZoomSelected)  { zoomSelected(); }
       else if (b == bLoadSelected) { loadSelected(); }
       else if(b == bJumpAnimation) { jumpToGraph("Animation"); }
       else if(b == bJumpProfile)   { jumpToGraph("Profile"); }
       else if(b == bJumpGraph)     { jumpToGraph("Graph"); }
       else if(b == bJumpHistogram) { jumpToGraph("Histogram"); }
       else if(b == bJumpComm)      { jumpToGraph("Comm"); }
       else if(b == bJumpStl)       { jumpToGraph("Stl"); }
       else {
	 int leftVal = HSB.getValue();
	 int rightVal = leftVal + data.vpw;
	 double leftTime = axisBotCanvas.canvasToTime(leftVal);
	 double rightTime = axisBotCanvas.canvasToTime(rightVal);
	 double oldScale = data.scale;

	 if (b == bDecrease) {
	   data.scale = (float)((int)(data.scale * 4)-1)/4;
	   if (data.scale < 1.0) { data.scale = (float)1.0; }
	 }
	 else if (b == bIncrease) {
	   data.scale = (float)((int)(data.scale * 4)+1)/4;
	 }
	 else if (b == bReset) { data.scale = (float)1.0; }
	 scaleField.setText("" + data.scale);
	 double newLeftTime = 
	   calcLeftTime(leftTime, rightTime, oldScale, data.scale);
	 
	 setCursor(new Cursor(Cursor.WAIT_CURSOR));
	 // setTLSizes();
	 // setScales();
	 // setTLBounds();
	 setAllSizes(false);
	 displayCanvas.makeNewImage();
	 axisTopCanvas.makeNewImage();
	 axisBotCanvas.makeNewImage();
	 if (data.scale != 1.0) {
	   HSB.setValue(axisBotCanvas.calcHSBOffset(newLeftTime));
	   displayCanvas.setLocation(-HSB.getValue(), -VSB.getValue());
	 }
	 setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
       }   
     }
     else if(evt.getSource() instanceof MenuItem) {
       String arg = ((MenuItem)evt.getSource()).getLabel();
       if(arg.equals("Close")) Close();
       else if(arg.equals("Modify Ranges")) ShowRangeDialog();
       else if(arg.equals("Print Timeline")) PrintTimeline();   
       /* Useless. To be removed.
       else if(arg.equals("Index")) mainWindow.ShowHelpWindow();
       else if(arg.equals("About")) mainWindow.ShowAboutDialog((Frame) this);
       */
       else if(arg.equals("Change Colors")) { ShowColorWindow(); }
       // **** sharon **
       else if(arg.equals("Save Colors")) {
	   saveColorFile();
	   	   
	   /*
	   try {
	       Util.saveColors(data.entryColor, "Timeline Graph");
	   } catch (IOException e) {
	       System.err.println("Attempt to write to color.map failed");
	   }
	   */
       }
       else if(arg.equals("Restore Colors")) {
	   openColorFile();
	   
	   /*
	   try {
	       Util.restoreColors(data.entryColor, "Timeline Graph");
	   } catch (IOException e) {
	       System.err.println("Attempt to read from color.map failed");
	   } 
	   */
	   data.displayCanvas.updateColors();
	   
       }
       else if (arg.equals("Default Colors")) {
	   for (int i=0; i<data.entryColor.length; i++) {
	       data.entryColor[i] = Analysis.getEntryColor(i);
	   }
	   data.displayCanvas.updateColors();
       }
     }
     else {
       int leftVal = HSB.getValue();
       int rightVal = leftVal + data.vpw;
       double leftTime = axisBotCanvas.canvasToTime(leftVal);
       double rightTime = axisBotCanvas.canvasToTime(rightVal);
       double oldScale = data.scale;
       data.scale = scaleField.getValue();
       if (data.scale < 1.0) { data.scale = (float)1.0; }
       double newLeftTime = 
	 calcLeftTime(leftTime, rightTime, oldScale, data.scale);
	 
       setCursor(new Cursor(Cursor.WAIT_CURSOR));
       setAllSizes(true);
       displayCanvas.makeNewImage();
       axisTopCanvas.makeNewImage();
       axisBotCanvas.makeNewImage();
       if (data.scale != 1.0) {
	 HSB.setValue(axisBotCanvas.calcHSBOffset(newLeftTime));
	 displayCanvas.setLocation(-HSB.getValue(), -VSB.getValue());
       }
       setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
     }                  
   }   
   
   //  **sharon**
   public void openColorFile()
   {
   	JFileChooser d = new JFileChooser(System.getProperty("user.dir"));
	d.setFileFilter(new ColorFileFilter());
	int returnVal = d.showOpenDialog(this);
	if(returnVal == JFileChooser.APPROVE_OPTION) {
		try {
	       		Util.restoreColors(data.entryColor, "Timeline Graph",
				d.getSelectedFile().getAbsolutePath());
	        } catch (IOException e) {
	       		System.err.println("Attempt to read from color.map failed");
	   	} 
	}
   }
   
   
  //  **sharon** change to .map files
  public void saveColorFile()
  {
  	JFileChooser d = new JFileChooser(System.getProperty("user.dir"));
	d.setFileFilter(new ColorFileFilter());
	int returnVal = d.showSaveDialog(this);
	if(returnVal == JFileChooser.APPROVE_OPTION) {
		try {
	        	Util.saveColors(data.entryColor, "Timeline Graph",
					d.getSelectedFile().getAbsolutePath());
	   	} catch (IOException e) {
	       		System.err.println("Attempt to write to color.map failed");
	   	}
	}
  }
   
   public void adjustmentValueChanged(AdjustmentEvent evt)
   {
	  Scrollbar sb = (Scrollbar)evt.getSource();
	  displayCanvas.setLocation(-HSB.getValue(), -VSB.getValue());
			
	  if(sb == HSB)
	  {
		 axisTopCanvas.repaint();
		 axisBotCanvas.repaint();
	  }
	  else if(sb == VSB)
	  {
		 labelCanvas.repaint();
	  }  
   }   
   private void Close()
   {
	  if(colorWindow != null)
	  {
		 colorWindow.dispose();
		 colorWindow = null;
	  }   
	  setVisible(false);
	  mainWindow.closeChildWindow(this);
	  dispose();
   }   
   public void CloseColorWindow()
   {
	  colorWindow = null;
   }   
   public void CloseRangeDialog()
   {
	  rangeDialog.dispose();
	  rangeDialog = null;

	  if(data.beginTime != data.oldBT || data.endTime != data.oldET ||
	     (data.processorList != null && 
	      !data.processorList.equals(data.oldplist)))
	  {
	    procRangeDialog(true);
	  }
	  setVisible(true);
	  
   }
	


  public void procRangeDialog(boolean keeplines) {
		//keeplines describes if the lines from message creation
		// to execution are to be retained or not.
    setCursor(new Cursor(Cursor.WAIT_CURSOR));
    data.tlh = data.tluh * data.numPs;
    VSB.setMaximum(data.tlh);
    displayCanvas.removeAll();
    data.createTLOArray();
    for(int p=0; p<data.numPs; p++)
      for(int i=0; i<data.tloArray[p].length; i++)
	displayCanvas.add(data.tloArray[p][i]);    
    if (data.userEventsArray != null)
      for(int p=0; p<data.numPs; p++)
	if (data.userEventsArray[p] != null)
	  for(int i=0; i<data.userEventsArray[p].length; i++)
	    displayCanvas.add(data.userEventsArray[p][i]);    
    
    setAllSizes(keeplines);
    labelCanvas.makeNewImage();
    axisTopCanvas.makeNewImage();
    axisBotCanvas.makeNewImage();
    displayCanvas.makeNewImage();
    cbUser.setLabel("View User Events ("+data.getNumUserEvents()+")");
    if (userEventWindow == null) {
      userEventWindow = new UserEventWindow(cbUser);
    }
    userEventWindow.setData(data);
    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
  }   
  
   private void CreateLayout()
   {
	  //// MAIN PANEL
	  
	  mainPanel    = new NoUpdatePanel();
	  displayPanel = new NoUpdatePanel();
	  
	  labelCanvas   = new TimelineLabelCanvas(data);
	  axisTopCanvas = new TimelineAxisCanvas(data, "top");
	  axisBotCanvas = new TimelineAxisCanvas(data, "bot");
	  displayCanvas = new TimelineDisplayCanvas(data); 
	  data.displayCanvas = displayCanvas;   
	  
	  mouseController = new AxisMouseController(this, displayCanvas, axisBotCanvas);

	  axisTopCanvas.addMouseListener(mouseController.mouseListener);
	  axisTopCanvas.addMouseMotionListener(
	    mouseController.mouseMotionAdapter);
	  
	  axisBotCanvas.addMouseListener(mouseController.mouseListener);
	  axisBotCanvas.addMouseMotionListener(
	    mouseController.mouseMotionAdapter);
	  
	  HSB = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 1);
	  VSB = new Scrollbar(Scrollbar.VERTICAL, 0, 1, 0, 1);

	  
	  mainPanel.setLayout(null);
	  mainPanel.setBackground(Color.black);
	  mainPanel.add(labelCanvas);
	  mainPanel.add(axisTopCanvas);
	  mainPanel.add(axisBotCanvas);
	  mainPanel.add(displayPanel);
	  mainPanel.add(HSB);
	  mainPanel.add(VSB);

	  displayPanel.setLayout(null);
	  displayPanel.add(displayCanvas);
	  
	  HSB.setBackground(Color.lightGray);
	  HSB.addAdjustmentListener(this);
	  
	  VSB.setBackground(Color.lightGray);
	  VSB.addAdjustmentListener(this);
	  
	  //// CHECKBOX PANEL
	  
	  cbPacks = new Checkbox("Display Pack Times", data.showPacks);
	  cbMsgs  = new Checkbox("Display Message Sends", data.showMsgs);
	  cbIdle  = new Checkbox("Display Idle Time", data.showIdle);
	  cbUser  = new Checkbox("Display User Event Window", false);
	  //colorbyObjectId = new Checkbox("Color Entry methods by Object ID",false);

	  
	  cbPacks.addItemListener(this);
	  cbMsgs.addItemListener(this);
	  cbIdle.addItemListener(this);
	  cbUser.addItemListener(this);
	  

	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  gbc.fill = GridBagConstraints.NONE;
	  gbc.anchor = GridBagConstraints.CENTER;
	  
	  Panel cbPanel = new Panel();
	  cbPanel.setLayout(gbl);
	  
	  Util.gblAdd(cbPanel, cbPacks, gbc, 0,0, 1,1, 1,1);
	  Util.gblAdd(cbPanel, cbMsgs,  gbc, 1,0, 1,1, 1,1);
	  Util.gblAdd(cbPanel, cbIdle,  gbc, 2,0, 1,1, 1,1);
	  Util.gblAdd(cbPanel, cbUser,  gbc, 3,0, 1,1, 1,1);
          //Util.gblAdd(cbPanel, colorbyObjectId, gbc, 4,0,1,1, 1,1); 	  
	  

	  //// BUTTON PANEL
	  
	  bSelectRange = new Button("Select Ranges");
	  bColors      = new Button("Change Colors");
	  bDecrease    = new Button("<<");
	  bIncrease    = new Button(">>");
	  bReset       = new Button("Reset");
	  
	  bSelectRange.addActionListener(this);
	  bColors.addActionListener(this);
	  bDecrease.addActionListener(this);
	  bIncrease.addActionListener(this);
	  bReset.addActionListener(this);
	   
	  Label lScale = new Label("SCALE: ", Label.CENTER);
	  scaleField   = new FloatTextField(data.scale, 5);
	  scaleField.addActionListener(this);
	 
	  
	  Panel buttonPanel = new Panel();
	  buttonPanel.setLayout(gbl);
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  
	  Util.gblAdd(buttonPanel, bSelectRange, gbc, 0,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, bColors,      gbc, 1,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, bDecrease,    gbc, 3,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, lScale,       gbc, 4,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, scaleField,   gbc, 5,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, bIncrease,    gbc, 6,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel, bReset,       gbc, 7,0, 1,1, 1,1);
	  
	  // ZOOM PANEL

	  bZoomSelected = new Button("Zoom Selected");
	  bLoadSelected = new Button("Load Selected");

	  bZoomSelected.addActionListener(this);
	  bLoadSelected.addActionListener(this);
	  
	  highlightTime = new TextField("");
	  selectionBeginTime = new TextField("");
	  selectionEndTime = new TextField("");
	  selectionDiff = new TextField("");
	  highlightTime.setEditable(false);
	  selectionBeginTime.setEditable(false);
	  selectionEndTime.setEditable(false);
	  selectionDiff.setEditable(false);

	  Panel zoomPanel = new Panel();
	  zoomPanel.setLayout(gbl);
	  gbc.fill = GridBagConstraints.BOTH;
	  
	  Util.gblAdd(zoomPanel, new Label(" "), gbc, 0,0, 1,1, 1,1);
	  Util.gblAdd(zoomPanel, bZoomSelected,  gbc, 0,2, 1,1, 1,1);
	  Util.gblAdd(zoomPanel, bLoadSelected,  gbc, 1,2, 1,1, 1,1);
	  Util.gblAdd(zoomPanel, highlightTime,  gbc, 2,2, 1,1, 1,1);
	  Util.gblAdd(zoomPanel, selectionBeginTime, gbc, 3,2, 1,1, 1,1);
	  Util.gblAdd(zoomPanel, selectionEndTime,   gbc, 4,2, 1,1, 1,1);
	  Util.gblAdd(zoomPanel, selectionDiff,   gbc, 5,2, 1,1, 1,1);
	  Util.gblAdd(zoomPanel, new Label("Highlight Time", Label.CENTER), gbc, 2,1, 1,1, 1,1);
	  Util.gblAdd(zoomPanel, new Label("Selection Begin Time", Label.CENTER),   gbc, 3,1, 1,1, 1,1);
	  Util.gblAdd(zoomPanel, new Label("Selection End Time", Label.CENTER), gbc, 4,1, 1,1, 1,1);
	  Util.gblAdd(zoomPanel, new Label("Selection Length", Label.CENTER), gbc, 5,1, 1,1, 1,1);

	  // JUMP TO GRAPH	
	  bJumpAnimation = new Button("Animation");
	  bJumpProfile = new Button("Usage Profile");
	  bJumpGraph = new Button("Graph");
	  bJumpHistogram = new Button("Histogram");
	  bJumpComm = new Button("Communication");
	  bJumpStl = new Button("Overview");
	  
	  bJumpAnimation.addActionListener(this);
	  bJumpProfile.addActionListener(this);
	  bJumpGraph.addActionListener(this);
	  bJumpHistogram.addActionListener(this);
	  bJumpComm.addActionListener(this);
	  bJumpStl.addActionListener(this);
	  
	  Panel jumpPanel = new Panel();
	  jumpPanel.setLayout(gbl);
	  gbc.fill = GridBagConstraints.BOTH;
	  
	  Util.gblAdd(jumpPanel, new Label(" "),  gbc, 0,0, 1,1, 1,1);
	  Util.gblAdd(jumpPanel, new Label("Jump to graph: ", Label.LEFT),   gbc, 1,1, 1,1, 1,1);
	  Util.gblAdd(jumpPanel, bJumpAnimation,  gbc, 2,1, 1,1, 1,1);
	  Util.gblAdd(jumpPanel, bJumpProfile,    gbc, 3,1, 1,1, 1,1);
	  Util.gblAdd(jumpPanel, bJumpGraph, 	  gbc, 4,1, 1,1, 1,1);
	  Util.gblAdd(jumpPanel, bJumpHistogram,  gbc, 5,1, 1,1, 1,1);
	  Util.gblAdd(jumpPanel, bJumpComm,       gbc, 6,1, 1,1, 1,1);
	  Util.gblAdd(jumpPanel, bJumpStl,        gbc, 7,1, 1,1, 1,1);
	  

	  //// WINDOW
	  
	  setLayout(gbl);
	  Util.gblAdd(this, mainPanel,   gbc, 0,0, 1,1, 1,1);
	  Util.gblAdd(this, cbPanel,     gbc, 0,1, 1,1, 1,0);
	  Util.gblAdd(this, buttonPanel, gbc, 0,2, 1,1, 1,0);
	  Util.gblAdd(this, zoomPanel,   gbc, 0,3, 1,1, 1,0);
	  Util.gblAdd(this, jumpPanel,   gbc, 0,4, 1,1, 1,0);
   }   
   private void CreateMenus()
   {
	  MenuBar mbar = new MenuBar();
	  
	  mbar.add(Util.makeMenu("File", new Object[]
	  {
		 "Print Timeline",
		 null,
		 "Close"
	  },
	  this));                   
		
	  mbar.add(Util.makeMenu("Tools", new Object[]
	  {
		 "Modify Ranges",
	  },
	  this));
	  
	  mbar.add(Util.makeMenu("Colors", new Object[]
	  {
		 "Change Colors",
		 "Save Colors",
		 "Restore Colors",
		 "Default Colors"
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
   public long getBeginTime()
   {
	  return data.beginTime;
   }   
   public long getEndTime()
   {
	  return data.endTime;
   }   
   public int[] getEntries()
   {
	  return data.entries;
   }   
   public Color[] getEntryColors()
   {
	  if(data == null)
		 return null;
	  else
		 return data.entryColor;
   }   
   public OrderedUsageList[] getEntryUsageData()
   {
	  if(data == null)
		 return null;
	  else
		 return data.entryUsageList;
   }   
   public Color getGraphColor(int e)
   {
	  return mainWindow.getGraphColor(e);
   }   
   public int getHSBValue()
   {
	  return HSB.getValue();
   }   
   public float[] getIdleUsageData()
   {
	  if(data == null)
		 return null;
	  else
		 return data.idleUsage;
   }   
   public float[] getPackUsageData()
   {
	  if(data == null)
		 return null;
	  else
		 return data.packUsage;
   }   
   public OrderedIntList getProcessorList()
   {
	  if(data == null)
		 return null;
	  else
		 return data.processorList;
   }   
   public int getVSBValue()
   {
	  return VSB.getValue();
   }   
   public boolean GraphExists()
   {
	  return mainWindow.GraphExists();
   }   
   public void itemStateChanged(ItemEvent evt)
   {
	  if(data == null)
		 return;
	  
	  Checkbox c = (Checkbox) evt.getSource();
	  
	  if(c == cbPacks)
		  data.showPacks = cbPacks.getState();
	  else if(c == cbMsgs)
		 data.showMsgs = cbMsgs.getState();
	  else if(c == cbIdle)
		 data.showIdle = cbIdle.getState();
	  else if (c == cbUser) {
	    if (cbUser.getState()) {
	      // pop up window
	      userEventWindow.setVisible(true);
	      userEventWindow.show();
	    }
	    else { userEventWindow.setVisible(false); }
	  }
	  
	  setCursor(new Cursor(Cursor.WAIT_CURSOR));
	  displayCanvas.makeNewImage();
	  setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
   }   
   private void PrintTimeline()
   {
	  PrintJob pjob = getToolkit().getPrintJob(this, "Print Timeline", null);
	  
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
	  

	  // Determine what time range we're going to print
	  int hsbval = HSB.getValue();

	  long minx = (long)Math.floor((hsbval - data.offset)/data.pixelIncrement) * data.timeIncrement;
	  if(minx < 0) minx = 0;
	  minx += data.beginTime;
	  
	  long maxx = (long)Math.ceil((hsbval + data.vpw - data.offset)/data.pixelIncrement) * data.timeIncrement;
	  maxx += data.beginTime;
	  if(maxx > data.endTime) maxx = data.endTime;
	  
	  
	  // Determine the range of processors to print
	  int vsbval = VSB.getValue();
	  int miny = (int)Math.floor((double)vsbval/data.tluh);
	  int maxy = (int)Math.floor((double)(vsbval + data.vph)/data.tluh);
	  
	  if(miny < 0) miny = 0;
	  if(maxy > data.numPs-1) maxy = data.numPs - 1;
   
   
	  // Get our first page
	  Graphics pg = pjob.getGraphics();
	  pg.setColor(Color.white);
	  pg.fillRect(0, 0, d.width, d.height);
	  pg.setFont(new Font("SansSerif", Font.PLAIN, 10));
	  pg.translate(marginLeft, marginTop);
	  FontMetrics pfm = pg.getFontMetrics(pg.getFont());
	  
	  // Figure out how many pages this thing will need
	  int textheight = pfm.getHeight();
	  int titleht = 2 * textheight + 12;
	  int axisht =  20 + textheight;
	  int footht = textheight + 5;
	  
	  int[] entries = new int[Analysis.getNumUserEntries()];
	  for(int i=0; i<Analysis.getNumUserEntries(); i++)
		 entries[i] = 0;
	  
	  int idle = 0;   
	  for(int p=miny; p<=maxy; p++)
	  {
		 for(int o=data.tloArray[p].length-1; o >= 0; o--)
		 {
			long bt = data.tloArray[p][o].getBeginTime();
			long et = data.tloArray[p][o].getEndTime();

			if(bt > maxx || et < minx)
			   continue;
			
			int entry = data.tloArray[p][o].getEntry();
			if(entry < 0)
			   idle = 1;
			else          
			   entries[entry] = 1;
		 }      
	  }   
	  
	  int legendcount = 0;
	  for(int i=0; i<Analysis.getNumUserEntries(); i++)
	  {
		 if(entries[i] > 0)
			legendcount++;
	  }
	  
	  if(idle == 1 && data.showIdle == true)
		 legendcount += 1;
		 
	  System.out.println("The number of items to be shown in the legend is " + legendcount);
	  
	  int legendht = ((int)Math.ceil(legendcount / 2.0) + 2) * (textheight + 2);
	  
	  System.out.println("The legend height = " + legendht);
			   
	  
	  int numpgs = 1;
	  
	  int tlht = titleht + 2*axisht + footht;
	  for(int i=miny; i<=maxy; i++)
	  {
		 tlht += data.tluh;
		 if(tlht > printHeight)
		 {
			numpgs++;
			tlht = titleht + 2*axisht + footht + data.tluh;
		 }
	  }       
	  
	  tlht += legendht;
	  while(tlht > printHeight)
	  {
		 numpgs++;
		 tlht = tlht - printHeight + titleht + footht;
	  }   
	  
	  System.out.println("It will take " + numpgs + " to fit this data");
		 
	  
	  // Figure out the scales to print to the page.
	  int plabellen = pfm.stringWidth("Processor " + maxy);
	  int pareawidth = plabellen + 20;
	  
	  int tlareawidth = printWidth - pareawidth;
	  
	  int leftoffset = 5 + pfm.stringWidth("" + minx)/2;
	  int rightoffset = 5 + pfm.stringWidth("" + maxx)/2;
	  long length = maxx - minx + 1;      
	  int width   = tlareawidth - leftoffset - rightoffset;
	 
	  int timeIncrement  = (int)Math.ceil(5/((double)width/length));
	  timeIncrement  = Util.getBestIncrement(timeIncrement); 
	  int numIntervals   = (int)Math.ceil(length/timeIncrement) + 1;
	  double pixelIncrement = (double)width/numIntervals; 
	  
	  int labelIncrement = (int)Math.ceil((pfm.stringWidth("" + maxx) + 10)/pixelIncrement); 
	  labelIncrement = Util.getBestIncrement(labelIncrement); 
	  

	  
	  data.processorList.reset();
	  for(int p=0; p<miny; p++)
		 data.processorList.nextElement();
		 
	  NumberFormat df = NumberFormat.getInstance();
	  String[][] names = Analysis.getEntryNames();

	  int curp = miny;
	  int curlegenditem = 0;
	  
	  boolean drawingLegend = false;
	  
	  for(int page = 0; page<numpgs; page++)
	  {
		 if(pg == null)
		 {
			pg = pjob.getGraphics();
			pg.setFont(new Font("SansSerif", Font.PLAIN, 10));
			pg.translate(marginLeft, marginTop);
		 }   
		 
		 int curheight = 0;
		 
		 // DRAW THE TITLE     
		 String title = "PROJECTIONS TIMELINE FOR " + Analysis.getFilename();
		 pg.setColor(Color.black);
		 curheight += textheight;
		 pg.drawString(title, (printWidth - pfm.stringWidth(title))/2, curheight);
	  
		 curheight += 10;
	  
		 // DRAW THE TOP AXIS
		 int axislength = (int)((numIntervals-1) * pixelIncrement);
		 int curx;
		 String tmp;
	  
		 if(!drawingLegend)
		 {
			curheight += 10 + textheight;
							
			pg.setColor(Color.black);
			pg.drawLine(pareawidth + leftoffset, curheight, pareawidth + leftoffset + axislength, curheight);
			 
			for(int x=0; x<numIntervals; x++)
			{
			   curx = pareawidth + leftoffset + (int)(x*pixelIncrement);
			   if(curx > maxx) break;
			   
			   if(x % labelIncrement == 0)
			   {  
				  tmp = "" + (minx + x*timeIncrement);
				  pg.drawLine(curx, curheight-5, curx, curheight + 5);
				  pg.drawString(tmp, curx - pfm.stringWidth(tmp)/2, curheight - 10);
			   }
			   else
			   {
				  pg.drawLine(curx, curheight-2, curx, curheight+2);
			   }
			}
		 }    
	  
		 // Draw the processor info
		 curheight += 10;

		 int axisheight;
		 for(int p = curp; p<=maxy; p++)
		 {
			if(curheight + data.tluh + axisht + footht > printHeight)
			   break;
			
			curp++;
			curheight += data.tluh;
			axisheight = curheight - data.tluh/2;   
			pg.setColor(Color.black);
			tmp = "Processor " + data.processorList.nextElement();
			pg.drawString(tmp, 10, axisheight);
			
			pg.setColor(Color.gray);
			tmp = "(" + df.format(data.processorUsage[p]) + "%)";
			pg.drawString(tmp, 20, axisheight + pfm.getHeight() + 2);
		 
			pg.setColor(Color.gray);
			pg.drawLine(pareawidth + leftoffset, axisheight, pareawidth + leftoffset + axislength, axisheight);

			for(int o=data.tloArray[p].length-1; o >= 0; o--)
			{
			   long bt = data.tloArray[p][o].getBeginTime();
			   long et = data.tloArray[p][o].getEndTime();

			   if(bt > maxx || et < minx)
				  continue;
 
			   int xpos = (int)((bt - minx) * pixelIncrement / timeIncrement);
			   if(bt < minx)
				  xpos = -5;
			   
			   pg.translate(pareawidth + leftoffset + xpos, axisheight - 10);
			  
			   data.tloArray[p][o].print(pg, minx, maxx, pixelIncrement, timeIncrement);
			   
			   pg.translate(-(pareawidth + leftoffset + xpos), -(axisheight - 10));   
			}
			   
		 }  
	  
		 // Draw the bottom axis
	  
		 if(!drawingLegend)
		 {
			curheight += 10;
			pg.setColor(Color.black);
			pg.drawLine(pareawidth + leftoffset, curheight, pareawidth + leftoffset + axislength, curheight);
			 
			for(int x=0; x<numIntervals; x++)
			{
			   curx = pareawidth + leftoffset + (int)(x*pixelIncrement);
			
			   if(curx > maxx) break;
			   
			   if(x % labelIncrement == 0)
			   {  
				  tmp = "" + (minx + x*timeIncrement);
				  pg.drawLine(curx, curheight-5, curx, curheight + 5);
				  pg.drawString(tmp, curx - pfm.stringWidth(tmp)/2, curheight + 10 + pfm.getHeight());
			   }
			   else
			   {
				  pg.drawLine(curx, curheight-2, curx, curheight+2);
			   }  
			}
			
			curheight += (10 + textheight);
		 }   
		 
		 // Draw the legend
		 if(curp > maxy)
		 {
			curheight += (10 + textheight);
			drawingLegend = true;
			pg.setColor(Color.black);
			String s = "LEGEND";
			pg.drawString(s, (printWidth - pfm.stringWidth(s))/2, curheight);
			curheight += 2*textheight;
		 
			int textx = 0;
			for(int i=curlegenditem; i<entries.length; i++)
			{
			   curlegenditem++;
			   if(entries[i] > 0)
			   {
				  pg.setColor(data.entryColor[i]);
				  s = names[i][1] + "::" + names[i][0];
				  pg.drawString(s, textx, curheight);
				  
				  if(textx == 0)
				  {
					 textx = (int)(printWidth/2.0);
				  
				  // Java seems to have a problem with the division in the following line!   
				  //   System.out.println("PW=" + printWidth + "   PW/2=" + (printWidth/2));
				  }   
				  else
				  {
					 textx = 0;
					 curheight += (textheight + 2);
					 if(curheight + footht > printHeight)
						break; 
				  }        
			   }
			}               
		 }
		 // Draw the footer
		 curheight = printHeight;
		 pg.setColor(Color.black);
		 String footer = "Page " + (page+1) + " of " + numpgs;
		 pg.drawString(footer, (printWidth - pfm.stringWidth(footer))/2, curheight);
		 
		 // Send the page to be printed
		 pg.dispose();
		 pg = null;
	  }    
	  
	  pjob.end(); 
   }   
		
	 private void setAllSizes(boolean clearordraw){
	 	if(clearordraw){
			data.clearAllLines();
			setAllSizes();
		}	
		else{
			setAllSizes();
			data.drawAllLines();
		}
	 }
	 
   private void setAllSizes()
   {
	  data.mpw = mainPanel.getSize().width;
	  data.mph = mainPanel.getSize().height;
	  
	  data.vpw = data.mpw - data.lcw - data.sbw;
	  data.abh = data.mph - data.ath - data.tlh - data.sbh;
	  
	  if(data.abh < data.ath)
	  {
		 data.abh = data.ath;
		 VSB.setVisible(true);
	  }
	  else
		 VSB.setVisible(false);
	  
	  data.vph = data.mph - data.ath - data.abh - data.sbh;
	   
	  setTLSizes();
	  setScales();

	  labelCanvas.setBounds  (0,                 data.ath,          data.lcw, data.vph);
	  axisTopCanvas.setBounds(data.lcw,          0,                 data.vpw, data.ath);
	  displayPanel.setBounds (data.lcw,          data.ath,          data.vpw, data.vph);
	  axisBotCanvas.setBounds(data.lcw,          data.ath+data.vph, data.vpw, data.abh);
	  HSB.setBounds          (data.lcw,          data.mph-data.sbh, data.vpw, data.sbh);
	  VSB.setBounds          (data.mpw-data.sbw, 0,                 data.sbw, data.mph-data.sbh);
	  
	  int vpw = data.vpw;
	  if (vpw < 1) vpw = 1;
	  HSB.setVisibleAmount(vpw);
	  HSB.setBlockIncrement(vpw);
		 
          int vph = data.vph;
	  if (vph < 1) vph = 1;
	  VSB.setVisibleAmount(vph);
	  VSB.setBlockIncrement(vph);
	  
	  setTLBounds();
	   
   }   
   public void setScales()
   {
     // ignoring the calculated value because the disaply canvas somehow 
     // doesn't correspond.
	  int hsbval = HSB.getValue();
	  int vsbval = VSB.getValue();
	  
	  int minx = (int)Math.floor((hsbval - data.offset)/data.pixelIncrement) * data.timeIncrement;
	  if(minx < 0) minx = 0;
	  
	  if(fm == null)
	  {
		 Graphics g = getGraphics();
		 if(g != null)
		 {
			fm = g.getFontMetrics(g.getFont());
			maxLabelLen = fm.stringWidth("" + data.endTime) + 20;
			data.offset = 5 + maxLabelLen/2;     
			g.dispose();
		 }
	  }

	  if(fm != null)
	  {
		 long length = data.endTime - data.beginTime;      
		 int width   = data.tlw - 2*data.offset;
	 
		 data.timeIncrement = (int)Math.ceil(5/((double)width/length));
		 data.timeIncrement = Util.getBestIncrement(data.timeIncrement); 
		 data.numIntervals = (int)Math.ceil(length/data.timeIncrement) + 1;
		 data.pixelIncrement = (double)width/data.numIntervals; 
	  
		 data.labelIncrement = (int)Math.ceil(maxLabelLen/data.pixelIncrement); 
		 data.labelIncrement = Util.getBestIncrement(data.labelIncrement); 
	  }
	  int newhsb = data.scale == 1.0 ? 0 : hsbval;
	  // (int)(minx * data.pixelIncrement / data.timeIncrement) + data.offset;
	  HSB.setValue(newhsb);
	  VSB.setValue(vsbval);
   }   
   private void setTLBounds()
   {   
	  HSB.setMaximum(data.tlw);
	  
	  displayCanvas.setBounds(0,0,data.tlw, data.tlh);
	  
	  if(data.tloArray != null)
	    for(int p=0; p<data.numPs; p++)
	      if(data.tloArray[p] != null)
		for(int i=0; i<data.tloArray[p].length; i++)
		  data.tloArray[p][i].setBounds(p);     
	  
	  if (data.userEventsArray != null) 
	    for(int p=0; p<data.numPs; p++)
	      if(data.userEventsArray[p] != null)
		for(int i=0; i<data.userEventsArray[p].length; i++)
		  data.userEventsArray[p][i].setBounds(p, data);     
   }   
   private void setTLSizes()
   {
	  data.tlw = (int)(data.vpw * data.scale);
	  
	  if(data.scale > 1)
		 HSB.setVisible(true);
	  else
		 HSB.setVisible(false);
   }   
   private void ShowColorWindow()
   {
	  if(colorWindow == null)
		 colorWindow = new TimelineColorWindow(this,data);
	  colorWindow.setVisible(true);
   }   

   private void ShowRangeDialog()
   {
	  data.oldBT = data.beginTime;
	  data.oldET = data.endTime;
	  data.mesgCreateExecVector = new Vector();
	  if(data.processorList == null)
		 data.oldplist = null;
	  else
		 data.oldplist = data.processorList.copyOf();
	  
	  if(rangeDialog == null)
		 rangeDialog = new TimelineRangeDialog(this, data);
	  rangeDialog.setVisible(true);
   }   
}
