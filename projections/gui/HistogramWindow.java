package projections.gui;
import projections.gui.graph.*;
import projections.misc.LogEntryData;
import projections.analysis.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class HistogramWindow extends ProjectionsWindow 
   implements ActionListener,ItemListener
{
   static final int NO_OF_BINS = 50;
   static final int FREQUENCY  = 100;	// ms?

   private MainWindow mainWindow;
   private GraphPanel graphPanel;
   private Graph graphCanvas;

//variables to be set by RangeDialog 
   private OrderedIntList validPEs;
   private long startTime;
   private long endTime;
   private long totalExecutionTime;	// sum total of the execution times of all the entry points selected
   private long longestExecutionTime;	// longest EntryPoint execution time

//variables to be passed to EntrySelectionDialog
   private EntrySelectionDialog entryDialog;
   private boolean stateArray[][];
   private Color colorArray[][];
   private String [] entryNames; 

   private JTextArea statusArea;		// displays the number of EPs in each bin as an ordered pair
   private boolean recordEP;			// should longest entrypoints be recorded & displayed as a table?
   private EntryPointWindow epFrame;

   private boolean startUp;			// show both range dialog & epdialog during startup
 
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
       	  for(int i=0; i<noEPs ; i++)
		entryNames[i] = names[i][0];

          colorArray = new Color[1][noEPs];
          for(int i=0; i < noEPs; i++)
                colorArray[0][i] = Analysis.getEntryColor(i);

	  recordEP = false;	// dont record longest EPs unless specified
	  epFrame = null;
	  statusArea = new JTextArea(6,2);	// to display the no. of EPs vs bins as text	
	  startUp = true;

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
	if(!isDialogCancelled)
		if(!startUp)
			refreshGraph();
		else
			showEntryDialog();
   }

/* Show the EntrySelectionDialog to select Entrypoints to be considered */

   void showEntryDialog()
   {
	if(startUp) startUp = false;

	int noEPs = Analysis.getUserEntryCount();
	String typeLabelStrings[] = {"Entry Points"};

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
	if(recordEP)
		refreshGraph();
	else
		epFrame.setVisible(false);
   }
 
   private void close()
   {
	  setVisible(false);
	  mainWindow.CloseHistogramWindow();
	  dispose();
  } 
  
  private void createLayout()
  {
	  JPanel mainPanel = new JPanel(); 
 	  graphCanvas = new Graph();
	  graphPanel = new GraphPanel(graphCanvas);	
	  mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));
	  mainPanel.add(graphPanel);
          mainPanel.add(Box.createRigidArea(new Dimension(0,6)));
	  mainPanel.add(new JScrollPane(statusArea,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
	  add(mainPanel);
  }  

   private void refreshGraph()
   {
// get new counts and redraw the graph
	  int [] counts = getCounts();
	  DataSource ds=new DataSource1D("Histogram (Granularity = 100us)",counts);
	  XAxis xa=new XAxisFixed("Entry Point Execution Time","");
	  YAxis ya=new YAxisAuto("Entry Points#","",ds);
	  
	  graphCanvas.setData(ds,xa,ya);
	  graphCanvas.repaint();

	  String firstRow  ="Bin  ", secondRow="EPs  ";	
	  String  thirdRow = "Total Execution Time: " + String.valueOf(totalExecutionTime)+ " us";
	  String fourthRow = "Longest Entry Point Execution Time: " + longestExecutionTime +" us";

	  for(int i=0; i<counts.length; i++)
		if(counts[i]!=0)
		{
			firstRow = firstRow + i + "\t";
			secondRow = secondRow + counts[i] +"\t";
		}

	   // clear the text area and enter new set
	   statusArea.setText("");	
	   statusArea.append(firstRow+"\n");
	   statusArea.append(secondRow+"\n\n");
	   statusArea.append(thirdRow+"\n");
	   statusArea.append(fourthRow);
   }

   private int[] getCounts()
   {

	  totalExecutionTime = 0;
	  longestExecutionTime=0;

	  OrderedIntList tmpPEs = validPEs.copyOf();
	  GenericLogReader r;
	  if(recordEP)
	  {
		//epFrame = new EPFrame();
		if(epFrame == null){
			epFrame = new EntryPointWindow();
	  		epFrame.setSize(600,600);
		}
		else
			epFrame.clearTableData();	// prepare table to enter new data in place of old
	  }
	 
	  int [] counts = new int[NO_OF_BINS];
	  for(int i=0; i<NO_OF_BINS; i++)
		counts[i] = 0;

	  LogEntryData logdata,logdata2;
	  logdata = new LogEntryData();
	  logdata2 = new LogEntryData();

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
				long executionTime = (logdata2.time - logdata.time);
				totalExecutionTime += executionTime;

				int diff = (int)(executionTime/FREQUENCY);
				if(diff >= NO_OF_BINS) 
				{
				// enter the data into the table
				if(recordEP)
				        epFrame.writeToTable(pe,entryNames[logdata.entry],logdata.time,logdata2.time,colorArray[0][logdata.entry]);

					longestExecutionTime=(executionTime>longestExecutionTime)?executionTime:longestExecutionTime;
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
	  if(recordEP)	epFrame.setVisible(true);
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
                 new CheckboxMenuItem("Show Longest EPs")
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
