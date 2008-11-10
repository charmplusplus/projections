package projections.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;


/** 
 * This class displays a dialog box requesting a PE from the user. 
 * The PE(processor) index is constrained to be valid
 */

public class LogFileViewerDialog extends JDialog implements ActionListener, TextListener
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	private static final long serialVersionUID = 1L;

	private IntTextField  textField;

	private int pmax;

	private JButton bOK, bCancel;
	private LogFileViewerWindow logFilesWindow;

	public LogFileViewerDialog(LogFileViewerWindow logFilesWindow)
	{
//		super((Frame)logFilesWindow, "Select Log File", true);
		this.setTitle("Select PE");
		
		this.logFilesWindow = logFilesWindow;

		pmax = MainWindow.runObject[myRun].getNumProcessors() - 1;
		JLabel l = new JLabel("<html><body><font size=+1>Enter a processor whose log file will be displayed.</font><p><font size=+1>Valid processors: (0 - " + pmax + ")</font></body></html>");

		textField = new IntTextField(0, 5);
		textField.addTextListener(this);
		textField.addActionListener(this);

		bOK = new JButton("Load Log File");
		bCancel = new JButton("Cancel");

		bOK.addActionListener(this);
		bCancel.addActionListener(this); 

		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.BOTH;
		setLayout(gbl);

		Util.gblAdd(this, l,        gbc, 0,0, 1,1, 1,1, 5,5,0,5);
		Util.gblAdd(this, textField, gbc, 0,2, 1,1, 1,1, 5,5,5,5);

		Panel p = new Panel();
		p.add(bOK);
		p.add(bCancel);
		Util.gblAdd(this, p, gbc, 0,3, 1,1, 1,1, 5,5,5,5);

		pack();
	}   


	/** Handle the button clicks */
	public void actionPerformed(ActionEvent evt)
	{
		if(evt.getSource() == bOK) {

			// Display the waiting cursor
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			
			logFilesWindow.setLogFileNum(textField.getValue());
			
			// Display the usual cursor
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			
			logFilesWindow.closeDialog();
		} 
		else if (evt.getSource() == bCancel) {
			logFilesWindow.closeDialog();
		}
	}   

	/** Constrain the PE entered by the user to be between 0 and pmax */
	public void textValueChanged(TextEvent evt)
	{
		if(evt.getSource() == textField) // This should always be true (unless we add another textbox)
		{
			int lastCaretPosition;
			if(textField.getValue() < 0)
			{
				lastCaretPosition = textField.getCaretPosition();
				textField.setText("" + 0);
				textField.setCaretPosition(lastCaretPosition);
			}
			else if(textField.getValue() > pmax)
			{
				lastCaretPosition = textField.getCaretPosition();
				textField.setText("" + pmax);
				textField.setCaretPosition(lastCaretPosition);
			}   
		}
	}   

}
