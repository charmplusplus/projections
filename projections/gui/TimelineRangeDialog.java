package projections.gui;

import java.awt.*;
import java.awt.event.*;
import projections.misc.*;


public class TimelineRangeDialog extends Dialog
   implements ActionListener, TextListener
{

   private TimeTextField beginField;
   private TimeTextField endField;
   private TimeTextField lengthField;
   private SelectField   processorField;
   
   private long beginTime;
   private long endTime;
   private long totalTime;
   private long length;
   
   private IntTextField nField, pField;
   private TimelineData data;
   private List eList;
   private TextArea msgArea;
   
   private Button bOK, bCancel, bSearch, bFullRange;
   
   public TimelineRangeDialog(Frame f, TimelineData data)
   {
	  super(f, "Select Timeline Ranges", true);
	  
	  this.data = data;
	  
	  beginTime = data.beginTime;
	  endTime   = data.endTime;
	  length    = endTime - beginTime;
	  totalTime = data.totalTime;
	  
	  beginField = new TimeTextField(beginTime, 10);
	  endField   = new TimeTextField(endTime,   10);
	  lengthField= new TimeTextField(length,    10);
	  processorField = new SelectField(data.processorString, 20);
	  beginField.addActionListener(this);
	  beginField.addTextListener(this);
	  endField.addActionListener(this);
	  endField.addTextListener(this);
	  lengthField.addActionListener(this);
	  lengthField.addTextListener(this);
	  processorField.addActionListener(this);
	  
	  
	  endField.addFocusListener(new FocusAdapter()
	  {
		 public void focusLost(FocusEvent evt)
		 {
			if(evt.getSource() instanceof TimeTextField)
			{
			   if(evt.getSource() == endField)
			   {
				  endTime = endField.getValue();
				  if(endTime <= beginTime || endTime > totalTime)
				  {
					 endField.selectAll();
					 endField.requestFocus();
				  }
			   }
			}
		 }
	  });

	  
	  bOK = new Button("OK");
	  bOK.addActionListener(this);
	  bCancel = new Button("Cancel");
	  bCancel.addActionListener(this);
	  bFullRange = new Button("Select Full Time Range");
	  bFullRange.addActionListener(this);
	  
	  Label lProcessor = new Label("Processor(s): ");
	  Label lBeginTime = new Label("Begin Time: ");
	  Label lEndTime   = new Label("End Time: ");
	  Label lLength    = new Label("Length: ");
	  Label lOR1       = new Label("-OR-", Label.CENTER);
	  Label lOR2       = new Label("-OR-", Label.CENTER);
	  Label lSearch    = new Label("Search for time (see below)");
	  Label lValidT    = new Label("Valid times: 0 - " + U.t(totalTime), Label.CENTER);
	  Label lValidP    = new Label("Valid processors: 0 - " + (Analysis.getNumProcessors()-1), Label.CENTER);
	  
	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  
	  GrayPanel p1 = new GrayPanel();
	  p1.setLayout(gbl);                                
	  gbc.fill = GridBagConstraints.BOTH;
	  Util.gblAdd(p1, lProcessor,     gbc, 0,0, 1,1, 1,1, 8,8,4,0);
	  Util.gblAdd(p1, processorField, gbc, 1,0, 4,1, 1,1, 8,0,4,8);
	  Util.gblAdd(p1, lBeginTime,     gbc, 0,1, 1,1, 1,1, 4,8,0,0);
	  Util.gblAdd(p1, beginField,     gbc, 1,1, 1,1, 1,1, 4,0,0,4);
	  Util.gblAdd(p1, lOR1,           gbc, 2,1, 1,1, 1,1, 4,4,0,4);
	  Util.gblAdd(p1, lSearch,        gbc, 3,1, 2,1, 1,1, 4,4,0,8);
	  Util.gblAdd(p1, lEndTime,       gbc, 0,2, 1,1, 1,1, 0,8,4,0);
	  Util.gblAdd(p1, endField,       gbc, 1,2, 1,1, 1,1, 0,0,4,4);
	  Util.gblAdd(p1, lOR2,           gbc, 2,2, 1,1, 1,1, 0,4,4,4);
	  Util.gblAdd(p1, lLength,        gbc, 3,2, 1,1, 1,1, 0,4,4,0);
	  Util.gblAdd(p1, lengthField,    gbc, 4,2, 1,1, 1,1, 0,0,4,8); 
	  Util.gblAdd(p1, lValidT,        gbc, 0,3, 2,1, 1,1, 4,8,8,4);
	  Util.gblAdd(p1, lValidP,        gbc, 2,3, 3,1, 1,1, 4,4,8,8);
	  
	  
	  nField = new IntTextField(1, 5);
	  pField = new IntTextField(0, 5);
	  eList  = new List(5, false);
	  
	  bSearch = new Button("Search for Begin Time");
	  bSearch.addActionListener(this);
	  
	  nField.addActionListener(this);
	  pField.addActionListener(this);
	  eList.addActionListener(this);
	  
	  nField.addTextListener(this);
	  pField.addTextListener(this);
	  
	  String[][] names = Analysis.getUserEntryNames();
	  int n = Analysis.getNumUserEntries();
	  
	  for(int i=0; i<n ; i++)
		 eList.add(names[i][1] + " -- " + names[i][0]);
	  eList.select(0);   

	  Label lDesc = new Label("SEARCH FOR BEGIN TIME:");
	  
	  Label lN    = new Label("N: ", Label.RIGHT);
	  Label lE    = new Label("Entry: ");
	  Label lP    = new Label("Processor: ", Label.RIGHT);  
	  
	  String s = "Search for begin time of Nth occurrence of entry E on processor P.";
	  msgArea = new TextArea(s, 4, 40, TextArea.SCROLLBARS_NONE);
	  msgArea.setEditable(false);

	  GrayPanel p2 = new GrayPanel();
	  p2.setLayout(gbl);
	  Util.gblAdd(p2, lDesc,   gbc, 0,0, 4,1, 1,1, 8,8,2,8);
	  Util.gblAdd(p2, lN,      gbc, 0,1, 1,1, 0,1, 2,8,2,0);
	  Util.gblAdd(p2, nField,  gbc, 1,1, 1,1, 1,1, 2,0,2,4);
	  Util.gblAdd(p2, lP,      gbc, 2,1, 1,1, 0,1, 2,4,2,0);
	  Util.gblAdd(p2, pField,  gbc, 3,1, 1,1, 1,1, 2,0,2,8);
	  
	  Util.gblAdd(p2, lE,      gbc, 0,2, 4,1, 1,1, 2,8,0,8);
	  Util.gblAdd(p2, eList,   gbc, 0,3, 4,1, 1,1, 0,8,2,8);
	  gbc.fill = GridBagConstraints.NONE;
	  Util.gblAdd(p2, bSearch, gbc, 0,4, 4,1, 0,1, 2,8,2,8);
	  gbc.fill = GridBagConstraints.BOTH;
	  Util.gblAdd(p2, msgArea, gbc, 0,5, 4,1, 1,1, 2,8,8,8); 

	  Panel p3 = new Panel();
	  p3.setLayout(new FlowLayout());
	  p3.add(bFullRange);
	  p3.add(bOK);
	  p3.add(bCancel);
	  Util.gblAdd(p1, p3, gbc, 0,4, 5,1, 1,1, 0,4,4,4);
	  
	  Panel p4 = new Panel();
	  p4.setBackground(Color.gray);
	  add("Center", p4);
	  
	  
	  p4.setLayout(gbl);
	  Util.gblAdd(p4, p1, gbc, 0,0, 1,1, 1,1, 4,4,2,4);
	  Util.gblAdd(p4, p2, gbc, 0,1, 1,1, 1,1, 2,4,4,4);
	  
	  pack();
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  Object s = evt.getSource();
	  
	  if((s instanceof Button && (Button)s == bOK) || s instanceof TimeTextField || s instanceof SelectField)
	  {

		 if(endTime > beginTime)
		 {
			data.processorList   = processorField.getValue(Analysis.getNumProcessors());
			data.processorString = data.processorList.listToString();
			data.numPs           = data.processorList.size();
		   
			data.beginTime = beginField.getValue();
			data.endTime   = endField.getValue();
			data.timelineWindow.CloseRangeDialog();
		 }
		 else
		 {
			endField.selectAll();
			endField.requestFocus();
		 }
	  } 
	  else if(s instanceof Button && (Button)s == bCancel)
	  {
		 data.timelineWindow.CloseRangeDialog();
	  }
	  else if(s instanceof Button && (Button)s == bFullRange)
	  {
		 beginTime = 0;
		 endTime   = totalTime;
		 length    = endTime - beginTime;
		 beginField.setText("" + beginTime);
		 endField.setText("" + endTime);
		 lengthField.setText("" + length);
	  }
		 
	  else if(s instanceof Button && (Button)s == bSearch)
	  {
		 int n = nField.getValue();
		 int p = pField.getValue();
		 int e = eList.getSelectedIndex();
		 
		 msgArea.setText("");
		 
		 if(n < 1)
			msgArea.append("Invalid N value.  N > 0.\n");
		 if(p < 0 || p > Analysis.getNumProcessors()-1)
			msgArea.append("Invalid processor value.  0 <= P <= " + (Analysis.getNumProcessors()-1) + "\n");
		 if(e < 0)
			msgArea.append("Invalid entry selection. Select an entry.\n");
				   
		 if(n >= 1 && p >= 0 && p < Analysis.getNumProcessors() && e >= 0)
		 {
			boolean foundit = false;
			long result;

			msgArea.setText("Searching...");
			try
			{
			   result = Analysis.searchTimeline(n, p, e);
			   foundit = true;
			}
			catch(EntryNotFoundException enfe)
			{
			   foundit = false;
			   result  = 0;
			}
			msgArea.setText("");
			if(foundit && result >= 0)
			{
			   String sr;
			   sr = "Begin Time = " + result + "\n";
			   msgArea.append(sr);
			   sr = " for Occurrence " + n + "\n";
			   msgArea.append(sr);
			   sr = " of Entry " + eList.getSelectedItem() + "\n";
			   msgArea.append(sr);
			   sr = " on Processor " + p;
			   msgArea.append(sr);
			   beginField.setText("" + result);
			}
			else
			{
			   String sr;
			   sr = "Invalid N value.\n";
			   msgArea.append(sr);
			   sr = "Entry " + eList.getSelectedItem() + " occurs " +
						  (-result) + " times on processor " + p + "\n";
			   msgArea.append(sr);
			}
		 }                               
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
			o = (TimeTextField) getFocusOwner();
	  
		 if(f == beginField)
		 {
			beginTime = beginField.getValue();
			if(beginTime > totalTime - 1)
			{
			   lastCaretPosition = beginField.getCaretPosition();
			   beginTime = totalTime - 1;
			   beginField.setText("" + beginTime);
			   beginField.setCaretPosition(lastCaretPosition);
			}
			length = endTime - beginTime;
			
			if(length < 1)
			{
			   length = 1;
			   endTime = beginTime + length;
			}

			endField.setText("" + endTime);
			lengthField.setText("" + length);      
		 }   
		 else if(f == endField && o == endField)
		 {
			endTime = endField.getValue();
			if(endTime > totalTime)
			{
			   lastCaretPosition = endField.getCaretPosition();
			   endTime = totalTime;
			   endField.setText("" + endTime);
			   endField.setCaretPosition(lastCaretPosition);
			}
			length = endTime - beginTime;
			lengthField.setText("" + length);     
		 }
		 else if(f == lengthField && o == lengthField)
		 {
			length = lengthField.getValue();
		 
			if(length < 1)
			{
			   lastCaretPosition = lengthField.getCaretPosition();
			   length = 1;
			   lengthField.setText("" + length);
			   lengthField.setCaretPosition(lastCaretPosition);
			}
			else if(length > totalTime - beginTime)
			{
			   lastCaretPosition = lengthField.getCaretPosition();
			   length = totalTime - beginTime;
			   lengthField.setText("" + length);
			   lengthField.setCaretPosition(lastCaretPosition);
			}   
		  
			endTime = beginTime + length;
			endField.setText("" + endTime);
		 }
	  }
	  else if(evt.getSource() instanceof IntTextField)
	  {
		 IntTextField f = (IntTextField)evt.getSource();
		 
		 if(f == nField)
		 {
			if(nField.getValue() < 1)
			{
			   lastCaretPosition = nField.getCaretPosition();
			   nField.setText("" + 1);
			   nField.setCaretPosition(lastCaretPosition);
			}
		 }
		 else if(f == pField)
		 {
			int p = pField.getValue();
			int pmax = Analysis.getNumProcessors()-1;
			
			if(p < 0)
			{
			   lastCaretPosition = pField.getCaretPosition();
			   pField.setText("" + 0);
			   pField.setCaretPosition(lastCaretPosition);
			}
			else if(p > pmax)
			{
			   lastCaretPosition = pField.getCaretPosition();
			   pField.setText("" + pmax);
			   pField.setCaretPosition(lastCaretPosition);
			}
		 }
	  }
   }   
}