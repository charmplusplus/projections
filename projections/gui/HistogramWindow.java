package projections.gui;
import projections.gui.graph.*;
import projections.misc.LogEntryData;
import projections.analysis.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class HistogramWindow extends ProjectionsWindow 
   implements ActionListener,ItemListener
{
   static final int NO_OF_BINS = 50;
   static final int FREQUENCY  = 100;	// ms?

   private MainWindow mainWindow;
   private GraphPanel graphPanel;
   private Graph graphCanvas;
   private EntrySelectionDialog entryDialog;
   private OrderedIntList validPEs;
   private long startTime;
   private long endTime;
   private boolean stateArray[][];
   private String [] entryNames; 

   private boolean recordEP;
   private File fileEP;
 
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

	  int noEPs = Analysis.getUserEntryCount();
	  stateArray = new boolean[1][noEPs];	// where should this be?
	  for(int i=0; i < noEPs; i++)
		stateArray[0][i] = true;
	  
          String names[][] = Analysis.getUserEntryNames();
	  entryNames = new String[noEPs];
	  Analysis.getUserEntryNames();
       	  for(int i=0; i<noEPs ; i++)
		entryNames[i] = names[i][0];

	  recordEP = false;	// dont record longest EPs unless specified
	  fileEP = null;
	
	  createMenus();
	  createLayout();
	  pack();
	  setVisible(true);
		
	  showDialog();

   }   

/* Show the RangeDialog to set processor numbers and interval times */
 
   void showDialog()
   {
	if(dialog == null)
		 dialog = new RangeDialog(this,"Select Range");
	dialog.displayDialog();
	refreshGraph();
   }

/* Show the EntrySelectionDialog to select Entrypoints to be considered */

   void showEntryDialog()
   {
	int noEPs = Analysis.getUserEntryCount();
	String typeLabelStrings[] = {"Entry Points"};

        Color colorArray[][] = new Color[1][noEPs];
	for(int i=0; i < noEPs; i++)
		colorArray[0][i] = Analysis.getEntryColor(i);

 
	boolean existsArray[][] = new boolean[1][noEPs];
	for(int i=0; i<noEPs; i++)
		existsArray[0][i] = true;
	  
	if(entryDialog == null)
		 entryDialog = new EntrySelectionDialog(this, typeLabelStrings,stateArray,colorArray,existsArray,entryNames);
	entryDialog.showDialog();
	refreshGraph();
   }

/* functions for RangeDialog to work */

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
		 else if(m.getLabel().equals("Select Entry Points"))
			showEntryDialog();
		 else if(m.getLabel().equals("Close"))
			close();
	  }
   } 

   public void itemStateChanged(ItemEvent evt)
   {
	recordEP = (evt.getStateChange()==1)?true:false;	// if the itemStateChanged is to 1, then it is selected
	if(recordEP && (fileEP == null))
			fileEP = new File(Analysis.getFilename()+".longestEPs.log");

	refreshGraph();
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

	  OrderedIntList tmpPEs = validPEs.copyOf();
	  GenericLogReader r;
	  FileWriter out = null;
	  int maxdiff=0;
	  int [] counts = new int[NO_OF_BINS];
	  for(int i=0; i<NO_OF_BINS; i++)
		counts[i] = 0;

	  LogEntryData logdata,logdata2;
	  logdata = new LogEntryData();
	  logdata2 = new LogEntryData();

     try{	
	if(fileEP!=null)  
		out = new FileWriter(fileEP);
     }catch(Exception e){
	  System.out.println("Cannot record Entry Points in the specified file:\n "+e);
	  fileEP = null;	// if unable to create file, forget abt it!
     }

	  while(tmpPEs.hasMoreElements()) 
	  {
		int pe = tmpPEs.nextElement();
	  	r = new GenericLogReader(Analysis.getLogName(pe),Analysis.getVersion());
	   try{
		r.nextEventOnOrAfter(startTime,logdata);
		while(true){
			r.nextEventOfType(ProjDefs.BEGIN_PROCESSING,logdata);
			r.nextEventOfType(ProjDefs.END_PROCESSING,logdata2);
			if(stateArray[0][logdata.entry]){			// if the entry method is selected, count it
				int diff = (int)((logdata2.time - logdata.time)/FREQUENCY);
				if(diff >= NO_OF_BINS) 
				{
					if(recordEP && (out!=null))
						out.write("["+pe+"]: "+entryNames[logdata.entry]+" "+logdata.time+" "+logdata2.time+" "+(logdata2.time-logdata.time)+"\n");
					maxdiff=(diff>maxdiff)?diff:maxdiff;
					diff = NO_OF_BINS-1;
				}
				counts[diff]++;
			 }
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
		 "Select Entry Points",
		 "Close"
	  },
	  this));

          mbar.add(Util.makeMenu("View", new Object[]
          {
                 new CheckboxMenuItem("Record Longest EPs")
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
