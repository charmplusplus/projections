package projections.gui;
import projections.misc.LogEntryData;
import projections.analysis.*;
import projections.gui.graph.*;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 *  @class ComputationalNoiseWindow
 *  @author Isaac Dooley
 */

public class ComputationalNoiseWindow extends ProjectionsWindow
    implements ItemListener
{
    private ComputationalNoiseWindow      thisWindow;    
    
    private Label                lTitle;
    private Panel                titlePanel;

    private JPanel	         mainPanel;
    private JPanel           controlPanel;

    private JButton              setRanges;
    
	private JTextArea   mainText;
	private JScrollPane	mainTextScroller;
	        
    public OrderedIntList        validPEs;
    public long                  startTime;
    public long                  endTime;
    
    private NoiseMiner			noiseMiner;
    
    void windowInit() {
    }

    public ComputationalNoiseWindow(MainWindow parentWindow, Integer myWindowID) {
		super(parentWindow, myWindowID);
		thisWindow = this;
		
		setBackground(Color.lightGray);
		setTitle("Projections Computational Noise Miner - " + Analysis.getFilename() + ".sts");
		
		mainText = new JTextArea("", 30, 30);
		mainTextScroller = new JScrollPane(mainText);
//		mainTextScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
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
			        return null;
			    }
			    public void finished() {
			    	System.out.println("displayDialog finished()");
			    }
		    };
		    worker.start();
		}
    }
    
    private void CreateLayout()
    {  
	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  mainPanel.setLayout(gbl);
	  	  
	  // control panel items
	  setRanges = new JButton("Select New Range");
	  setRanges.addActionListener(this);
	  controlPanel = new JPanel();
	  controlPanel.setLayout(gbl);
	  	    
	  Util.gblAdd(controlPanel, setRanges, gbc, 0,0, 1,1, 0,0); 
	  Util.gblAdd(mainPanel, mainTextScroller,  gbc, 0,1, 1,1, 1,1);
	  Util.gblAdd(mainPanel, controlPanel,   	gbc, 0,2, 1,0, 0,0);
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
