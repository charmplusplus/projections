package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class AnimationWindow extends Frame
   implements ActionListener
{

   private AnimationColorBarPanel colorbarPanel;
   private AnimationDisplayPanel  displayPanel;

   private Button bPlusOne, bMinusOne, bAuto;
   private IntTextField delayField, intervalField;
   private Panel statusPanel;
   private Panel titlePanel; 
   
   private Label lTitle, lStatus;
   
   private int curInterval = 0;
   private AnimateThread thread;
   
   public AnimationWindow()
   {
      addWindowListener(new WindowAdapter()
      {                    
         public void windowClosing(WindowEvent e)
         {
            if(thread != null && thread.isAlive())
            {
               thread.stop();
               thread = null;
            }   
            dispose();
         }
      });
      
      setBackground(Color.lightGray);
      Toolkit   tk = Toolkit.getDefaultToolkit();
      Dimension d  = tk.getScreenSize();
      int w = d.width/2;
      int h = d.height/2;
      pack();
      setSize(w, h);
      setLocation(w/2, h/2);
      setTitle("Projections Animation");
      
      createMenus();
      createLayout();
      
      setVisible(true);
   }
   
   private void createMenus()
   {
   }
   
   private void createLayout()
   {
      Panel mainPanel     = new Panel();
      titlePanel    = new Panel();
      statusPanel   = new Panel();
      Panel controlPanel  = new Panel();
      colorbarPanel = new AnimationColorBarPanel();
      displayPanel  = new AnimationDisplayPanel(this);
      
      bPlusOne  = new Button(">>");
      bMinusOne = new Button("<<");
      bAuto     = new Button("Auto");
      
      bPlusOne.addActionListener(this);
      bMinusOne.addActionListener(this);
      bAuto.addActionListener(this);
      
      delayField = new IntTextField(5, 5);
      intervalField = new IntTextField(0, 5);
      
      delayField.addActionListener(this);
      intervalField.addActionListener(this);
      
      titlePanel.setBackground(Color.black);
      titlePanel.setForeground(Color.white);
      displayPanel.setBackground(Color.black);
      Font titleFont = new Font("SansSerif", Font.BOLD, 16);
      lTitle = new Label("Processor Usage for Interval " + curInterval, Label.CENTER);
      lTitle.setFont(titleFont);
      titlePanel.add(lTitle);
      
      lStatus = new Label("");
      statusPanel.add(lStatus, "Center");
      statusPanel.setBackground(Color.lightGray);
      
      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.BOTH;
      
      controlPanel.setLayout(gbl);
      Util.gblAdd(controlPanel, bMinusOne, gbc, 0,0, 1,1, 1,1);
      Util.gblAdd(controlPanel, bPlusOne,  gbc, 1,0, 1,1, 1,1);
      Util.gblAdd(controlPanel, bAuto,     gbc, 2,0, 1,1, 1,1);
      Util.gblAdd(controlPanel, delayField, gbc, 3,0, 1,1, 1,1);
      Util.gblAdd(controlPanel, intervalField, gbc, 4,0, 1,1, 1,1);
      
      mainPanel.setBackground(Color.gray);
      mainPanel.setLayout(gbl);
      
      Util.gblAdd(mainPanel, titlePanel,    gbc, 0,0, 1,1, 1,0);
      Util.gblAdd(mainPanel, displayPanel,  gbc, 0,1, 1,1, 1,1);
      Util.gblAdd(mainPanel, colorbarPanel, gbc, 0,2, 1,1, 1,0);
      Util.gblAdd(mainPanel, statusPanel,   gbc, 0,3, 1,1, 1,0);
      Util.gblAdd(mainPanel, controlPanel,  gbc, 0,4, 1,1, 1,0); 
      
      add(mainPanel,"Center");
   }
   
   public void setStatusInfo(int p, int i, int u)
   {
      String status;
      if(p < 0)
         status = "";
      else   
         status = "Processor " + p + ": Usage = " + u + "% for Interval " + i;
      lStatus.setText(status);
      lStatus.invalidate();
      statusPanel.validate();
   }  
   
   public void setTitleInfo(int i)
   {
      String title = "Processor Usage for Interval " + i;
      lTitle.setText(title);
      lTitle.invalidate();
      titlePanel.validate();
   }   
   
   private void changeCurI(int i)
   {
      displayPanel.setCurI(i);
      setTitleInfo(displayPanel.getCurI()); 
   }   
   
   public void actionPerformed(ActionEvent evt)
   {
      if(evt.getSource() instanceof IntTextField)
      {
         IntTextField f = (IntTextField)evt.getSource();
         if(f == delayField)
         {
            displayPanel.setNumPs(delayField.getValue());
         }
         else if(f == intervalField)
         {
            displayPanel.setNumIs(intervalField.getValue());
         }   
      }   
      else if(evt.getSource() instanceof Button)
      {
         Button b = (Button)evt.getSource();
         if(b == bPlusOne)
         {
            changeCurI(displayPanel.getCurI() + 1);
         }
         else if(b == bMinusOne)
         {
            changeCurI(displayPanel.getCurI() - 1);
         }
         else if(b == bAuto)
         {
            if(b.getLabel().equals("Auto"))
            {
               b.setLabel("Stop");
               thread = new AnimateThread();
               thread.start();
            }   
            else
            {
               b.setLabel("Auto");
               if(thread != null && thread.isAlive())
               {
                  thread.stop();
                  thread = null;
                  changeCurI(displayPanel.getCurI());
               }   
            }
         }
      }                        
   }
   
   class AnimateThread extends Thread
   {
      private long delay;
      private long delay2;
      public AnimateThread()
      {
         delay = 500;
      }
   
      public void run()
      {
         long finish = delay + System.currentTimeMillis();
         while(true)
         {
            if(System.currentTimeMillis() > finish)
            {
               finish += delay;
               changeCurI(displayPanel.getCurI() + 1);
            }   
         }
      }  
   }         
}      
