package projections.gui;

import projections.misc.*;
import projections.analysis.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class BGGraphWindow extends GenericGraphWindow 
   implements ActionListener
{
    private long totalExecutionTime;	
    private boolean startUp;
    
    public BGGraphWindow(MainWindow mainWindow)
    {
	super("Projections Unified Summary Overview");
	setTitle("Projections Unified Summary Overview");

	//	showDialog();
	startTime = 0;
	endTime = Analysis.getTotalTime();

	startUp = true;
       
	createMenus();
	getContentPane().add(getMainPanel());
       
	setGraphSpecificData();

	pack();
	setVisible(true);
    }   

    public void close(){
	super.close();
    }

    /* Show the RangeDialog to set processor numbers and interval times */
   void showDialog()
   {
       if(dialog == null) {
	   System.out.println("dialog is null, creating dialog");
	   dialog = new RangeDialog(this,"Select Range");
       }
       dialog.displayDialog();
       if(dialog.dialogState == RangeDialog.DIALOG_OK)
	   if(!startUp)
	       refreshGraph();
   }

    public void actionPerformed(ActionEvent evt)
    {
	if(evt.getSource() instanceof JMenuItem)
	    {
		JMenuItem m = (JMenuItem)evt.getSource();
		if(m.getText().equals("Set Range"))
		    showDialog();
		else if(m.getText().equals("Close"))
		    close();
	    }
    } 

    protected void setGraphSpecificData() {
	setDataSource("Avg Processor Utilization", Analysis.getBGData());
	// start from 0th interval and increase by 1 step each.
	setXAxis("Intervals Size = " + U.t(Analysis.getIntervalSize()),
		 U.t(Analysis.getIntervalSize()),0,1);
	setYAxis("Utilization", "(%)");
	refreshGraph();
    }
}

