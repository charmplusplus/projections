package projections.gui;

import java.awt.*;
import java.awt.event.*;


public class LogFileViewerDialog extends Dialog
   implements ActionListener, TextListener
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;
 
	private static final long serialVersionUID = 1L;

private IntTextField  textField;
 
   private int pmax;
   
   private Button bOK, bCancel;
   private LogFileViewerWindow logFilesWindow;
   
   public LogFileViewerDialog(LogFileViewerWindow logFilesWindow)
   {
	  super((Frame)logFilesWindow, "Select Log File", true);
	  
	  this.logFilesWindow = logFilesWindow;
	  
	  pmax = MainWindow.runObject[myRun].getNumProcessors() - 1;
	  Label l1 = new Label("Select a processor to view the log file for.");
	  Label l2 = new Label("Valid processors: (0 - " + pmax + ")");
	  
	  textField = new IntTextField(0, 5);
	  textField.addTextListener(this);
	  textField.addActionListener(this);
	  
	  bOK = new Button("OK");
	  bCancel = new Button("Cancel");
	  
	  bOK.addActionListener(this);
	  bCancel.addActionListener(this); 
	  
	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  setLayout(gbl);

	  Util.gblAdd(this, l1,        gbc, 0,0, 1,1, 1,1, 5,5,0,5);
	  Util.gblAdd(this, l2,        gbc, 0,1, 1,1, 1,1, 0,5,5,5);
	  Util.gblAdd(this, textField, gbc, 0,2, 1,1, 1,1, 5,5,5,5);
	  
	  Panel p = new Panel();
	  p.add(bOK);
	  p.add(bCancel);
	  Util.gblAdd(this, p, gbc, 0,3, 1,1, 1,1, 5,5,5,5);
	  
	  pack();
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  if(evt.getSource() instanceof Button)
	  {
		 Button b = (Button)evt.getSource();
		 
		 if(b == bOK)
		 {
			logFilesWindow.setLogFileNum(textField.getValue());
		 }
		 
		 logFilesWindow.CloseDialog();
	  }
	  else if(evt.getSource() instanceof IntTextField)
	  {
		 logFilesWindow.setLogFileNum(textField.getValue());
		 logFilesWindow.CloseDialog();
	  }             
   }   
   public void textValueChanged(TextEvent evt)
   {
	  if(evt.getSource() instanceof IntTextField)
	  {
		 int lastCaretPosition;
		 IntTextField f = (IntTextField)evt.getSource();
		 
		 if(f.getValue() < 0)
		 {
			lastCaretPosition = f.getCaretPosition();
			f.setText("" + 0);
			f.setCaretPosition(lastCaretPosition);
		 }
		 else if(f.getValue() > pmax)
		 {
			lastCaretPosition = f.getCaretPosition();
			f.setText("" + pmax);
			f.setCaretPosition(lastCaretPosition);
		 }   
	  }
   }   
}
