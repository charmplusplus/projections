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

	private double[][] 	msgCount;
	private double[][] 	byteCount;
	private ArrayList		histogram;
	private int[]			histArray;
	private String[][]	histText;
	private String[][]	msgText;
	private String[][]	byteText;
	private String[][]	popupText;
	private JPanel			mainPanel;
	private JPanel			graphPanel;
	private JPanel			checkBoxPanel;
	private Checkbox		sentMssgs;
	private Checkbox		sentBytes;
	private Checkbox		histogramCB;

    /**
     *  **CW** STUPID mix of old and new ... will need to standardize soon.
     *  ... if I ever get my butt to it
     */
    public CommWindow(MainWindow mainWindow) {
	this();
    }
	
	public CommWindow(){
		super("Projections Communications");
		// graphCanvas = new MyGraph();
		setGraphSpecificData();
		mainPanel = new JPanel();
    	getContentPane().add(mainPanel);
		setLayout();
		showDialog();
		setPopupText(msgText);
		pack();

		setVisible(true);
	}
	
	public void itemStateChanged(ItemEvent ae){
		if(ae.getSource() instanceof Checkbox){
			setCursor(new Cursor(Cursor.WAIT_CURSOR));			
			Checkbox cb = (Checkbox)ae.getSource();
						
			 if(cb == histogramCB){
				System.out.println("HistogramView");
				
				setDataSource("Histogram", histArray);
				setPopupText(histText);
				setYAxis("Frequency", null);
				setXAxis("Byte Size", "bytes");
				super.refreshGraph();
			}else if(cb == sentMssgs){
				System.out.println("mssgs");
				setDataSource("Communications", msgCount);
				setPopupText(msgText);
				setYAxis("Messages Sent", "");
				setXAxis("Processor", null);
				super.refreshGraph();
			}else if(cb == sentBytes){
				System.out.println("bytes");
				setDataSource("Communications", byteCount);
				setPopupText(byteText);
				setYAxis("Bytes Sent", "bytes");
				setXAxis("Processor", null);
				super.refreshGraph();
			}
			
			
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	private void setPopupText(String[][] input){
		popupText = input;
		
		if(popupText == histText)
			System.out.println("Histogram Text Set");
	}
	
	protected JPanel getMainPanel(){
		JPanel mainPanel = new JPanel();
		graphCanvas = new MyGraph();
      graphPanel = new GraphPanel(graphCanvas);
      mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));
		mainPanel.add(graphPanel);
		return mainPanel;
   }
	
	protected void setLayout(){
		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();
		
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.setLayout(gbl);
		
		graphPanel = getMainPanel();
		checkBoxPanel = new JPanel();
		
		CheckboxGroup cbg = new CheckboxGroup();
		histogramCB = new Checkbox("Histogram", cbg, false);
		sentMssgs = new Checkbox("Messages Sent (mssgs)", cbg, true);
		sentBytes = new Checkbox("Messages Sent (bytes)", cbg, false);
		
		histogramCB.addItemListener(this);
		sentMssgs.addItemListener(this);
		sentBytes.addItemListener(this);
		
		Util.gblAdd(checkBoxPanel, histogramCB, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, sentMssgs, gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, sentBytes, gbc, 2,0, 1,1, 1,1);
					
		Util.gblAdd(mainPanel, graphPanel, gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(mainPanel, checkBoxPanel, gbc, 0,2, 1,1, 0,0);
		
		
		
	
	/*
		checkBoxPanel.setLayout(new GridLayout(2, 1));
		checkBoxPanel.add(getMainPanel());
		CheckboxGroup cbg = new CheckboxGroup();
		checkBoxPanel.add(new Checkbox("mssgs", cbg, true));
		checkBoxPanel.add(new Checkbox("bytes", cbg, false));
	*/
	}
	
	public String[][] getPopupText(){
		return popupText;
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
		msgText = new String[dialog.numProcessors][];
		byteText = new String[dialog.numProcessors][];
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
		histogram = new ArrayList();
		
		while(peList.hasMoreElements()){
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
				
				
/*				// Is this the best way? I have to read twice, but wont have to read again
				// The other choice would be to sort on read using a default size bin, but 
				// for each time the user wishes to change the bin size, we'll need to reread
				// Will a user want to change bin size? 
				histogram = new int[count];
				count = 0;
				glr.nextEventOnOrAfter(startTime, logdata);
				while(true){
					grl.nextEvent(logdata);
					if(logdata.type == ProjDefs.CREATION){
						histogram[count] = logdata.msglen;
					}else if(logdata.type == ProjDefs.END_PROCESSING){
						break;
					}
				}
*/				
				// Do we allready have a sorting routine writting?  quickSort(histogram);
				
			}catch(java.io.EOFException e){
			}catch(Exception e){
				System.out.println("Exception: " +e);
				e.printStackTrace();
			}
			
		}

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
	
		/*
		for(int k=0; k<numEPs; k++){
			System.out.println(k +": " + msgText[0][k] +"  " +byteText[0][k]);
		} 
		*/
			
	}


	 private class MyGraph extends Graph{
		 public String[] getPopup(int xVal, int yVal){
		 	 String[][] popupText = getPopupText();
			 if( (xVal < 0) || (yVal <0) || popupText==null)
				 return null;
				 
			 String[] rString = new String[2];
			 
			 if(popupText != histText){
				 rString[0] = "EPid: " + yVal;
				 rString[1] = popupText[xVal][yVal];
			}else{
				rString[0] = popupText[xVal][0];
				rString[1] = popupText[xVal][1];
			}			 
			 
			 return rString;
		 }	
	 }
	 
	 private class MyArray {
		public int index;	
		public MyArray(int setIndex){
			index = setIndex;
		}	
	 }
	 

	
}

