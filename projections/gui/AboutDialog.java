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
      p1.setLayout(new GridLayout(3,1));
      p1.add(new Label("PROJECTIONS v2.0", Label.CENTER));
      p1.add(new Label("By Mike DeNardo and Sid Cammeresi ", Label.CENTER));
      p1.add(new Label("June 1, 1998", Label.CENTER));
      add("Center", p1);
      
      Panel p2 = new Panel();
      Button ok = new Button("OK");
      p2.add(ok);
      add(p2, "South");
      
      pack();
      
      Toolkit tk = Toolkit.getDefaultToolkit();
      Dimension d = tk.getScreenSize();     
      setLocation((d.width - getSize().width)/2, (d.height - getSize().height)/2);
      
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
