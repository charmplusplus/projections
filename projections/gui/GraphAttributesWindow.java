package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class GraphAttributesWindow extends ColorWindowFrame
   implements ActionListener
{
   private ColorSelectWindow colorSelectWindow;
   private GraphData data;

   private Checkbox[]     cbSystemUsage;
   private ColorPanel[]   cpSystemUsage;
   private ColorPanel[][] cpSystemMsgs;
   private ColorPanel[][] cpUserEntry;
   private Checkbox[][]   cbSystemMsgs;
   private Checkbox[][]   cbUserEntry;
   
   private ColorPanel  selectedCP;
   
   private GrayPanel p1, p2, p4;
   
   
   private Button bAll, bClear, bApply, bClose;
   
   public GraphAttributesWindow(Frame parent, GraphData data)
   {
	  super(parent);
	  this.data = data;
	  
	  addWindowListener(new WindowAdapter()
	  {                    
		 public void windowClosing(WindowEvent e)
		 {
			Close();
		 }
	  });
	  
	  setBackground(Color.lightGray);
	  setTitle("Select Display Items");
	  setLocation(0, 0);
		
	  CreateLayout();
	  pack();
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  if(evt.getSource() instanceof ColorPanel)
	  {   
		 selectedCP = (ColorPanel)evt.getSource();
		 String s = "TEST";
		
		 for(int a=0; a<3; a++)
			if(selectedCP == cpSystemUsage[a])
			   s = data.systemUsage[a].name;
		 
		 for(int a=0; a<5; a++)
			for(int t=0; t<2; t++)
			   if(selectedCP == cpSystemMsgs[a][t])
			   {
				  s = data.systemMsgs[a][t].name;
				  s += data.systemMsgs[a][t].type;
			   }   
			   
		 for(int a=0; a<data.numUserEntries; a++) 
			for(int t=0; t<2; t++)
			   if(selectedCP == cpUserEntry[a][t])
			   {
				  s = data.userEntry[a][t].name;
				  s += data.userEntry[a][t].type;
			   }   
			   
		 colorSelectWindow = new ColorSelectWindow(this, selectedCP.getColor(), s);
	  }   
	  else if(evt.getSource() instanceof Button)
	  {
		 Button b = (Button) evt.getSource();
	  
		 if(b == bAll || b== bClear)
		 {
			boolean dest=(b==bAll);
			for(int a=0; a<3; a++)  if (null!=cbSystemUsage[a])
			   cbSystemUsage[a].setState(dest);
			for(int a=0; a<5; a++) if (null!=cbSystemMsgs[a][0])
			{
			   cbSystemMsgs[a][0].setState(dest);
			   cbSystemMsgs[a][1].setState(dest);
			}   
			for(int a=0; a<data.numUserEntries; a++)  if (null!=cbUserEntry[a][0])
			{
			   cbUserEntry[a][0].setState(dest);
			   cbUserEntry[a][1].setState(dest);
			}   
		 }
		 else if(b == bClose)
		 {
			Close();   
		 } 
		 else if(b == bApply)
		 {
			int numOnGraph = 0;
			
			for(int a=0; a<3; a++)
			if (null!=cbSystemUsage[a])
			{   
			   data.systemUsage[a].state = cbSystemUsage[a].getState();
			   data.systemUsage[a].color = cpSystemUsage[a].getColor();
			   if(data.systemUsage[a].state == true &&
				data.systemUsage[a].exists == true)
				  numOnGraph++;
			}   
			for(int a=0; a<5; a++)
			if (null!=cbSystemMsgs[a][0])
			{
			   for(int t=0; t<3; t++)
			   {
				  if(t < 2)
				  {
					 data.systemMsgs[a][t].state = cbSystemMsgs[a][t].getState();
					 data.systemMsgs[a][t].color = cpSystemMsgs[a][t].getColor();
				  }
				  else
				  {   
					 if(data.systemMsgs[a][0].state || data.systemMsgs[a][1].state)
						data.systemMsgs[a][2].state = true;
					 else
						data.systemMsgs[a][2].state = false;   
					 data.systemMsgs[a][2].color = data.systemMsgs[a][0].color;
				  }   
				  if(data.systemMsgs[a][t].state == true &&
					data.systemMsgs[a][t].exists == true)
					 numOnGraph++;
			   }      
			}
			for(int a=0; a<data.numUserEntries; a++)
			if (null!=cbUserEntry[a][0])
			{
			   for(int t=0; t<3; t++)
			   {
				  if(t < 2)
				  {
					 data.userEntry[a][t].state = cbUserEntry[a][t].getState();
					 data.userEntry[a][t].color = cpUserEntry[a][t].getColor();
				  }
				  else
				  {
					 if(data.userEntry[a][0].state || data.userEntry[a][1].state)
						data.userEntry[a][2].state = true;
					 else
						data.userEntry[a][2].state = false;   
					 data.userEntry[a][2].color = data.userEntry[a][0].color;
				  }
				  if(data.userEntry[a][t].state == true &&
					data.userEntry[a][t].exists == true)
					 numOnGraph++;
			   }         
			}
		 
			data.onGraph = new ZItem[numOnGraph];
			int onGraphIndex = 0;
			
			for(int a=0; a<3; a++)
			if (data.systemUsage[a].exists)
			{   
			   if(data.systemUsage[a].state == true)
				  data.onGraph[onGraphIndex++] = data.systemUsage[a];
			}                         
			for(int a=0; a<5; a++)
			{
			   for(int t=0; t<3; t++)
			   if (data.systemMsgs[a][t].exists)
			   {
				  if(data.systemMsgs[a][t].state == true)
					 data.onGraph[onGraphIndex++] = data.systemMsgs[a][t];
			   }
			}         
			for(int a=0; a<data.numUserEntries; a++)
			{
			   for(int t=0; t<3; t++)
			   if (data.userEntry[a][t].exists)
			   {
				  if(data.userEntry[a][t].state == true)
					 data.onGraph[onGraphIndex++] = data.userEntry[a][t];
			   }      
			}
			
			data.setData(); 

			data.legendPanel.UpdateLegend();
			data.displayPanel.setAllBounds();
			data.displayPanel.UpdateDisplay();
		 }
	  }           
   }   
   public void applyNewColor(Color c)
   {
	  selectedCP.setColor(c);
   }   
   public void Close()
   {
	  setVisible(false);
	  for(int a=0; a<3; a++)
	  if (cbSystemUsage[a]!=null)
	  {
		 cbSystemUsage[a].setState(data.systemUsage[a].state);
		 cpSystemUsage[a].setColor(data.systemUsage[a].color);
	  }   
	  for(int a=0; a<5; a++)
	  if (null!=cbSystemMsgs[a][0])
	  {
		 for(int t=0; t<2; t++)
		 {
			cbSystemMsgs[a][t].setState(data.systemMsgs[a][t].state);
			cpSystemMsgs[a][t].setColor(data.systemMsgs[a][t].color);
		 }
	  }   
	  for(int a=0; a<data.numUserEntries; a++)
	  if (null!=cbUserEntry[a][0])
	  {
		 for(int t=0; t<2; t++)
		 {
			cbUserEntry[a][t].setState(data.userEntry[a][t].state);  
			cpUserEntry[a][t].setColor(data.userEntry[a][t].color);
		 }
	  } 
   }   
   private void CreateLayout()
   {
	  cbSystemUsage = new Checkbox[3];
	  cpSystemUsage = new ColorPanel[3];
	  for(int a=0; a<3; a++)
	  if (data.systemUsage[a].exists)
	  {
		 cbSystemUsage[a] = new Checkbox();
		 cbSystemUsage[a].setState(data.systemUsage[a].state);
		 cpSystemUsage[a] = new ColorPanel(data.systemUsage[a].color);
		 cpSystemUsage[a].addActionListener(this);
	  }
	  
	  cbSystemMsgs = new Checkbox[5][2];
	  cpSystemMsgs = new ColorPanel[5][2];
	  for(int a=0; a<5; a++)
	  {
		 for(int t=0; t<2; t++) 
		 if (data.systemMsgs[a][0].exists || data.systemMsgs[a][1].exists)
		 {
			cbSystemMsgs[a][t] = new Checkbox();
			cbSystemMsgs[a][t].setState(data.systemMsgs[a][t].state);
			cpSystemMsgs[a][t] = new ColorPanel(data.systemMsgs[a][t].color);
			cpSystemMsgs[a][t].addActionListener(this);
		 }      
	  }
	  
	  cbUserEntry = new Checkbox[data.numUserEntries][2];
	  cpUserEntry = new ColorPanel[data.numUserEntries][2];
	  for(int a=0; a<data.numUserEntries; a++)
	  {
		 for(int t=0; t<2; t++) 
		 if (data.userEntry[a][0].exists || data.userEntry[a][1].exists)
		 {
			cbUserEntry[a][t] = new Checkbox();
			cbUserEntry[a][t].setState(data.userEntry[a][t].state);
			cpUserEntry[a][t] = new ColorPanel(data.userEntry[a][t].color);
			cpUserEntry[a][t].addActionListener(this);
			
		 }
	  }
	  Panel p = new Panel();
	  add("Center", p);
	  p.setBackground(Color.gray);
	  
	  GridBagLayout      gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  p.setLayout(gbl);
	  
	  p1 = new GrayPanel();
	  p2 = new GrayPanel();
	  p4 = new GrayPanel();
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  gbc.insets = new Insets(0, 2, 0, 2);
	  
	  Label lUsageA = new Label("SYSTEM USAGE", Label.LEFT);
	  lUsageA.setFont(new Font("SansSerif", Font.BOLD, 12));
	  
	  p1.setLayout(gbl);
	  
	  Util.gblAdd(p1, lUsageA,          gbc, 0,0, 3,1, 1,0, 5,5,0,5);
	  for (int a=0;a<3;a++) if (data.systemUsage[a].exists) 
	  {
		Label lUsage0 = new Label(data.systemUsage[a].name, Label.RIGHT);
	  
		Util.gblAdd(p1, lUsage0,          gbc, 0,a+1, 1,1, 0,0, 0,5,0,5);
		Util.gblAdd(p1, cbSystemUsage[a], gbc, 1,a+1, 1,1, 0,0, 0,0,0,0);
		Util.gblAdd(p1, cpSystemUsage[a], gbc, 2,a+1, 1,1, 0,0, 0,0,0,5);      
	  }

	  ScrollPane sp = new ScrollPane();
	  Label lNote0 = new Label("* For a Y-Axis setting of Time:");
	  Label lNote1 = new Label("- System Usage items will not be displayed.");
	  Label lNote2 = new Label("- Selecting Creation AND/OR Processing will display the others.");
	  lNote0.setFont(new Font("SansSerif", Font.PLAIN, 10));
	  lNote1.setFont(new Font("SansSerif", Font.PLAIN, 10));
	  lNote2.setFont(new Font("SansSerif", Font.PLAIN, 10));
	  
	  p2.setLayout(gbl);
	  
	  Util.gblAdd(p2, sp,     gbc,  0,0, 1,1, 1,1, 5,5, 0,5);
	  Util.gblAdd(p2, lNote0, gbc,  0,1, 1,1, 1,0, 0,5,-1,5);
	  Util.gblAdd(p2, lNote1, gbc,  0,2, 1,1, 1,0, 0,5,-1,5);
	  Util.gblAdd(p2, lNote2, gbc,  0,3, 1,1, 1,0, 0,5, 5,5);
	  
	  
	  Label lhdr1 = new Label("SYSTEM MESSAGES", Label.LEFT);
	  Label lhdr2 = new Label("USER ENTRIES",   Label.LEFT);
	  Label lC    = new Label("Creation",      Label.CENTER);
	  Label lP    = new Label("Processing",    Label.CENTER);
	  lhdr1.setFont(new Font("SansSerif", Font.BOLD, 12));
	  lhdr2.setFont(new Font("SansSerif", Font.BOLD, 12));
   
	  
	  LWPanel p3 = new LWPanel();
	  sp.add(p3);
	  p3.setLayout(gbl);
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  gbc.anchor = GridBagConstraints.CENTER;
	  gbc.insets = new Insets(0, 2, 0, 2);
	  Util.gblAdd(p3, lC,    gbc, 1,0, 2,1, 1,0);
	  Util.gblAdd(p3, lP,    gbc, 3,0, 2,1, 1,0);
	  Util.gblAdd(p3, lhdr1, gbc, 0,1, 3,1, 1,0);
	  
	  int ypos = 2;
	  for(int a=0; a<5; a++)
	  if (data.systemMsgs[a][0].exists)
	  {
		 Label l = new Label(data.systemMsgs[a][0].name, Label.RIGHT);
		 gbc.anchor = GridBagConstraints.CENTER;
		 gbc.fill = GridBagConstraints.BOTH;
		 Util.gblAdd(p3, l,                   gbc, 0,ypos,   1,1, 1,0, 0,2,0,5);       
		 gbc.fill = GridBagConstraints.VERTICAL;
		 gbc.anchor = GridBagConstraints.EAST;
		 Util.gblAdd(p3, cbSystemMsgs[a][0], gbc, 1,ypos,   1,1, 0,0, 0,0,0,0);
		 Util.gblAdd(p3, cbSystemMsgs[a][1], gbc, 3,ypos,   1,1, 0,0, 0,0,0,0);
		 gbc.anchor = GridBagConstraints.WEST;
		 Util.gblAdd(p3, cpSystemMsgs[a][0], gbc, 2,ypos,   1,1, 0,0, 0,0,0,5);
		 Util.gblAdd(p3, cpSystemMsgs[a][1], gbc, 4,ypos++, 1,1, 0,0, 0,0,0,5);
	  }
	  
	  String chareName = new String();
	  Label l;
	  gbc.fill = GridBagConstraints.BOTH;
	  Util.gblAdd(p3, lhdr2, gbc, 0, ypos++, 1, 1, 1, 0);
	  for(int a=0; a<data.numUserEntries; a++)
	  if (data.userEntry[a][0].exists || data.userEntry[a][1].exists)
	  {
		 gbc.anchor = GridBagConstraints.CENTER;
		 gbc.fill = GridBagConstraints.BOTH;
		 if(!data.userEntry[a][0].parent.equals(chareName))
		 {
			chareName = data.userEntry[a][0].parent;
			l = new Label(chareName, Label.LEFT);
			Util.gblAdd(p3, l, gbc, 0, ypos++, 3, 1, 1, 0);
		 }
		 l = new Label(data.userEntry[a][0].name, Label.RIGHT);
		 Util.gblAdd(p3, l, gbc, 0, ypos, 1, 1, 1, 0, 0,2,0,5);
		 gbc.fill = GridBagConstraints.VERTICAL;
		 gbc.anchor = GridBagConstraints.EAST;
		 Util.gblAdd(p3, cbUserEntry[a][0], gbc, 1,ypos,   1,1, 0,0, 0,0,0,0);
		 Util.gblAdd(p3, cbUserEntry[a][1], gbc, 3,ypos,   1,1, 0,0, 0,0,0,0);
		 gbc.anchor = GridBagConstraints.WEST;
		 Util.gblAdd(p3, cpUserEntry[a][0], gbc, 2,ypos,   1,1, 0,0, 0,0,0,5);
		 Util.gblAdd(p3, cpUserEntry[a][1], gbc, 4,ypos++, 1,1, 0,0, 0,0,0,5);
	  }       
	  sp.validate();
	  
	  p4.setLayout(new FlowLayout());
	  bAll   = new Button("Select All");
	  bClear = new Button("Clear All");
	  bApply = new Button("Apply");
	  bClose = new Button("Close");
	  
	  bAll.addActionListener(this);
	  bClear.addActionListener(this);
	  bApply.addActionListener(this);
	  bClose.addActionListener(this);
	  
	  p4.add(bAll);
	  p4.add(bClear);
	  p4.add(bApply);
	  p4.add(bClose);
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  Util.gblAdd(p, p1, gbc,  0,0,  1,1,  1,0,  4,4,2,4);
	  Util.gblAdd(p, p2, gbc,  0,1,  1,1,  1,1,  2,4,2,4);
	  Util.gblAdd(p, p4, gbc,  0,2,  1,1,  1,0,  2,4,4,4);
   }   
}
