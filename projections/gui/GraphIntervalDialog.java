package projections.gui;

import java.awt.*;
import java.awt.event.*;


public class GraphIntervalDialog extends Dialog
   implements TextListener , ActionListener
{
   private GraphWindow graphWindow;
   private IntTextField numField;
   private TimeTextField sizeField;
   private SelectField   processorsField;
   
   private long intervalSize, totalTime;
   private int numIntervals;
   private OrderedIntList processorList;
   private String processorString;
   
   private Button bOK, bCancel;
	  
   public GraphIntervalDialog(GraphWindow graphWindow, int nI, long iSize, String pStr)
   {
	  super((Frame)graphWindow, "Set Interval Size", true);
		
	  numIntervals = nI;
	  intervalSize = iSize;
	  
	  this.graphWindow = graphWindow;

	  addWindowListener(new WindowAdapter()
	  {
		 public void windowClosing(WindowEvent e)
		 {
			setVisible(false);
			dispose();
		 }
	  });
	  
	  totalTime    = Analysis.getTotalTime();

	  if(numIntervals <= 0)  {
	    numIntervals = 50;
	    if(numIntervals > totalTime)
		 numIntervals = (int)totalTime;
	    intervalSize = totalTime / numIntervals;
	    intervalSize=U.makeEven(intervalSize);
	    numIntervals=(int)(totalTime/intervalSize+1);
	  }
	  
          int numProcs = Analysis.getNumProcessors();
	  if (pStr == null)
		processorString = "0-"+Integer.toString(numProcs-1);
	  else
		processorString = pStr;

	  sizeField = new TimeTextField(intervalSize, 12);
	  numField  = new IntTextField(numIntervals, 12);
          processorsField = new SelectField(processorString, 12);
	  
	  sizeField.addTextListener(this);
	  numField.addTextListener (this);
          processorsField.addTextListener(this);
	  sizeField.addActionListener(this);
	  numField.addActionListener(this);
          processorsField.addActionListener(this);
	  
	  Panel p1 = new Panel();
	  Panel p2 = new Panel();
	  
	  GridBagLayout      gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  p1.setLayout(gbl);
	  p2.setLayout(new FlowLayout());
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  gbc.insets = new Insets(5, 5, 5, 5);
	  
          int row = 0;
	  Util.gblAdd(p1, new Label("Total Time:", Label.RIGHT), gbc, 0, row, 1, 1, 1, 1);
	  Util.gblAdd(p1, new Label(U.t(totalTime), Label.RIGHT), gbc, 1, row, 1, 1, 1, 1);
	  row ++;
	  
	  Util.gblAdd(p1, new Label("Total Processors:", Label.RIGHT), gbc, 0, row, 1, 1, 1, 1);
	  Util.gblAdd(p1, new Label(Integer.toString(numProcs), Label.RIGHT), gbc, 1, row, 1, 1, 1, 1);
	  row ++;
	  
	  Util.gblAdd(p1, new Label("Processors:", Label.RIGHT), gbc, 0, row, 1, 1, 1, 1);
	  Util.gblAdd(p1, processorsField,                       gbc, 1, row, 1, 1, 1, 1);
	  row ++;

	  Util.gblAdd(p1, new Label("Interval Size:", Label.RIGHT), gbc, 0, row, 1, 1, 1, 1);
	  Util.gblAdd(p1, sizeField,                                gbc, 1, row, 1, 1, 1, 1);
	  row ++;
	  
	  Util.gblAdd(p1, new Label("# of Intervals:", Label.RIGHT), gbc, 0, row, 1, 1, 1, 1);
	  Util.gblAdd(p1, numField,                                 gbc, 1, row, 1, 1, 1, 1); 
	  row ++;
	  
	  bOK     = new Button("OK");
	  bCancel = new Button("Cancel");
	  
	  p2.add(bOK);
	  p2.add(bCancel);
	  
	  bOK.addActionListener    (this);
	  bCancel.addActionListener(this);
	  
	  add(p1, "Center");
	  add(p2, "South" );
	  
	  pack();
	  setResizable(false);
	  
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  if(evt.getSource() instanceof Button)
	  {
		 Button b = (Button) evt.getSource();
	  
		 if(b == bOK)
		 {
			graphWindow.setIntervalSize(intervalSize);
			graphWindow.setNumIntervals(numIntervals);
                        processorList = processorsField.getValue(Analysis.getNumProcessors());
			processorString = processorList.listToString();
			graphWindow.setProcessorRange(processorList);
		 }
		 setVisible(false);
		 dispose();
	  }
	  else
	  {
		 graphWindow.setIntervalSize(intervalSize);
		 graphWindow.setNumIntervals(numIntervals);
                 processorList = processorsField.getValue(Analysis.getNumProcessors());
		 processorString = processorList.listToString();
		 graphWindow.setProcessorRange(processorList);
		 setVisible(false);
		 dispose();
	  }     
   }   
   public void textValueChanged(TextEvent evt)
   {
	  int lastCaretPosition;
	  
	  if(evt.getSource() instanceof TimeTextField)
	  {
		 TimeTextField f = (TimeTextField) evt.getSource();
		 TimeTextField o = null;
		 if(getFocusOwner() instanceof TimeTextField)
			o = (TimeTextField)getFocusOwner();

		 if(f == sizeField && o == sizeField)
		 {
			intervalSize = sizeField.getValue();
			if(intervalSize > totalTime)
			{
			   lastCaretPosition = sizeField.getCaretPosition();
			   intervalSize = totalTime;
			   sizeField.setText("" + intervalSize);
			   sizeField.setCaretPosition(lastCaretPosition);
			}
			if(intervalSize != 0)
			{
			   numIntervals = (int)Math.ceil((double)totalTime / intervalSize);
			   numField.setText("" + numIntervals);
			}   
		 }
			
	  }
	  else if(evt.getSource() instanceof IntTextField)
	  {
		 IntTextField f = (IntTextField) evt.getSource();
		 IntTextField o = null;
		 if(getFocusOwner() instanceof IntTextField)
			o = (IntTextField)getFocusOwner();

		 if(f == numField && o == numField)
		 {
			numIntervals = (int)numField.getValue();
		 
			if(numIntervals != 0)
			{
			   if(numIntervals > totalTime)
			   {
				  lastCaretPosition = numField.getCaretPosition();
				  numIntervals = (int)totalTime;
				  numField.setText("" + numIntervals);
				  numField.setCaretPosition(lastCaretPosition);
			   }
			   intervalSize = totalTime / numIntervals;
			   sizeField.setText("" + intervalSize);
			}
		 }
	  }
   }   
}
