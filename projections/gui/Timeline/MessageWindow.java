package projections.gui.Timeline;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JScrollPane;

public class MessageWindow extends Frame
   implements ActionListener
{


	private static final long serialVersionUID = 1L;
	private EntryMethodObject obj;
   private MessagePanel canvas;
   private ScrollPane sp;
   
   public MessageWindow(EntryMethodObject obj)
   {
	  this.obj  = obj;
	  
	  addWindowListener(new WindowAdapter()
	  {                    
		 public void windowClosing(WindowEvent e)
		 {
			Close();
		 }
	  });

	  setBackground(Color.lightGray);
	  setTitle("Timeline Entry Details");
 
	  sp = new ScrollPane();
	  
	  canvas = new MessagePanel(obj);
	  sp.add(canvas);
	  
	  Panel p = new Panel();
	  p.setLayout(new FlowLayout());
	  Button bClose = new Button("Close");
	  bClose.addActionListener(this);
	  p.add(bClose);
	  
	  setLayout(new BorderLayout());
	  add(sp, "Center");
	  add(p,  "South");
	  
	  pack();
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  Close();
   }   
   void Close()
   {
	  setVisible(false);
	  obj.CloseMessageWindow();
	  dispose();
   }   
   public Dimension getPreferredSize() {
       if (canvas != null) { 
	   Dimension d = canvas.getPreferredSize(); 
	   d.width += 10;
	   d.height += 30;
	   return d;
       }
       return new Dimension(640, 480);
   }
}



