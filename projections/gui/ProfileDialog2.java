package projections.gui;

import java.awt.*;
import java.awt.event.*;
import projections.misc.*;


public class ProfileDialog2 extends Dialog
   implements ActionListener, TextListener
{

   private TimeTextField beginField;
   private TimeTextField endField;
   private TimeTextField lengthField;
   private SelectField   processorField;
   private SelectField phasesField;
   
   private long beginTime;
   private long endTime;
   private long totalTime;
   private long length;
   
   private ProfileData data;
   
   private Button bOK, bCancel, bFullRange;
   
   public ProfileDialog2(Frame f, ProfileData data)
   {
	  super(f, "Select Processor and Time Ranges", true);
	  
	  this.data = data;


	  beginTime = data.begintime;
	  endTime   = data.endtime;
	  length    = endTime - beginTime;
	  totalTime = Analysis.getTotalTime();
	  
	  beginField = new TimeTextField(beginTime, 10);
	  endField   = new TimeTextField(endTime,   10);
	  lengthField= new TimeTextField(length,    10);
	  processorField = new SelectField(data.pstring, 20);
	  phasesField = new SelectField(data.pstring, 20);

	  beginField.addActionListener(this);
	  beginField.addTextListener(this);
	  endField.addActionListener(this);
	  endField.addTextListener(this);
	  lengthField.addActionListener(this);
	  lengthField.addTextListener(this);
	  processorField.addActionListener(this);
	  phasesField.addActionListener(this);

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
	  Label lValidT    = new Label("Valid times: 0 - " + U.t(totalTime), Label.CENTER);
	  Label lValidP    = new Label("Valid processors: 0 - " + (Analysis.getNumProcessors()-1), Label.CENTER);
	  Label lPhases    = new Label("Phase(s): ");

	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  
	  GrayPanel p1 = new GrayPanel();
	  p1.setLayout(gbl);                                
	  gbc.fill = GridBagConstraints.BOTH;
	  Util.gblAdd(p1, lProcessor,     gbc, 0,0, 1,1, 1,1, 8,8,4,0);
	  Util.gblAdd(p1, processorField, gbc, 1,0, 4,1, 1,1, 8,0,4,8);
	  Util.gblAdd(p1, lPhases,        gbc, 0,1, 1,1, 1,1, 8,8,4,0);
	  Util.gblAdd(p1, phasesField,    gbc, 1,1, 1,1, 1,1, 4,0,0,4);
	  Util.gblAdd(p1, lBeginTime,     gbc, 0,2, 1,1, 1,1, 4,8,0,0);
	  Util.gblAdd(p1, beginField,     gbc, 1,2, 1,1, 1,1, 4,0,0,4);
	  Util.gblAdd(p1, lOR1,           gbc, 2,2, 1,1, 1,1, 4,4,0,4);
	  Util.gblAdd(p1, lEndTime,       gbc, 0,3, 1,1, 1,1, 0,8,4,0);
	  Util.gblAdd(p1, endField,       gbc, 1,3, 1,1, 1,1, 0,0,4,4);
	  Util.gblAdd(p1, lOR2,           gbc, 2,3, 1,1, 1,1, 0,4,4,4);
	  Util.gblAdd(p1, lLength,        gbc, 3,3, 1,1, 1,1, 0,4,4,0);
	  Util.gblAdd(p1, lengthField,    gbc, 4,3, 1,1, 1,1, 0,0,4,8); 

	  Util.gblAdd(p1, lValidT,        gbc, 0,4, 2,1, 1,1, 4,8,8,4);
	  Util.gblAdd(p1, lValidP,        gbc, 2,4, 3,1, 1,1, 4,4,8,8);
	 
	  Panel p3 = new Panel();
	  p3.setLayout(new FlowLayout());
	  p3.add(bFullRange);
	  p3.add(bOK);
	  p3.add(bCancel);
	  Util.gblAdd(p1, p3, gbc, 0,5, 5,1, 1,1, 0,4,4,4);
 
	  Panel p4 = new Panel();
	  p4.setBackground(Color.gray);
	  add("Center", p4);
	  
	  p4.setLayout(gbl);
	  Util.gblAdd(p4, p1, gbc, 0,0, 1,1, 1,1, 4,4,2,4);
	  
	  pack();
   }   
   public void actionPerformed(ActionEvent evt)
   {
	 
	  Object s = evt.getSource();
	  
	  if((s instanceof Button && (Button)s == bOK) || s instanceof TimeTextField || s instanceof SelectField)
	  {
		 if(endTime > beginTime)
		 {
			data.plist     = processorField.getValue(Analysis.getNumProcessors());
	    data.phaselist = phasesField.getValue(Analysis.getNumPhases());
			data.pstring   = data.plist.listToString();
			data.begintime = beginField.getValue();
			data.endtime   = endField.getValue();
			data.profileWindow.CloseDialog();
		 }
		 else
		 {
			endField.selectAll();
			endField.requestFocus();
		 }
	  } 
	  else if(s instanceof Button && (Button)s == bCancel)
	  {
		 data.profileWindow.CancelDialog();
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
   }   
}