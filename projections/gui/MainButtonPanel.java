package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class MainButtonPanel extends Container
   implements ActionListener
{
   private MainWindow mainWindow;
   private MyButton[] buttonArray = new MyButton[7];
   private boolean inside = false;

   public MainButtonPanel(MainWindow mainWindow)
   {
      this.mainWindow = mainWindow;

      setBackground(Color.lightGray);

      Font f = new Font("SansSerif", Font.PLAIN, 14);
      setFont(f);

      buttonArray[0] = new MyButton("Open File",       Color.blue);
      buttonArray[1] = new MyButton("Graphs",          Color.blue);
      buttonArray[2] = new MyButton("Timelines",       Color.blue);
      buttonArray[3] = new MyButton("Usage Profile",   Color.blue);
      buttonArray[4] = new MyButton("Animations",      Color.blue);
      buttonArray[5] = new MyButton("View Log Files",  Color.blue);
      buttonArray[6] = new MyButton("Histograms",      Color.blue);

      setLayout(new GridLayout(4,2,20,20));
      for(int i=0; i<7; i++)
      {
         add(buttonArray[i]);
         buttonArray[i].addActionListener(this);
         if(i > 0) buttonArray[i].setEnabled(false);
      }
   }

   public void enableButtons()
   {
      for(int i=1; i<7; i++)
         buttonArray[i].setEnabled(true);
   }
   public void disableButtons()
   {
      for(int i=1;i<7; i++)
         buttonArray[i].setEnabled(false);
   }

   public void actionPerformed(ActionEvent evt)
   {
      MyButton c = (MyButton) evt.getSource();
      if(c == buttonArray[0])
         mainWindow.ShowOpenFileDialog();
      else if(c == buttonArray[1])
         mainWindow.ShowGraphWindow();
      else if(c == buttonArray[2])
         mainWindow.ShowTimelineWindow();
      else if(c == buttonArray[3])
         mainWindow.ShowProfileWindow();
      else if(c == buttonArray[4])
         mainWindow.ShowAnimationWindow();
      else if(c == buttonArray[5])
         mainWindow.ShowLogFileViewerWindow();
      else if(c == buttonArray[6])
         mainWindow.ShowHistogramWindow();
   }
}




