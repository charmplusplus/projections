package projections.misc;

import java.awt.*;
import java.awt.event.*;
import projections.gui.Util;

/**
  general purpose error dialog
*/
public class ErrorDialog extends Dialog
   implements ActionListener
{
	private static final long serialVersionUID = 1L;

	public ErrorDialog(Frame parent, String msg)
   {
	  super(parent, "ERROR", true);
	  
	  Button bOK = new Button("OK");
	  bOK.addActionListener(this);
	  Label l1 = new Label(msg, Label.LEFT);
	  
	  
	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  setLayout(gbl);
	  gbc.fill = GridBagConstraints.BOTH;
	  Util.gblAdd(this, l1,          gbc, 0,0, 1,1, 1,1, 2, 2, -2, 2);
	  gbc.fill = GridBagConstraints.VERTICAL;
	  Util.gblAdd(this, bOK,         gbc, 0,1, 1,1, 0,1);
											 
	  pack();
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  setVisible(false);
	  dispose();
   }   
}
