package projections.gui.Timeline;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

public class MessageWindow extends JFrame
   implements ActionListener
{

	private static final long serialVersionUID = 1L;
	private EntryMethodObject obj;
	private MessagePanel canvas;
	private ScrollPane sp;
   
   public MessageWindow(EntryMethodObject obj)
   {
	  this.obj  = obj;

	  setTitle("Timeline Entry Details");
 
	  sp = new ScrollPane();
	  
	  canvas = new MessagePanel(obj);
	  sp.add(canvas);

	  getContentPane().setLayout(new BorderLayout());
	  getContentPane().add(sp, "Center");
	  
	  pack();
   }   
   
   public Dimension getPreferredSize() {
       return new Dimension(400, 800);
   }


   public void actionPerformed(ActionEvent e) {
	   // TODO Auto-generated method stub

   }
}



