package projections.gui;
import projections.gui.graph.*;
import projections.misc.LogEntryData;
import projections.analysis.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class HistogramWindow extends ProjectionsWindow 
   implements ActionListener
{
   static final int NO_OF_BINS = 50;
   static final int FREQUENCY  = 100;	// ms?

   private MainWindow mainWindow;
   private GraphPanel graphPanel;
   private Graph graphCanvas;

   private OrderedIntList validPEs;
   private long startTime;
   private long endTime;

   public HistogramWindow(MainWindow mainWindow)
   {
	  this.mainWindow = mainWindow;

	  addWindowListener(new WindowAdapter()
	  {
		 public void windowClosing(WindowEvent e)
		 {
			close();
		 }
	  });

	  setBackground(Color.lightGray);
	  setTitle("Projections Histograms");

	  createMenus();
	  createLayout();
	  pack();
	  setVisible(true);
		
	  showDialog();
   }   
 
   void showDialog()
   {
	if(dialog == null)
		 dialog = new RangeDialog(this,"Select Range");
	dialog.displayDialog();
	refreshGraph();
   }

   public void setProcessorRange(OrderedIntList proc)
   {
  	validPEs = proc;
   }

   public void setStartTime(long time)
   {
	startTime = time;
   }

   public void setEndTime(long time)
   {
	endTime = time;
   }

   public void actionPerformed(ActionEvent evt)
   {
	  if(evt.getSource() instanceof MenuItem)
	  {
		 MenuItem m = (MenuItem)evt.getSource();
		 if(m.getLabel().equals("Set Range"))
		        showDialog();
		 else if(m.getLabel().equals("Close"))
			close();
	  }
   }  
 
   private void close()
   {
	  setVisible(false);
	  mainWindow.CloseHistogramWindow();
	  dispose();
  } 
  
  private void createLayout()
  {
 	  graphCanvas = new Graph();
	  graphPanel = new GraphPanel(graphCanvas);	
	  add(graphPanel);
   }  

   private void refreshGraph()
   {
// get new counts and redraw the graph
	  int [] counts = getCounts();
	  DataSource ds=new DataSource1D("Histogram (Granularity = 100us)",counts);
	  XAxis xa=new XAxisFixed("Entry Point Execution Time","");
	  YAxis ya=new YAxisAuto("Number of Entry Points","",ds);
	  
	  graphCanvas.setData(ds,xa,ya);
	  graphCanvas.repaint();
   }

   private int[] getCounts()
   {
	  GenericLogReader r;
	  int maxdiff=0;
	  int [] counts = new int[NO_OF_BINS];
	  for(int i=0; i<NO_OF_BINS; i++)
		counts[i] = 0;

	  LogEntryData logdata,logdata2;
	  logdata = new LogEntryData();
	  logdata2 = new LogEntryData();
	  
	  while(validPEs.hasMoreElements()) 
	  {
	  	r = new GenericLogReader(Analysis.getLogName(validPEs.nextElement()),Analysis.getVersion());
	    try{
		r.nextEventOnOrAfter(startTime,logdata);
		while(true){
			r.nextEventOfType(ProjDefs.BEGIN_PROCESSING,logdata);
			r.nextEventOfType(ProjDefs.END_PROCESSING,logdata2);
			int diff = (int)((logdata2.time - logdata.time)/FREQUENCY);
			if(diff >= NO_OF_BINS) 
			{
				maxdiff=(diff>maxdiff)?diff:maxdiff;
				diff = NO_OF_BINS-1;
			}
			counts[diff]++;
			if(logdata2.time > endTime)
				break;
		}
	     }catch(EOFException e){
	     	// do nothing just reached end-of-file
	     }catch(Exception e){
		System.out.println("Exception " + e);
	     }
         }

	  System.out.println("Entry Point with longest time difference: " + maxdiff);

	 return(counts);
   }

   private void createMenus()
   {
	  MenuBar mbar = new MenuBar();

	  mbar.add(Util.makeMenu("File", new Object[]
	  {
		 "Set Range",
		 "Close"
	  },
	  this));

	  Menu helpMenu = new Menu("Help");
	  mbar.add(Util.makeMenu(helpMenu, new Object[]      {

		 "Index",
		 "About"
	  },
	  this));

	  mbar.setHelpMenu(helpMenu);
	  setMenuBar(mbar);
   }   
}
