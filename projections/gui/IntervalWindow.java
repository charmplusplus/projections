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

/* Aim is to record the number of sends in the given interval range
*  check for CREATION event and increment corresponding begin time  
*  At the same time record the entrypoints that exceed the threshold 
*  execution time in the corresponing beginTime.
*  It also has the ability to show the message length (in bytes) of the send events
*  along with the stretched eps.  
*/

public class IntervalWindow extends ProjectionsWindow
	implements ActionListener,ItemListener{

   private GraphPanel graphPanel;
   private Graph graphCanvas;

//variables (other than those set by super class) to be set by RangeDialog
   private long thresholdTime;	// to record EPs that cross this time
   private long intervalSize;

   private double [][] dataSource;	// to store the data
   private boolean countSends = true;	// default: draw Sends vs Eps graph
   private boolean getNewData = false;	// default: refresh graph is false 
					// if false, refresh graph just sets the graph to a new data source
// constants 
   private final int NO_OF_DATASOURCES = 3;
   private final int STRETCHED_EP      = 0;
   private final int SEND_COUNT        = 1;
   private final int MSG_LEN_COUNT     = 2;

//later replace this with IntervalWindow()
   public IntervalWindow(MainWindow mainWindow)
   {
          super();
          setTitle("Projections Interval Graph");

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
                 dialog = new IntervalRangeDialog(this,"Select Range");
        dialog.displayDialog();
        if(!isDialogCancelled)
	{
	// Range has been changed, so get new data while refreshing
		getNewData = true;
                refreshGraph();
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

   public void setThresholdTime(long time)
   {
	thresholdTime = time;
   }


   public void actionPerformed(ActionEvent evt)
   {
	if(evt.getSource() instanceof JMenuItem)
          {
                 JMenuItem m = (JMenuItem)evt.getSource();
                 if(m.getText().equals("Set Range"))
                        showDialog(); 
		 else if(m.getText().equals("Send Count vs Stretched EntryPoints"))
		 {
			countSends = (m.isSelected())?true:false;
			// no need to get new data while refreshing
			getNewData = false;
			refreshGraph();
   		 }		
		 else if(m.getText().equals("Bytes Sent vs Stretched EntryPoints"))
		 {
			countSends = (m.isSelected())?false:true;	
			// no need to get new data while refreshing
			getNewData = false;
			refreshGraph();
		 }
 		 else if(m.getText().equals("Close"))
                        close();
          }
   }

    public void itemStateChanged(ItemEvent e) {
        JMenuItem source = (JMenuItem)(e.getSource());
        String s = "Item event detected."
                   + " \n   Event source: " + source.getText()
                   + "\n    New state: "
                   + ((e.getStateChange() == ItemEvent.SELECTED) ?
                     "selected":"unselected");
        System.out.println(s);
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
	  JRadioButtonMenuItem countGraphButton = new JRadioButtonMenuItem("Send Count vs Stretched EntryPoints",true);
	  countGraphButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
	  JRadioButtonMenuItem byteGraphButton  = new JRadioButtonMenuItem("Bytes Sent vs Stretched EntryPoints",false);	
	  byteGraphButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));

	  ButtonGroup graphChoiceButton = new ButtonGroup();
	  graphChoiceButton.add(countGraphButton);
	  graphChoiceButton.add(byteGraphButton);

          JMenuBar mbar = new JMenuBar();
          setJMenuBar(mbar);

          mbar.add(Util.makeJMenu("File", new Object[]
          {
                 "Set Range",
                 "Close"
          },
          this));

	  mbar.add(Util.makeJMenu("Options",new Object[]
	  {
		countGraphButton,
		byteGraphButton	
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
	  if(getNewData)
          	getNewData();
	  
	  if(dataSource != null){

		  // if countSends is true, draw the "send vs Eps" graph, else draw "bytes vs Eps"
		int mode;
	  	String titleString;
		if(countSends){
			titleString = "Stretched EntryPoints vs Send Messages";
			mode  	    = SEND_COUNT;	
		}else{
			titleString = "Stretched EntryPoints vs Bytes Sent";
			mode 	    = MSG_LEN_COUNT;
		}

	  	double[][] data = new double[dataSource.length][2];		// show two values at a time  
	  	for(int i=0; i<dataSource.length; i++)
	  	{
			data[i][0] = dataSource[i][STRETCHED_EP];
			data[i][1] = dataSource[i][mode];
	  	}

	  	// set values and draw the graph
          	DataSource ds     = new DataSource2D(titleString,data);
          	XAxisFixed xa     = new XAxisFixed("Time Interval ("+U.t(startTime)+" - "+U.t(endTime) + ")","");
	  	double multiplier = (double)(endTime-startTime)/(dataSource.length-1);	// in us
	  	xa.setLimits(startTime/1000,multiplier/1000);			// to display in ms
          	YAxis ya=new YAxisAuto("#","",ds);
 
          	graphCanvas.setData(ds,xa,ya);
         	graphCanvas.repaint();
	  }	
   }

   private void getNewData()
   {

        OrderedIntList tmpPEs = validPEs.copyOf();
        GenericLogReader logReader;

	LogEntryData logData,prevLogData;
        logData = new LogEntryData();
        prevLogData = new LogEntryData();
	
	int no_of_bins = (int)((endTime - startTime)/intervalSize)+1;		// number of x-axis points
	dataSource = new double[no_of_bins][NO_OF_DATASOURCES];

	for(int i=0; i<no_of_bins; i++)
		for(int j=0; j<NO_OF_DATASOURCES; j++)
			dataSource[i][j] = 0;

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
			   case ProjDefs.CREATION:		// send Event
					binValue = (int)((logData.time-startTime)/intervalSize);
					dataSource[binValue][SEND_COUNT]++;	// increment corresponding value
					dataSource[binValue][MSG_LEN_COUNT] += logData.msglen;	// increment the number of bytes sent
					break;
			   case ProjDefs.BEGIN_PROCESSING:		// found the beginning of an entrypoint execution
					prevLogData = logData.copyOf();		// store the data
					break;
			   case ProjDefs.END_PROCESSING:		// found the ending of an entrypoint execution
					long executionTime = logData.time-prevLogData.time; 
					if(executionTime > thresholdTime)
					{
						binValue = (int)((prevLogData.time - startTime)/intervalSize);
						dataSource[binValue][STRETCHED_EP]++;	// increment corresponding value
					}
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

	

