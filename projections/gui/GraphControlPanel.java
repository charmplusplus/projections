package projections.gui;

import java.awt.*;
import java.awt.event.*;
import projections.misc.*;

public class GraphControlPanel extends Panel
   implements ActionListener, ItemListener
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;


   private Button       bPlus5;
   private Button       bPlus1;
   private Button       bMinus1;
   private Button       bMinus5;
   private Button       bSelectAll;
   private Button       bApply;
   private Checkbox     cbXInterval;
   private Checkbox     cbXProcessor;
   private Checkbox     cbYMsg;
   private Checkbox     cbYTime;
   private GraphData    data; 
   private Label        lIterate;
   private Label        lSelect;
   private JSelectField  rangeField;

   private ErrorDialog      errDlg;
   private OrderedIntList  origProcList;    // original proc list
   
   public GraphControlPanel()
   {
	  setBackground(Color.lightGray);
	  
	  CheckboxGroup cbgXAxis = new CheckboxGroup();
	  CheckboxGroup cbgYAxis = new CheckboxGroup();
	  cbXInterval  = new Checkbox("Interval",  true,  cbgXAxis);
	  cbXProcessor = new Checkbox("Processor", false, cbgXAxis);
	  cbYMsg       = new Checkbox("Msgs",      true,  cbgYAxis);
	  cbYTime      = new Checkbox("Time",      false, cbgYAxis);
	  bPlus5       = new Button(">>");
	  bPlus1       = new Button(">");
	  bMinus1      = new Button("<");
	  bMinus5      = new Button("<<");
	  bSelectAll   = new Button("Select All");
	  bApply       = new Button("Apply");
	  
	  cbXInterval.addItemListener (this);
	  cbXProcessor.addItemListener(this);
	  cbYMsg.addItemListener      (this);
	  cbYTime.addItemListener     (this);
	  bPlus5.addActionListener    (this);
	  bPlus1.addActionListener    (this);
	  bMinus1.addActionListener   (this);
	  bMinus5.addActionListener   (this);
	  bSelectAll.addActionListener(this);
	  bApply.addActionListener    (this);
	 
	  GridBagLayout gbl      = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  setLayout(gbl);
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  
	  LWPanel p1 = new LWPanel();
	  LWPanel p2 = new LWPanel();
	  LWPanel p3 = new LWPanel();

	  p1.setLayout(gbl);
	  p2.setLayout(gbl);
	  p3.setLayout(gbl);
	  gbc.insets = new Insets(3,3,3,3);
	  Util.gblAdd(this,    p1, gbc, 0, 0, 1, 1, 1, 1);
	  Util.gblAdd(this,    p2, gbc, 1, 0, 1, 1, 3, 1);
	  Util.gblAdd(this,    p3, gbc, 2, 0, 1, 1, 2, 1); 
	  
	  gbc.insets = new Insets(0,0,0,0);
	  Util.gblAdd(p1, new Label("X-AXIS", Label.CENTER), gbc, 0, 0, 1, 1, 1, 1);
	  Util.gblAdd(p1, new Label("Y-AXIS", Label.CENTER), gbc, 1, 0, 1, 1, 1, 1);
	  Util.gblAdd(p1, cbXInterval,  gbc, 0, 1, 1, 1, 1, 1);
	  Util.gblAdd(p1, cbXProcessor, gbc, 0, 2, 1, 1, 1, 1);
	  Util.gblAdd(p1, cbYMsg,       gbc, 1, 1, 1, 1, 1, 1);
	  Util.gblAdd(p1, cbYTime,      gbc, 1, 2, 1, 1, 1, 1);
	  
	  lIterate = new Label("", Label.CENTER);
	  Util.gblAdd(p2, lIterate, gbc, 2, 0, 4, 1, 1, 1);
	  Util.gblAdd(p2, new Label("-5", Label.CENTER), gbc, 2, 1, 1, 1, 1, 1);
	  Util.gblAdd(p2, new Label("-1", Label.CENTER), gbc, 3, 1, 1, 1, 1, 1);
	  Util.gblAdd(p2, new Label("+1", Label.CENTER), gbc, 4, 1, 1, 1, 1, 1);
	  Util.gblAdd(p2, new Label("+5", Label.CENTER), gbc, 5, 1, 1, 1, 1, 1);
	  Util.gblAdd(p2, bMinus5,  gbc, 2, 2, 1, 1, 1, 1);
	  Util.gblAdd(p2, bMinus1,  gbc, 3, 2, 1, 1, 1, 1);
	  Util.gblAdd(p2, bPlus1,   gbc, 4, 2, 1, 1, 1, 1);
	  Util.gblAdd(p2, bPlus5,   gbc, 5, 2, 1, 1, 1, 1);
			
	  lSelect = new Label("",  Label.CENTER);
	  String selectFormat = "Format: #,#-#,#,#,#-#,etc...";

	  rangeField = new JSelectField("", 12);
	  Util.gblAdd(p3, lSelect,    gbc, 6, 0, 4, 1, 1, 1);
	  Util.gblAdd(p3, rangeField, gbc, 6, 1, 4, 1, 1, 1); 
	  Util.gblAdd(p3, bSelectAll, gbc, 6, 2, 2, 1, 1, 1); 
	  Util.gblAdd(p3, bApply,     gbc, 8, 2, 2, 1, 1, 1);
		 
	  rangeField.setText(selectFormat);
	  rangeField.addActionListener(this);

   }   
   public void actionPerformed(ActionEvent evt)
   {
	  if(data == null)
		 return;
		 
	  if(evt.getSource() instanceof Button)
	  {
	  
		 Button c = (Button) evt.getSource();

		 if(c == bPlus5 || c == bPlus1 || c == bMinus1 || c == bMinus5)
		 {
			int delta = 0;
			if(c == bPlus5)       delta = 5;
			else if(c == bPlus1)  delta = 1;
			else if(c == bMinus1) delta = -1;
			else if(c == bMinus5) delta = -5;      
		 
			int x = 0;
		 
			if(data.xmode == GraphData.PROCESSOR)
			{
			   data.interval.list.reset();
			   x = data.interval.list.currentElement();
			   x = (x + delta) % data.interval.num;
			   if(x < 0) x += data.interval.num;
			}
			else   
			{
			   // go through the saved origProcList instead
			   // of data.processor.list because it is 
			   // already changed to the current selection.
			   if (delta < 0) delta += origProcList.size();
			   x = origProcList.currentElement();
			   for (int i=0; i<delta; i++) {
			     x = origProcList.nextElement();
			     if (x==-1) x = origProcList.nextElement();
			   }
/*  gzheng
			   data.processor.list.reset();
			   x = data.processor.list.currentElement();
			   x = (x + delta) % data.processor.num;
			   if(x < 0) x += data.processor.num;
*/
			}
			rangeField.setText(x + "");         
		 }
		 else if(c == bSelectAll)
		 {
			int x = 0;
			
			if(data.xmode == GraphData.PROCESSOR) {
			   x = data.interval.num - 1;
			   rangeField.setText("0-" + x);
			}
			else  {
//			   x = data.processor.num - 1;
			   rangeField.setText(origProcList.listToString());
			}
			
		 }   
	  }
	  if(data.xmode == GraphData.PROCESSOR) 
	  {
		 data.interval.list   = rangeField.getValue(data.interval.num);
		 data.interval.string = rangeField.getText(); 
		 data.setData();
	  }
	  else
	  {
//		 data.processor.list   = rangeField.getValue(data.processor.num);
		 data.processor.list   = rangeField.getValue(MainWindow.runObject[myRun].getNumProcessors());
		 if (!origProcList.contains(data.processor.list)) {
		   System.out.println("Invalid processor range. ");
	  	   // create a error dialog
		   if (errDlg == null)
          	     errDlg = new ErrorDialog(data.graphWindow,"Processors Range Beyond Scope!");
		   if (!errDlg.isShowing()) errDlg.setVisible(true);
		   rangeField.setText(origProcList.listToString());
		   return;
		 }
		 data.processor.string = rangeField.getText(); 
		 data.setData();
	  }
	  
	  data.displayPanel.setAllBounds();
	  data.displayPanel.UpdateDisplay();   
   }   
   public void itemStateChanged(ItemEvent evt)
   {
	  if(data == null)
		 return;
	  
	  Checkbox c = (Checkbox) evt.getSource();
	  
	  if(c == cbXProcessor)
		 setXMode(GraphData.PROCESSOR);
	  else if(c == cbXInterval)
		 setXMode(GraphData.INTERVAL);
	  else if(c == cbYMsg)
		 setYMode(GraphData.MSGS);
	  else if(c == cbYTime)
		 setYMode(GraphData.TIME);
   }   
   public void paint(Graphics g)
   {
	  g.setColor(Color.lightGray);
	  g.fillRect(0, 0, getSize().width, getSize().height);
	  g.setColor(Color.black);
	  g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
	  
	  super.paint(g);
   }   
   public void setGraphData(GraphData data)
   {
	  this.data = data;
	  this.origProcList = data.processor.list;
   }   
   public void setXMode(int mode)
   {
	  if(data == null)
		 return;   
		 
	  data.xmode = mode;
	  if(mode == GraphData.PROCESSOR)
	  {
		 if(cbXProcessor.getState() != true) cbXProcessor.setState(true);
		 lIterate.setText("ITERATE INTERVALS");
		 lSelect.setText ("SELECT INTERVALS (0-" + (data.interval.num-1) + ")");
		 rangeField.setText(data.interval.string);
	  }
	  else
	  {
		 if(cbXInterval.getState() != true) cbXInterval.setState(true);
		 lIterate.setText("ITERATE PROCESSORS");
		 lSelect.setText ("SELECT PROCESSORS (" + origProcList.listToString() +")");
		 rangeField.setText(data.processor.string);
	  }
	  data.displayPanel.setAllBounds();
	  data.displayPanel.UpdateDisplay();
   }   
   public void setYMode(int mode)
   {
	  if(data == null)
		 return; 
		 
	  if(mode == GraphData.TIME && cbYTime.getState() != true)
		 cbYTime.setState(true);
	  else if(mode == GraphData.MSGS && cbYMsg.getState() != true)
		 cbYMsg.setState(true);
				 
	  data.ymode = mode;
	  data.displayPanel.setAllBounds();
	  data.displayPanel.UpdateDisplay();
	  data.legendPanel.UpdateLegend();
   }   
}

