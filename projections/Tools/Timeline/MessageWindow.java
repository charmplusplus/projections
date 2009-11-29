package projections.Tools.Timeline;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

public class MessageWindow extends JFrame
   implements ActionListener
{

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
       return new Dimension(600, 800);
   }


   public void actionPerformed(ActionEvent e) {
	   // TODO Auto-generated method stub

   }
}



