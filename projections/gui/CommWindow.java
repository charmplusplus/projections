package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import javax.swing.*;
import projections.analysis.*;
import projections.gui.graph.*;
import projections.misc.LogEntryData;



public class CommWindow extends GenericGraphWindow 
    implements ItemListener
{
	private double[][] 	msgCount;		// rename to sentMsgCount
	private double[][] 	byteCount;		// rename to sentBytCount
	private double[][] 	recivedMsgCount;		
	private double[][]	exclusiveSent;
	private ArrayList		histogram;
	private int[]			histArray;
	private String[][]	histText;
	private String[][]	msgText;
	private String[][]	byteText;
	private String 		currentArrayName;
	private String[][]	popupText;
	private String[][]	EPNames;
	private JPanel			mainPanel;
	private JPanel			graphPanel;
	private JPanel			checkBoxPanel;
	private Checkbox		sentMssgs;
	private Checkbox		sentBytes;
	private Checkbox		histogramCB;
	private Checkbox		recivedMssgs;
	private Checkbox		sentExclusive;


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
		setPopupText("histArray");
		pack();
		setVisible(true);
	}
	
	public void itemStateChanged(ItemEvent ae){
		if(ae.getSource() instanceof Checkbox){
			setCursor(new Cursor(Cursor.WAIT_CURSOR));			
			Checkbox cb = (Checkbox)ae.getSource();
			 if(cb == histogramCB){
				//System.out.println("HistogramView");
				setDataSource("Histogram", histArray, this);
				setPopupText("histArray");
				setYAxis("Frequency", null);
				setXAxis("Byte Size", "bytes");
				super.refreshGraph();
			}else if(cb == sentMssgs){
				//System.out.println("mssgs");
				setDataSource("Communications", msgCount, this);
				setPopupText("msgCount");
				//setPopupText(msgText);
				setYAxis("Messages Sent", "");
				setXAxis("Processor", null);
				super.refreshGraph();
			}else if(cb == sentBytes){
				//System.out.println("bytes");
				setDataSource("Communications", byteCount, this);
				setPopupText("byteCount");
				setYAxis("Bytes Sent", "bytes");
				setXAxis("Processor", null);
				super.refreshGraph();
			}else if(cb == recivedMssgs){
				setDataSource("Communications", recivedMsgCount, this);
				setPopupText("recivedMsgCount");
				setYAxis("Mssages Recived", "");
				setXAxis("Processor", null);
				super.refreshGraph();
			}else if(cb == sentExclusive){
				setDataSource("Communications", exclusiveSent, this);
				setPopupText("exclusiveSent");
				setYAxis("Messages Sent Externally", "");
				setXAxis("Processor", null);
				super.refreshGraph();
			}
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	private void setPopupText(String input){
		currentArrayName = input;
	}
	
	private void setPopupText(String[][] input){
		popupText = input;
	}
	
	 public String[] getPopup(int xVal, int yVal){
	 	 //System.out.println("CommWindow.getPopup()");
		 //System.out.println(xVal +", " +yVal);
		 if( (xVal < 0) || (yVal <0) || currentArrayName==null)
			 return null;
			 
		 if(EPNames == null)
		 	EPNames = Analysis.getEntryNames();

		 String[] rString = new String[2];
		 
		 if(currentArrayName.equals("histArray")){
		 	rString[0] = xVal + " bytes";
			rString[1] = "Count = " +  histArray[xVal];
		 }else if(currentArrayName.equals("msgCount")){
		 	rString[0] = "EPid: " + EPNames[yVal][0];
			rString[1] = "Count = " + msgCount[xVal][yVal];
		 }else if(currentArrayName.equals("byteCount")){
		 	rString[0] = "EPid: " + EPNames[yVal][0];
			rString[1] = "Bytes = " + byteCount[xVal][yVal];
		 }else if(currentArrayName.equals("recivedMsgCount")){
		 	rString[0] = "EPid: " + EPNames[yVal][0];
			rString[1] = "Count = " + recivedMsgCount[xVal][yVal];
		 }else if(currentArrayName.equals("exclusiveSent")){
		 	rString[0] = "EPid: " + EPNames[yVal][0];
			rString[1] = "Count = " + exclusiveSent[xVal][yVal];
		 }

/*		
		 if(popupText != histText){
			 rString[0] = "EPid: " + EPNames[yVal][0];
			 rString[1] = popupText[xVal][yVal];
		}else{
			rString[0] = popupText[xVal][0];
			rString[1] = popupText[xVal][1];
			//System.out.println("==> " + rString[0]);
			//System.out.println("==> " + rString[1]);
		}			
		
*/
		//System.out.println("==> " + rString);
		return rString;
	 }	
	
/*	protected JPanel getMainPanel(){
		JPanel mainPanel = new JPanel();
		graphCanvas = new MyGraph();
      graphPanel = new GraphPanel(graphCanvas);
      mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));
		mainPanel.add(graphPanel);
		return mainPanel;
   }
*/	
	protected void setLayout(){
		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();
		
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.setLayout(gbl);
		
		graphPanel = getMainPanel();
		checkBoxPanel = new JPanel();
		
		CheckboxGroup cbg = new CheckboxGroup();
		histogramCB = new Checkbox("Histogram", cbg, true);
		sentMssgs = new Checkbox("Messages Sent (mssgs)", cbg, false);
		sentBytes = new Checkbox("Messages Sent (bytes)", cbg, false);
		recivedMssgs = new Checkbox("Messages Recived", cbg, false);
		sentExclusive = new Checkbox("Messages Sent Externally", cbg, false);
		
		histogramCB.addItemListener(this);
		sentMssgs.addItemListener(this);
		sentBytes.addItemListener(this);
		recivedMssgs.addItemListener(this);
		sentExclusive.addItemListener(this);
		
		Util.gblAdd(checkBoxPanel, histogramCB, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, sentMssgs, gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, sentBytes, gbc, 2,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, recivedMssgs, gbc, 3,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, sentExclusive, gbc, 4,0, 1,1, 1,1);
					
		Util.gblAdd(mainPanel, graphPanel, gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(mainPanel, checkBoxPanel, gbc, 0,2, 1,1, 0,0);
	}
		
	protected void setGraphSpecificData(){
		setXAxis("Byte Size", "");
		setYAxis("Frequency", "");
	}
	
	protected void showDialog(){
		if(dialog == null)
			dialog = new RangeDialog(this, "select Range");
		int dialogstatus = dialog.showDialog();
		
		if(dialogstatus == RangeDialog.DIALOG_OK){
			dialog.setAllData();
		}
	
		msgCount = new double[validPEs.size()][];
		byteCount = new double[validPEs.size()][];
		recivedMsgCount = new double[validPEs.size()][];
		exclusiveSent = new double[validPEs.size()][];
		msgText = new String[validPEs.size()][];
		byteText = new String[validPEs.size()][];
		getData();
		setDataSource("Histogram", histArray, this);
		super.refreshGraph();
	}
	
	protected void getData(){
		GenericLogReader glr;
		LogEntryData logdata = new LogEntryData();

		OrderedIntList peList = validPEs.copyOf();
		
		int numPe = peList.size();
		int numEPs = Analysis.getNumUserEntries();
		histogram = new ArrayList();
		
		while(peList.hasMoreElements()){
			int pe = peList.nextElement();
			glr = new GenericLogReader(Analysis.getLogName(pe), Analysis.getVersion());
			
			try{
				msgCount[pe] = new double[numEPs];
				byteCount[pe] = new double[numEPs];
				recivedMsgCount[pe] = new double[numEPs];
				exclusiveSent[pe] = new double[numEPs];
				msgText[pe] = new String[numEPs];
				byteText[pe] = new String[numEPs];
				int EPid;
				
				glr.nextEventOnOrAfter(startTime, logdata);
				
				while(true){ 		// we'll just use the EOFException to break us out of this loop :)
					glr.nextEvent(logdata);
					if(logdata.type == ProjDefs.CREATION){
						EPid = logdata.entry;
						msgCount[pe][EPid] ++;
						msgText[pe][EPid] = "Count = " + msgCount[pe][EPid];		// need to get rid of this
						byteCount[pe][EPid] += logdata.msglen;
						byteText[pe][EPid] = "Bytes: " + byteCount[pe][EPid];  	// and this too
						histogram.add(new MyArray(logdata.msglen));
					} else if (logdata.type == ProjDefs.BEGIN_PROCESSING) {
						EPid = logdata.entry;
						recivedMsgCount[pe][EPid] ++;
						if(logdata.pe == pe)
							exclusiveSent[pe][EPid] ++;
					}
				}
			
			}catch (java.io.EOFException e){
			}catch (Exception e){
				System.out.println("Exception: " +e);
				e.printStackTrace();
			}
		}
		
		
		/*while(peList.hasMoreElements()){
			int pe = peList.nextElement();
			glr = new GenericLogReader(Analysis.getLogName(pe), Analysis.getVersion());
			
			try{
				msgCount[pe] = new double[numEPs +1];
				byteCount[pe] = new double[numEPs +1];
				msgText[pe] = new String[numEPs +1];
				byteText[pe] = new String[numEPs +1];
				double tempMC, tempBC;
				glr.nextEventOnOrAfter(startTime, logdata);
				
				
				while(true){
					glr.nextEventOfType(ProjDefs.BEGIN_PROCESSING, logdata);
					int EPid = logdata.entry;
					// System.out.println(EPid);
					// tempMC = 0;
					// tempBC = 0;
					//int count = 0;
					while(true){						
						glr.nextEvent(logdata);
						if(logdata.type == ProjDefs.CREATION){
							msgCount[pe][EPid] ++;
							msgText[pe][EPid] = "Count = " + msgCount[pe][EPid];
							byteCount[pe][EPid] += logdata.msglen;
							byteText[pe][EPid] = "Bytes: " + byteCount[pe][EPid];
							//count++;
							//System.out.print(histogram.size() + " ... ");
							// System.out.println("histogram-> " +histogram.get(logdata.msglen));
							histogram.add(new MyArray(logdata.msglen));
							//histogram.add(temp);
							//System.out.println(logdata.msglen + " ... " + histogram.size());							
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
		*/

		int max = ((MyArray)histogram.get(0)).index;
		int min = ((MyArray)histogram.get(0)).index;
		
		for(int k=1; k<histogram.size(); k++){
			if(((MyArray)histogram.get(k)).index < min)
				min = ((MyArray)histogram.get(k)).index;
			if(((MyArray)histogram.get(k)).index > max)
				max = ((MyArray)histogram.get(k)).index;
		}

		histArray = new int[max+1];
		histText = new String[max+1][];
		for(int k=0; k<Array.getLength(histArray); k++){
			histArray[k] = 0;
			histText[k] = new String[2];
			histText[k][0] = "";
			histText[k][1] = "";
		}

		int index;
		for(int k=0; k<histogram.size(); k++){
			index = ((MyArray)histogram.get(k)).index;
			histArray[index] += 1;
			histText[index][0] = index + " bytes";
			histText[index][1] = "Count = " + histArray[index];
		}
		
		for(int k=0; k<numPe; k++){
			for(int j=0; j<numEPs; j++){
				exclusiveSent[k][j] = msgCount[k][j] - exclusiveSent[k][j];
				
				// AJ - i'm doing this to prevent any negitive numbers from 
				// getting sent into the stack array because it messes up
				// the drawing				
				if(exclusiveSent[k][j] < 0)
					exclusiveSent[k][j] = 0;
			}
		}
	
		/*
		for(int k=0; k<numEPs; k++){
			System.out.println(k +": " + msgText[0][k] +"  " +byteText[0][k]);
		} 
		*/
			
	}
	 
	 // Needed for Communications Histogram since it is currently being
	 // implemented via ArrayList which iwll not allow the use of a int array
	 private class MyArray {
		public int index;	
		public MyArray(int setIndex){
			index = setIndex;
		}	
	 }
	 

	
}

