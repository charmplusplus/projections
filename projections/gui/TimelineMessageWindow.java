package projections.gui;

import java.awt.*;
import java.awt.event.*;
import projections.analysis.*;

public class TimelineMessageWindow extends Frame
   implements ActionListener
{
   private TimelineObject obj;
   private TimelineMessageCanvas canvas;
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
      
      addComponentListener(new ComponentAdapter()
      {
         public void componentResized(ComponentEvent e)
         {
            if(canvas != null && sp != null)
            {
               int w = canvas.getPreferredSize().width;
               int spw = sp.getSize().width;
               int h = canvas.getPreferredSize().height;
               int sph = sp.getSize().height;
               
               if(w < spw) w = spw;
               if(h < sph) h = sph;
               
               canvas.setSize(w, h);
               canvas.repaint();              
            }    
         }
      });

      setBackground(Color.lightGray);
      
      Toolkit tk = Toolkit.getDefaultToolkit();
      Dimension d = tk.getScreenSize();
      int w = d.width;
      int h = d.height;
      
      pack();
      setTitle("Timeline Entry Details");
      setSize(w/2, h/3);
      setLocation(w/4, h/3);
 
      sp = new ScrollPane();
      
      canvas = new TimelineMessageCanvas(obj);
      sp.add(canvas);
      
      Panel p = new Panel();
      p.setLayout(new FlowLayout());
      Button bClose = new Button("Close");
      bClose.addActionListener(this);
      p.add(bClose);
      
      setLayout(new BorderLayout());
      add(sp, "Center");
      add(p,  "South");
   }       

   private void Close()
   {
      setVisible(false);
      obj.CloseMessageWindow();
      dispose();
   }
   
   public void actionPerformed(ActionEvent evt)
   {
      Close();
   }   
         
}
      
      
      
      
