package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class InvalidFileDialog extends Dialog
   implements ActionListener
{
   public InvalidFileDialog(Frame parent)
   {
	  super(parent, "ERROR", true);
	  
	  Button bOK = new Button("OK");
	  bOK.addActionListener(this);
	  Label l1 = new Label("Invalid File Name", Label.LEFT);
	  Label l2 = new Label("Please select a *.sts file", Label.LEFT);
	  
	  
	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  setLayout(gbl);
	  gbc.fill = GridBagConstraints.BOTH;
	  Util.gblAdd(this, l1,          gbc, 0,0, 1,1, 1,1, 2, 2, -2, 2);
	  Util.gblAdd(this, l2,          gbc, 0,1, 1,1, 1,1, -2, 2, 2, 2);
	  gbc.fill = GridBagConstraints.VERTICAL;
	  Util.gblAdd(this, bOK,         gbc, 0,2, 2,1, 0,1);
											 
	  pack();
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  setVisible(false);
	  dispose();
   }   
}