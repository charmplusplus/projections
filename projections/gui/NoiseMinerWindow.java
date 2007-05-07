package projections.gui;
import projections.misc.LogEntryData;
import projections.analysis.*;
import projections.gui.graph.*;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;



/**
 *  @class NoiseMinerWindow
 *  @author Isaac Dooley
 */

public class NoiseMinerWindow extends ProjectionsWindow
    implements ItemListener
{
    private NoiseMinerWindow      thisWindow;    
    
    private Label                lTitle;
    private Panel                titlePanel;

    private JPanel	         mainPanel;
    private JPanel           controlPanel;
    private JPanel	         noiseMinerResultPanel;
    
	private JTable tableInternal;
	private DefaultTableModel tableModelInternal;
	private JTable tableExternal;
	private DefaultTableModel tableModelExternal;
	
	
	
	private final Vector columnNames;
	
    private JButton              setRanges;
    
	private JTextArea   mainText;
	private JScrollPane	mainTextScroller;
	        
    public OrderedIntList        validPEs;
    public long                  startTime;
    public long                  endTime;
    
    private NoiseMiner			noiseMiner;
    
    void windowInit() {
    }

    public NoiseMinerWindow(MainWindow parentWindow, Integer myWindowID) {
		super(parentWindow, myWindowID);
		thisWindow = this;
		
		setBackground(Color.lightGray);
		setTitle("Projections Computational Noise Miner - " + Analysis.getFilename() + ".sts");
		
		columnNames = new Vector();
		columnNames.add(new String("Noise Duration(us)"));
		columnNames.add(new String("Seen on Processors"));
		columnNames.add(new String("Occurrences/PE"));
		columnNames.add(new String("Periodicity(ms)")); 
		columnNames.add(new String("Periodicity from FFT(ms)"));

		
		mainText = new JTextArea("", 8, 30); // height, width
		mainTextScroller = new JScrollPane(mainText);
		mainTextScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			
		mainPanel = new JPanel();
		setLayout(mainPanel);
		CreateLayout();
		pack();
		showDialog();
		setVisible(true);
    }

    public void actionPerformed(ActionEvent e)
    {
    	if(e.getSource() instanceof JButton) {
		    JButton b = (JButton)e.getSource();
		    if(b == setRanges)
			showDialog();
        } 
    }   
    
    public void showDialog() {
		if (dialog == null) {
		    dialog = new RangeDialog(this, "select Range");
		}
		else {
		    setDialogData();
		}
		dialog.displayDialog();
		if (!dialog.isCancelled()) {
		    getDialogData();
		    final SwingWorker worker = new SwingWorker() {
			    public Object construct() {
			    	noiseMiner = new NoiseMiner(startTime, endTime, validPEs);
			    	noiseMiner.gatherData(thisWindow);
			        mainText.setText(noiseMiner.getText());
			        addResultsToTable(noiseMiner.getResultsTableInternal(), noiseMiner.getResultsTableExternal());
			        return null;
			    }
			    public void finished() {
//			    	System.out.println("displayDialog finished()");
			    }
		    };
		    worker.start();
		}
    }
    
    
    private void addResultsToTable(Vector dataInternal, Vector dataExternal){
    	tableModelInternal = new DefaultTableModel(dataInternal, columnNames);
    	tableModelExternal = new DefaultTableModel(dataExternal, columnNames);

    	tableInternal.setModel(tableModelInternal);
    	tableExternal.setModel(tableModelExternal);
    	
    }
    
    
    private void CreateLayout()
    {  
    	JPanel noiseMinerResultPanel = new JPanel();

    	DefaultTableModel tableModelInternal = new DefaultTableModel(columnNames, 0);
    	DefaultTableModel tableModelExternal = new DefaultTableModel(columnNames, 0);
    	
    	tableExternal = new JTable(tableModelExternal);
    	tableInternal = new JTable(tableModelInternal);
    
    	JScrollPane resultTableInternal = new JScrollPane(tableInternal);
    	JScrollPane resultTableExternal = new JScrollPane(tableExternal);
    	
    	JLabel descriptionInternal = new JLabel("<html><body>The following table contains the noise components that have durations much shorter than the OS Timeslice Quanta</body></html>");
    	JLabel descriptionExternal = new JLabel("<html><body>The following table contains the noise components that have durations that are similar to or longer than the OS Timeslice Quanta</body></html>");
  	
    	JPanel internalPanel = new JPanel();
    	internalPanel.setLayout(new java.awt.BorderLayout());
    	internalPanel.add(resultTableInternal, BorderLayout.CENTER );
    	internalPanel.add(descriptionInternal, BorderLayout.NORTH );
    	
    	JPanel externalPanel = new JPanel();
    	externalPanel.setLayout(new java.awt.BorderLayout());
    	externalPanel.add(resultTableExternal, BorderLayout.CENTER );
    	externalPanel.add(descriptionExternal, BorderLayout.NORTH );
    	   	
    	
        /* Setup our tabbed Pane at the top. Add this pane to the main applet Content Pane.  */
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Internal Noise", internalPanel);
        tabbedPane.addTab("External OS Noise", externalPanel);

    	
    	noiseMinerResultPanel.setLayout(new java.awt.BorderLayout());
    	noiseMinerResultPanel.add(mainTextScroller, BorderLayout.SOUTH );
    	noiseMinerResultPanel.add(tabbedPane, BorderLayout.CENTER );
//    	noiseMinerResultPanel.add(tempLabel, BorderLayout.NORTH );

    	
	  mainPanel.setLayout(new java.awt.BorderLayout());
	  	  
	  // control panel items
	  setRanges = new JButton("Select New Range");
	  setRanges.addActionListener(this);
	  controlPanel = new JPanel();
	  	    
	  controlPanel.add(setRanges);
	  
	  // Add the result gui
	  mainPanel.add(noiseMinerResultPanel, BorderLayout.CENTER);
	  mainPanel.add(controlPanel, BorderLayout.SOUTH);
    }
 
    public void itemStateChanged(ItemEvent ae){
    }


    public void getDialogData() {
		validPEs = dialog.getValidProcessors();
		startTime = dialog.getStartTime();
		endTime = dialog.getEndTime();
    }

    public void setDialogData() {
		dialog.setValidProcessors(validPEs);
		dialog.setStartTime(startTime);
		dialog.setEndTime(endTime);
		super.setDialogData();	
    }

    public void showWindow() {
    }
    


}
