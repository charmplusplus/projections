package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class GraphWindow extends Frame
   implements ActionListener
{
   private MainWindow mainWindow;

   private GraphDisplayPanel      displayPanel;
   private GraphControlPanel      controlPanel;
   private GraphLegendPanel       legendPanel;
   private GraphAttributesWindow  attributesWindow;
   private GraphIntervalDialog    intervalDialog;
   
   private GraphData data;
   private int  numintervals = -1;
   private long intervalsize;
   private OrderedIntList processorList;
   private String processorListString;
   private int w, h;
   private boolean firstTime = true;
   
   public GraphWindow(MainWindow mainWindow)
   {
	   this.mainWindow = mainWindow;

	  addWindowListener(new WindowAdapter()
	  {                    
		 public void windowClosing(WindowEvent e)
		 {
			Close();
		 }
	  });
	  setBackground(Color.lightGray);
   
	  CreateLayout();
	  CreateMenus();
	  pack();
	  
	  setTitle("Projections Graph");
	  setVisible(true);  
	  
	  ShowIntervalDialog();
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  if(evt.getSource() instanceof MenuItem)
	  {
		 MenuItem mi = (MenuItem)evt.getSource();
		 String arg = mi.getLabel();
		 if(arg.equals("Close"))
			Close();
		 else if(arg.equals("Export Data"))
			ExportData();
		 else if(arg.equals("Print Graph"))
			PrintGraph();   
		 else if(arg.equals("Set Interval Size"))
			ShowIntervalDialog();
		 else if(arg.equals("Timeline"))
			mainWindow.ShowTimelineWindow();
		 else if(arg.equals("Index"))
			mainWindow.ShowHelpWindow();
		 else if(arg.equals("About"))
			mainWindow.ShowAboutDialog((Frame) this);       
	  }
   }   
   private void Close()
   {
	  if(legendPanel != null)
		 legendPanel.closeAttributesWindow();
	  setVisible(false);
	  dispose();
	  mainWindow.CloseGraphWindow();
   }   
   private void CreateLayout()
   {   
	  Panel p = new Panel();
	  add("Center", p);
	  p.setBackground(Color.gray);
	  
	  displayPanel = new GraphDisplayPanel();
	  controlPanel = new GraphControlPanel();
	  legendPanel  = new GraphLegendPanel ();
	  
	  GridBagLayout      gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  p.setLayout(gbl);
	  
	  gbc.fill   = GridBagConstraints.BOTH;
	  Util.gblAdd(p, displayPanel, gbc, 0,0, 1,1, 2,1, 2,2,2,2);
	  Util.gblAdd(p, legendPanel,  gbc, 1,0, 1,1, 1,1, 2,2,2,2);
	  Util.gblAdd(p, controlPanel, gbc, 0,1, 2,1, 1,0, 2,2,2,2);

   }   
   private void CreateMenus()
   {
	  MenuBar mbar = new MenuBar();
	  
	  mbar.add(Util.makeMenu("File", new Object[]
	  {
		 "Export Data",
		 "Print Graph",
		 null,
		 "Close"
	  },
	  this));                   
		
	  mbar.add(Util.makeMenu("Tools", new Object[]
	  {
		 "Set Interval Size",
		 "Timeline"
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
   private void ExportData()
   {
	  if(data == null)
		 return;
	  
	  System.out.println("Exporting Data");
	  FileDialog d = new FileDialog((Frame)this, "Select save file", FileDialog.SAVE);
	  d.setDirectory(".");
	  d.setFile("");
	  d.setVisible(true);
	  String filename = d.getFile();
	  
	  if(filename == null)
		 return;
		 
	  filename = d.getDirectory() + filename;
			
	   try
	  {
		 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));

		 bw.write("# PROJECTIONS GRAPH for " + Analysis.getFilename());   bw.newLine();
		 bw.newLine();
		 
		 bw.write("# X-AXIS = ");
		 if(data.xmode == GraphData.INTERVAL)
			bw.write("Intervals");
		 else if(data.xmode == GraphData.PROCESSOR)
			bw.write("Procesors");
		 bw.newLine();
		 bw.write("# Y-AXIS = ");
		 if(data.ymode == GraphData.MSGS)
			bw.write("Messages");
		 else if(data.ymode == GraphData.TIME)
			bw.write("Time");
		 bw.newLine();
		 bw.write("# Data is shown for ");
		 if(data.xmode == GraphData.INTERVAL)
			bw.write("processors " + data.processor.string);
		 else if(data.xmode == GraphData.PROCESSOR)
			bw.write("intervals " + data.interval.string);
		 bw.newLine();      
		 bw.newLine();         
		 bw.write("# Number of intervals = " + numintervals); bw.newLine();
		 bw.write("# Interval size = " + U.t(intervalsize)); bw.newLine();
		 bw.write("# Number of Processors = " + Analysis.getNumProcessors());bw.newLine();
		 bw.newLine();
		 
		 bw.write("# UNITS IN LEGEND:");                                  bw.newLine();
		 bw.write("# \t% = percentage");                                  bw.newLine();
		 bw.write("# \tM = messages");                                    bw.newLine();
		 bw.newLine();
		 bw.write("# LEGEND: (units) Item Name");                         bw.newLine();
		 
		 int numitems = 0;
		 
		 for(int i=0; i<data.onGraph.length; i++)
		 {
			if(data.onGraph[i].ymode == data.ymode || data.onGraph[i].ymode == GraphData.BOTH)
			{
			   bw.write("#\t" + numitems + "\t= ");
			   if(data.onGraph[i].ymode == GraphData.MSGS)
				  bw.write("(M)\t");
			   else if(data.onGraph[i].ymode == GraphData.TIME)
				  bw.write("(T)\t");
			   else if(data.onGraph[i].ymode == GraphData.BOTH)
				  bw.write("(%)\t");      
			
			   if(data.onGraph[i].parent != null)
				  bw.write(data.onGraph[i].parent + "::");
			   
			   bw.write(data.onGraph[i].name);
			
			   if(!(data.onGraph[i].type.equals("%") || data.onGraph[i].type.equals("Msgs")))
				  bw.write(" " + data.onGraph[i].type);
				
			   bw.newLine();
			   numitems++;
			}   
		 }
		 
		 if(data.xmode == GraphData.INTERVAL)
		 {
			bw.newLine();
			bw.write("# DATA BY INTERVAL"); bw.newLine();
			bw.write("#\tFirst column = interval number"); bw.newLine();
			bw.write("#\tFirst row = legend index"); bw.newLine();
			bw.newLine();
		 }
		 else if(data.xmode == GraphData.PROCESSOR)
		 {   
			bw.newLine();
			bw.write("# DATA BY PROCESSOR"); bw.newLine();
			bw.write("#\tFirst column = processor number"); bw.newLine();
			bw.write("#\tFirst row = legend index"); bw.newLine();
			bw.newLine();
		 }   
		 
		 bw.write(" ");
		 for(int i=0; i<numitems; i++)
			bw.write("\t" + i);
		 bw.newLine();
			
		 if(data.xmode == GraphData.INTERVAL)
		 {   
			for(int i=0; i<numintervals; i++)
			{
			   bw.write("" + i);
			   for(int j=0; j<data.onGraph.length; j++)
			   {
				  if(data.onGraph[j].ymode == data.ymode || data.onGraph[j].ymode == GraphData.BOTH)
					 bw.write("\t" + data.onGraph[j].curIData[i]);
			   }   
			   bw.newLine();
			}   
		 }
		 else if(data.xmode == GraphData.PROCESSOR)
		 {
			for(int p=0; p<Analysis.getNumProcessors(); p++)
			{
			   bw.write("" + p);
			   for(int j=0; j<data.onGraph.length; j++)
			   {
				  if(data.onGraph[j].ymode == data.ymode || data.onGraph[j].ymode == GraphData.BOTH)
					 bw.write("\t" + data.onGraph[j].curPData[p]);
			   }      
			   bw.newLine();
			}
		 }                  
		 
		 bw.close();
	  }
	  catch (IOException e) 
	  {
		 System.out.println("ERROR WRITING TO FILE " + filename);
	  };
			
   }   
   public Color getGraphColor(int e)
   {
	  if(data != null && data.userEntry != null)
		 return data.userEntry[e][0].color;
	  else
		 return null;
   }   
   public long getIntervalSize()
   {
	  return intervalsize;
   }   
   public void paint(Graphics g)
   {
	  super.paint(g);
   }   
   private void PrintGraph()
   {
	  PrintJob pjob = getToolkit().getPrintJob(this, "Print Graph", null);
	  
	  if(pjob != null)
	  {
		 Graphics pg = pjob.getGraphics();
		 displayPanel.PrintGraph(pg, pjob);
		 pg.dispose();
		 
		 Graphics pg2 = pjob.getGraphics();
		 legendPanel.PrintLegend(pg2, pjob);
		 pg2.dispose();
		 
		 pjob.end();
	  }
   }   
   private void setChildDatas()
   {
	   controlPanel.setGraphData(data);
	  displayPanel.setGraphData(data);
	  legendPanel.setGraphData(data);
	  
	  data.graphWindow  = this;
	  data.controlPanel = controlPanel;
	  data.displayPanel = displayPanel;
	  data.legendPanel  = legendPanel;
   }   
   public void setIntervalSize(long x)
   {
	  intervalsize = x;
   }   
   public void setNumIntervals(int x)
   {
	  numintervals = x;
   }   
   public void setProcessorRange(OrderedIntList x)
   {
	  processorList = x;
          processorListString = x.listToString();
   }   
   private void ShowIntervalDialog()
   {

	  int oldIntervals = numintervals;
          OrderedIntList oldProcList = null;
          if (processorList!=null) oldProcList = processorList.copyOf();
	  
	  intervalDialog = new GraphIntervalDialog(this, numintervals, processorListString);
	  intervalDialog.setVisible(true);
	  intervalDialog = null;
	  
	  if(numintervals != oldIntervals || !oldProcList.equals(processorList))
	  {
		 setCursor(new Cursor(Cursor.WAIT_CURSOR));
		 Dialog d = new Dialog((Frame)this, "Loading data...", false);
		 d.setLayout(new GridLayout(2,1));
		 d.add(new Label("Please wait while your data", Label.CENTER));
		 d.add(new Label("is loaded and analyzed", Label.CENTER));
		 d.pack();
		 d.setLocation((w-d.getSize().width)/2, (h-d.getSize().height)/2);
		 d.setVisible(true);
	  
		 Analysis.LoadGraphData(numintervals, intervalsize, true, processorList);
		 if(data == null || !oldProcList.equals(processorList))
		 {
			data = null;
			data = new GraphData(numintervals, intervalsize, processorList);
			setChildDatas();
			/* also need to close and free legendPanel */
			if (legendPanel!=null) 
			  legendPanel.closeAttributesWindow();
		 }   
		 else {
			// only can reuse the data when processor list
                        // is unchanged.
	                data.initData(numintervals, intervalsize);
                 }
	  
		 controlPanel.setXMode(data.xmode);
		 controlPanel.setYMode(data.ymode);
	  
		 d.setVisible(false);
		 d.dispose();
		 
		 legendPanel.UpdateLegend();
		 displayPanel.setAllBounds();
		 displayPanel.UpdateDisplay(); 

		 setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	  }   
   }   
}
