package projections.gui;

import java.awt.*;
import java.awt.event.*;

class AboutDialog extends Dialog
{
   public AboutDialog(Frame parent)
   {
	  super(parent, "About Projections", true);
	  
	  addWindowListener(new WindowAdapter()
	  {
		 public void windowClosing(WindowEvent e)
		 {
			setVisible(false);
		 }
	  });
	  
	  Panel p1 = new Panel();
	  p1.setLayout(new GridLayout(9,1));
	  p1.add(new Label("PROJECTIONS v2.1", Label.CENTER));
	  p1.add(new Label("Performance analysis for Charm++ programs", Label.CENTER));
	  p1.add(new Label("By Mike DeNardo, Sid Cammeresi, ", Label.CENTER));
	  p1.add(new Label("Theckla Loucios, Orion Lawlor,", Label.CENTER));
	  p1.add(new Label("and Gengbin Zheng", Label.CENTER));
	  p1.add(new Label("Parallel Programming Lab", Label.CENTER));
	  p1.add(new Label("University of Illinois at Urbana-Champaign", Label.CENTER));
	  p1.add(new Label("http://charm.cs.uiuc.edu/", Label.CENTER));
	  p1.add(new Label("February 9, 2001", Label.CENTER));
	  add("Center", p1);
	  
	  Panel p2 = new Panel();
	  Button ok = new Button("OK");
	  p2.add(ok);
	  add(p2, "South");
	  
	  pack();
	  setResizable(false);   
  
	  ok.addActionListener(new ActionListener()
	  {
		 public void actionPerformed(ActionEvent evt) 
		 {
			setVisible(false);
		 }
	  });
   }   
}
