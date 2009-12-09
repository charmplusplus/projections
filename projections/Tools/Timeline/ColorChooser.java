package projections.Tools.Timeline;

import java.awt.Button;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JColorChooser;

import projections.gui.Analysis;
import projections.gui.ColorPanel;
import projections.gui.MainWindow;
import projections.gui.Util;


/** The color chooser for choosing the colors for each entry method (i think)*/
class ColorChooser extends Frame
   implements ActionListener
{


	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;
   
   private Data data;
   private TimelineWindow parentTimelineWindow;
   private ColorPanel[] cpEntry;
   private ColorPanel   selectedCP;
   private Button       bApply, bClose;
   
   
   class GrayLWPanel extends Panel
   {
       
	public void paint(Graphics g)
       {
	   g.setColor(Color.lightGray);
	   g.fillRect(0, 0, getSize().width, getSize().height);
	   
	   g.setColor(Color.black);
	   g.drawRect(0, 0, getSize().width-1, getSize().height-1);
	   
	   super.paint(g);
       }   
   }   

   protected ColorChooser(Data data, TimelineWindow parentTimelineWindow_)
    {
	super();
	parentTimelineWindow = parentTimelineWindow_;
	this.data = data;
	
	addWindowListener(new WindowAdapter()
	    {                    
		public void windowClosing(WindowEvent e)
		{
		    Close();
		}
	    });
	
	setBackground(Color.lightGray);
	setTitle("Timeline Colors");
	setLocation(0, 0);
	setSize(400, 600);
	
	CreateLayout();
	pack();
    }   

    public void actionPerformed(ActionEvent evt)
    {
	if (evt.getSource() instanceof Button) {
	    Button b = (Button)evt.getSource();
		 
	    if (b == bApply) {
		for (int e=0; e<MainWindow.runObject[myRun].getNumUserEntries(); e++) {
		    if (cpEntry[e] != null) {
			data.entryColor()[e] = cpEntry[e].getColor();
		    }
		
		}
		parentTimelineWindow.refreshDisplay(false);
		
	    } else if (b == bClose) {
		Close(); 
	    } 
//      FIXME reenable this functionality
//	    else if (b == bGraphColors) {
//		if (GraphExists()) {
//		    for (int e=0; e<MainWindow.runObject[myRun].getNumUserEntries(); e++) {
//			if (cpEntry[e] != null) {
//			    cpEntry[e].setColor(getGraphColor(e));
//			}
//		    }
//		}   
//	    }                                      
	} else if (evt.getSource() instanceof ColorPanel) {
	    selectedCP = (ColorPanel)evt.getSource();
	    String s = null;
		 
	    for (int e=0; e<MainWindow.runObject[myRun].getNumUserEntries(); e++) {
		if (selectedCP == cpEntry[e]) {
		    s = MainWindow.runObject[myRun].getEntryNameByIndex(e);
		}
	    }
//	    JColorChooser colorWindow = new JColorChooser();
	    Color returnColor =
		JColorChooser.showDialog(this, s,
				       selectedCP.getColor());
	    if (returnColor != null) {
		selectedCP.setColor(returnColor);
	    }
	} 
    }   

    private void Close() 
    {
	setVisible(false);
	parentTimelineWindow.controls.CloseColorWindow();
	dispose();  
    }   
    
    private void CreateLayout()
    {
	Panel p = new Panel();
	add("Center", p);
	
	GridBagLayout      gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.fill = GridBagConstraints.BOTH;

	cpEntry = new ColorPanel[MainWindow.runObject[myRun].getNumUserEntries()];
	
	Panel p1 = new Panel();
	p1.setLayout(gbl);
	
	int ypos = 0;
	String charename = new String();
	Analysis a = MainWindow.runObject[myRun];
	for (int e=0; e<a.getNumUserEntries(); e++) {
	    if (data.entries()[e] > 0) {
		if (!charename.equals(a.getEntryChareNameByIndex(e))) {
		    charename = a.getEntryChareNameByIndex(e);
		    Label lc = new Label(a.getEntryChareNameByIndex(e), Label.LEFT);
		    Util.gblAdd(p1, lc, gbc, 0,ypos++, 1,1, 1,0, 0,5,0,5);
		}   
		Label l = new Label(a.getEntryNameByIndex(e), Label.RIGHT);
		Util.gblAdd(p1, l, gbc, 0,ypos, 1,1, 1,0, 0,5,0,5);
		
		cpEntry[e] = new ColorPanel(data.entryColor()[e]);
		Util.gblAdd(p1, cpEntry[e], gbc, 1,ypos++, 1,1, 0,0, 0,5,0,5);
		cpEntry[e].addActionListener(this);
	    }
	}
	
	Util.gblAdd(p1, new Label(""), gbc, 0,ypos, 1,1, 1,1);
	ScrollPane sp = new ScrollPane();
	sp.add(p1);
	
	GrayLWPanel p2 = new GrayLWPanel();
	p2.setLayout(gbl);
	Util.gblAdd(p2, sp, gbc, 0,0, 1,1, 1,1, 5,5,5,5);
	
	GrayLWPanel p3 = new GrayLWPanel();
	p3.setLayout(new FlowLayout());
	
	//FIXME readd this functionality
//	bGraphColors = new Button("Use Graph colors");
//	p3.add(bGraphColors);
//	if (GraphExists()) {
//	    bGraphColors.addActionListener(this);   
//	} else {
//	    bGraphColors.setForeground(Color.gray);
//	}
	  
	bApply = new Button("Apply");
	bApply.addActionListener(this);
	p3.add(bApply);
	
	bClose = new Button("Close");
	bClose.addActionListener(this);
	p3.add(bClose);
	
	p.setLayout(gbl);
	Util.gblAdd(p, p2, gbc, 0,0, 1,1, 1,1, 4,4,2,4);
	Util.gblAdd(p, p3, gbc, 0,1, 1,1, 1,0, 2,4,4,4);
    }   
   
//    private Color getGraphColor(int e)
//    {
//	return parentTimelineWindow.controlsPanel.getGraphColor(e);
//    }


}
