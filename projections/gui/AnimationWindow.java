package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class AnimationWindow extends Frame
   implements ActionListener
{

   private AnimationColorBarPanel colorbarPanel;
   private AnimationDisplayPanel  displayPanel;

   private Button bPlusOne, bMinusOne, bAuto;
   private TimeTextField delayField,intervalField;
   private Panel statusPanel;
   private Panel titlePanel; 
   
   private Label lTitle, lStatus;
   
   private int redrawDelay; //Real time between frames (ms)
   private int curInterval = 0; //Frame number
   private boolean keepAnimating;
   private AnimateThread thread;
   
   class AnimateThread extends Thread
   {
	  public AnimateThread()
	  {
	  	 keepAnimating=true;
	  }
   
	  public void run()
	  {
		 long finish = redrawDelay + System.currentTimeMillis();
		 while(keepAnimating)
		 {
			long timeLeft=finish-System.currentTimeMillis();
			if (timeLeft>0) {
			  try { //Give other threads a chance
				sleep(timeLeft);
			  } catch (InterruptedException E) {}
			}
			else
			{ //Advance to next frame
			   finish += redrawDelay;
			   changeCurI(displayPanel.getCurI() + 1);
			}   
		 }
	  }  
   }         
   public AnimationWindow()
   {
	  addWindowListener(new WindowAdapter()
	  {                    
		 public void windowClosing(WindowEvent e)
		 {
			if(thread != null && thread.isAlive())
			{
			   keepAnimating=false;
			   thread = null;
			}   
			dispose();
		 }
	  });
	  
	  setBackground(Color.lightGray);
	  setTitle("Projections Animation");
	  
	  createMenus();
	  createLayout();
	  pack();
	  
	  setVisible(true);
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  if(evt.getSource()==delayField)
	  {
		 redrawDelay=(int)(delayField.getValue()/1000); //Get redraw in milliseconds
	  }
	  else if(evt.getSource()==intervalField)
	  {
	  	 displayPanel.setIsize((int)intervalField.getValue());
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
			   keepAnimating=true;
			   thread = new AnimateThread();
			   thread.start();
			}   
			else
			{
			   b.setLabel("Auto");
			   if(thread != null && thread.isAlive())
			   {
			   	  keepAnimating=false;
				  thread = null;
				  changeCurI(displayPanel.getCurI());
			   }   
			}
		 }
	  }                        
   }   
   private void changeCurI(int i)
   {
	  displayPanel.setCurI(i);
	  setTitleInfo(displayPanel.getCurI()); 
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
	  
	  redrawDelay=500;
	  delayField = new TimeTextField("500 ms", 8);
	  intervalField = new TimeTextField("100 ms",8);
	  
	  delayField.addActionListener(this);
	  intervalField.addActionListener(this);
	  
	  titlePanel.setBackground(Color.black);
	  titlePanel.setForeground(Color.white);
	  displayPanel.setBackground(Color.black);
	  Font titleFont = new Font("SansSerif", Font.BOLD, 16);
	  lTitle = new Label("Processor Usage at 0s ", Label.CENTER);
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
   private void createMenus()
   {
   }   
   public void setStatusInfo(int p, int i, int u)
   {
	  String status;
	  if(p < 0)
		 status = "";
	  else   
		 status = "Processor " + p + ": Usage = " + u + "% at " + U.t(displayPanel.getIsize()*i);
	  lStatus.setText(status);
	  lStatus.invalidate();
	  statusPanel.validate();
   }   
   public void setTitleInfo(int i)
   {
	  String title = "Processor Usage at " + U.t(displayPanel.getIsize()*i);
	  lTitle.setText(title);
	  lTitle.invalidate();
	  titlePanel.validate();
   }   
}
