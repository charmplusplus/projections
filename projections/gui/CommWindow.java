package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import projections.analysis.*;
import projections.misc.LogEntryData;



public class CommWindow extends GenericGraphWindow 
						implements ItemListener
{

	private double[][] 	msgCount;
	private double[][] 	byteCount;
	private JPanel		mainPanel;
	private JPanel		graphPanel;
	private JPanel		checkBoxPanel;
	private Checkbox	mssgs;
	private Checkbox	bytes;

    /**
     *  **CW** STUPID mix of old and new ... will need to standardize soon.
     *  ... if I ever get my butt to it
     */
    public CommWindow(MainWindow mainWindow) {
	this();
    }
	
	public CommWindow(){
		super("Projections Communications");
		setGraphSpecificData();
		mainPanel = new JPanel();
    	getContentPane().add(mainPanel);
		setLayout();
		showDialog();
		
		pack();

		setVisible(true);
	}
	
	public void itemStateChanged(ItemEvent ae){
		if(ae.getSource() instanceof Checkbox){
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			
			Checkbox cb = (Checkbox)ae.getSource();
			
			if(cb == mssgs){
				setDataSource("Communications", msgCount);
				setYAxis("Messages Sent", "");
				super.refreshGraph();
			}else if(cb == bytes){
				setDataSource("Communications", byteCount);
				setYAxis("Bytes Sent", "bytes");
				super.refreshGraph();
			}
			
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	protected void setLayout(){
		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();
		
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.setLayout(gbl);
		
		graphPanel = getMainPanel();
		checkBoxPanel = new JPanel();
		
		CheckboxGroup cbg = new CheckboxGroup();
		mssgs = new Checkbox("mssgs", cbg, true);
		bytes = new Checkbox("bytes", cbg, false);
		
		mssgs.addItemListener(this);
		bytes.addItemListener(this);
		
		Util.gblAdd(checkBoxPanel, mssgs,
					gbc, 0,0,  1,1,  1,1);
		Util.gblAdd(checkBoxPanel, bytes,
					gbc, 1,0,  1,1,  1,1);
					
					
		Util.gblAdd(mainPanel, graphPanel,
				    gbc, 0, 0, 1,1, 1,1);
		Util.gblAdd(mainPanel, checkBoxPanel,
					gbc, 0, 1, 1,1, 0,0);
		
		
		
	
	/*
		checkBoxPanel.setLayout(new GridLayout(2, 1));
		checkBoxPanel.add(getMainPanel());
		CheckboxGroup cbg = new CheckboxGroup();
		checkBoxPanel.add(new Checkbox("mssgs", cbg, true));
		checkBoxPanel.add(new Checkbox("bytes", cbg, false));
	*/
	}
	
	protected void setGraphSpecificData(){
		setXAxis("Processor", "");
		setYAxis("Messages Sent", "");
	}
	
	protected void showDialog(){
		
		if(dialog == null)
			dialog = new RangeDialog(this, "select Range");
		int dialogstatus = dialog.showDialog();
		
		if(dialogstatus == RangeDialog.DIALOG_OK){
			dialog.setAllData();
		}
	
		msgCount = new double[dialog.numProcessors][];
		byteCount = new double[dialog.numProcessors][];
		
		getData();
		refreshGraph();
	}

	public void refreshGraph(){
		setDataSource("Communcations", msgCount);
		super.refreshGraph();
	}
	
	protected void getData(){
		GenericLogReader glr;
		LogEntryData logdata = new LogEntryData();

		OrderedIntList peList = validPEs.copyOf();
		
		int numPe = peList.size();
		int numEPs = Analysis.getNumUserEntries();
		
		while(peList.hasMoreElements()){
			int pe = peList.nextElement();
			glr = new GenericLogReader(Analysis.getLogName(pe), Analysis.getVersion());
			
			try{
				msgCount[pe] = new double[numEPs +1];
				byteCount[pe] = new double[numEPs +1];
				double tempMC, tempBC;
				
				
				glr.nextEventOnOrAfter(startTime, logdata);
				
				while(true){
					glr.nextEventOfType(ProjDefs.BEGIN_PROCESSING, logdata);
					int EPid = logdata.entry;
					System.out.println(EPid);
					// tempMC = 0;
					// tempBC = 0;
					
					while(true){						
						glr.nextEvent(logdata);
						
						// right now, i'm going to ignore checking that the logEvent gotten is the same as what I thought it should be, but I shouldn't
						/*
						if(logdata.type == ProjDefs.END_PROCESSING){
							if(logdata.event == EPid){
								msgCount[pe][EPid] += tempMC;
								byteCount[pe][EPid] += tempBC;
							}else{
							 	msgCount[pe][numEPs] += tempMC;
								byteCount[pe][numEPs] += tempBC;
							}							
							break;
						}else if(logdata.type == ProjDefs.CREATION){
							tempMC++;
							tempBC+=logdata.msglen;
						}else if(logdata.type == ProjDefs.BEGIN_PROCESSING){
							msgCount[pe][numEPs] += tempMC;
							byteCount[pe][numEPs] += tempBC;
							EPid = logdata.entry;
							tempMC = 0;
							tempBC = 0;
						}
						*/
						if(logdata.type == ProjDefs.CREATION){
							msgCount[pe][EPid] ++;
							byteCount[pe][EPid] += logdata.msglen;
						}else if(logdata.type == ProjDefs.END_PROCESSING){
							break;
						}
						
					}
					
				}	
			}catch(java.io.EOFException e){
			}catch(Exception e){
				System.out.println("Exception: " +e);
				e.printStackTrace();
			}
			
		}
		/*
		for(int k=0; k<numEPs; k++){
			System.out.println(msgCount[0][k] +"  " +byteCount[0][k]);
		} 
		*/
			
	}
}


