package projections.gui;
import projections.misc.LogEntryData;
import projections.analysis.*;
import projections.gui.graph.*;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/* EntryPoint characteristics Window
*  This class aims to plot the entrypoints and other events 
*  against several parameters such as executiontime
*/

public class EPCharWindow extends ProjectionsWindow
	implements ActionListener{

   private GraphPanel graphPanel;
   private Graph graphCanvas;

// ADD: variable for pie chart
// private

//variables to be set by RangeDialog
/*   private OrderedIntList validPEs;
   private long startTime;
   private long endTime;*/

   private long intervalSize;
//   private long thresholdTime;  // to record EPs that cross this time	ADD IF NEEDED?

//variables to be passed to EntrySelectionDialog
   private EntrySelectionDialog entryDialog;
   private boolean stateArray[][];
   private Color colorArray[][];
   private String [] entryNames;

   private double [][] dataSource;	// to store the data
// ADD IF NEEDED
//   private boolean getNewData = true;	// default: refresh graph always calculates new data before display
					// if false, refresh graph just sets the graph to a new data source

   private boolean startUp = true;               // show both range dialog & epdialog during startup

// constants 
   private final int NO_OF_PARAMETERS  = 1;	// execution time
   private final int EXECUTION_TIME    = 0;		

// later replace this with EPCharWindow()
   public EPCharWindow(MainWindow mainWindow)
   {
	  super(); 
 
          setTitle("Projections EntryPoint Characteristics Graph");

          createLayout();
	  createMenus();
          pack();
          setVisible(true);
          showDialog();
	  refreshGraph();
   }

/* Show the RangeDialog to set processor numbers and interval times */
   void showDialog()
   {
        if(dialog == null)
                 dialog = new RangeDialog(this,"Select Range"); // OR IntervalRangeDialog AS NEEDED
        dialog.displayDialog();
        if(!isDialogCancelled)
	{
	// Range has been changed, so get new data while refreshing
	//	getNewData = true;
	//   if(!startUp)
                refreshGraph();
        //   else
         //       showEntryDialog();
	}
   }

/* functions for RangeDialog to work */
/*   public void setProcessorRange(OrderedIntList proc)
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
*/

   public void setIntervalSize(long size)
   {
	intervalSize = size;
   }

/* ADD IF NEEDED
   public void setThresholdTime(long time)
   {
	thresholdTime = time;
   } */

/* Show the EntrySelectionDialog to select Entrypoints to be considered */

   void showEntryDialog()
   {
        //if(startUp) startUp = false;

        int noEPs = Analysis.getUserEntryCount();
        String typeLabelStrings[] = {"Entry Points"};

        boolean existsArray[][] = new boolean[1][noEPs];
        for(int i=0; i<noEPs; i++)
                existsArray[0][i] = true;

        if(entryDialog == null)
                 entryDialog = new EntrySelectionDialog(this, typeLabelStrings,stateArray,colorArray,existsArray,entryNames);
        entryDialog.showDialog();
       // refreshGraph();
   }

   public void actionPerformed(ActionEvent evt)
   {
	if(evt.getSource() instanceof JMenuItem)
          {
                 JMenuItem m = (JMenuItem)evt.getSource();
                 if(m.getText().equals("Set Range"))
                        showDialog(); 
                 else if(m.getText().equals("Select Entry Points"))
                        showEntryDialog(); 
		 else if(m.getText().equals("Show Pie Chart"))
		 {
			// no need to get new data while refreshing
			// getNewData = false;
			refreshGraph();
   		 }		
 		 else if(m.getText().equals("Close"))
                        close();
          }
   }

/* add graphPanel to the window */
  private void createLayout()
  {
	  Container contentPane = getContentPane();
          graphCanvas = new Graph();
          graphPanel = new GraphPanel(graphCanvas);
	  contentPane.add(graphPanel,BorderLayout.CENTER);
  }

/* create the menu bar */
   protected void createMenus()
   {
          JMenuBar mbar = new JMenuBar();
          setJMenuBar(mbar);

	  JCheckBoxMenuItem pieChartMenuItem = new JCheckBoxMenuItem("Show Pie Chart");
	  pieChartMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));

          mbar.add(Util.makeJMenu("File", new Object[]
          {
                 "Set Range",
		 "Select Entry Points",
                 "Close"
          },
          this));

	  mbar.add(Util.makeJMenu("View",new Object[]
	  {
		pieChartMenuItem	
	  },
	  this)); 

// setHelpMenu is not yet implemented for JMenuBar
         /* JMenu helpMenu = new JMenu("Help");
          mbar.add(Util.makeJMenu(helpMenu, new Object[]      {
                 "Index",
                 "About"
          },
          this));
 
          mbar.setHelpMenu(helpMenu);*/

    }

   private void refreshGraph()
   {	
	  // get new data only if either range or entrypoint changes. 
	  // not when a change is made btw "sends vs Eps" or "bytes vs Eps"
	//  if(getNewData)
          	getNewData();
	  
	  String titleString = "EntryPoints vs Execution Time";
//for(int i=0;i<dataSource.length; i++)
	//System.out.println(dataSource[i][0];
	  // set values and draw the graph
          DataSource ds=new DataSource2D(titleString,dataSource);
          XAxis xa=new XAxisFixed("Entry Points","");
          YAxis ya=new YAxisAuto("Time","",ds);
 
          graphCanvas.setData(ds,xa,ya);
          graphCanvas.repaint();	
   }

   private void getNewData()
   {

        OrderedIntList tmpPEs = validPEs.copyOf();
        GenericLogReader logReader;

	LogEntryData logData,prevLogData;
        logData = new LogEntryData();
        prevLogData = new LogEntryData();
	
	int no_of_bins = Analysis.getNumUserEntries();		// number of x-axis points
	dataSource = new double[no_of_bins][NO_OF_PARAMETERS];

	for(int i=0; i<no_of_bins; i++)
		for(int j=0; j<NO_OF_PARAMETERS; j++)
			dataSource[i][j] = 0;
System.out.println("no. of bins: " + no_of_bins);
        while(tmpPEs.hasMoreElements())
        {
                int pe = tmpPEs.nextElement();
		int binValue;
                logReader = new GenericLogReader(Analysis.getLogName(pe),Analysis.getVersion());

	try{
                logReader.nextEventOnOrAfter(startTime,logData);
                while(true){
                        logReader.nextEvent(logData);	// find the next event
			// this might not count the last EP. is it ok??
                        if(logData.time > endTime)
                                break;

			switch(logData.type){
			   case ProjDefs.BEGIN_PROCESSING:		// found the beginning of an entrypoint execution
					prevLogData = logData.copyOf();		// store the data
					break;
			   case ProjDefs.END_PROCESSING:		// found the ending of an entrypoint execution
					long executionTime = logData.time-prevLogData.time; 
		//			if(executionTime > thresholdTime)
		//			{
						binValue = logData.entry;
						dataSource[binValue][0]+= executionTime;	// increment corresponding value
						//System.out.println("entry: "+binValue+" time: "+executionTime);
		//			}
					break;
			}
                }
 
             }catch(EOFException e){
                // do nothing just reached end-of-file
             }catch(IOException e){
                System.out.println("Exception at IntervalWindow::getData() " + e);
             }
 
         }
   }
}

	

