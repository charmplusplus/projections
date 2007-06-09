package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class TimelineMessageWindow extends Frame
   implements ActionListener
{
   private TimelineObject obj;
   private TimelineMessagePanel canvas;
   private ScrollPane sp;
   
   public TimelineMessageWindow(TimelineObject obj)
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
	  
	  canvas = new TimelineMessagePanel(obj);
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



