package projections.gui;

/**
 * Small Time Line (Stl) Display Panel
 * Orion Sky Lawlor, olawlor@acm.org, 2/9/2001
 *
 * A Stl compresses an entire parallel run into a single
 * image by coding processor utilization as color.
 * Since images are assembled pixel-by-pixel, this is
 * much faster than a timeline.
 */

import java.awt.*;
import java.awt.event.*;
import projections.misc.ProgressDialog;

public class StlWindow extends Frame
	implements MouseListener, ActionListener, ScalePanel.StatusDisplay
{
   private ScaleSlider hor,ver;
   private ScalePanel scalePanel;
   private StlPanel stl;
   private Label status;
   
   public StlWindow()
   {
	  addWindowListener(new WindowAdapter()
	  {                    
		 public void windowClosing(WindowEvent e)
		 {
		 	dispose();
		 }
	  });
	  
	  setBackground(Color.black);
	  setTitle("Projections-- Overview");
	  
	  createMenus();
	  createLayout();
	  pack();
	  
	  setVisible(true);
   }                        
   public void actionPerformed(ActionEvent evt)
   {
     if(evt.getSource() instanceof MenuItem)
     {
     	MenuItem mi = (MenuItem)evt.getSource();
        String arg = mi.getLabel();
	if(arg.equals("Close"))  {
	 	dispose();
        }
     }
   }   
   private void createLayout()
   {
	  GridBagLayout      gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  setLayout(gbl);
	  
	  gbc.fill  = GridBagConstraints.HORIZONTAL;
	  gbc.gridx = 0; gbc.gridy=2;
	  add(status=new Label(""),gbc);
	  status.setBackground(Color.black);
	  status.setForeground(Color.lightGray);
	  
	  gbc.fill  = GridBagConstraints.HORIZONTAL;
	  gbc.gridx = 0; gbc.gridy=1;
	  hor=new ScaleSlider(Scrollbar.HORIZONTAL);
	  add(hor,gbc);
	  hor.addMouseListener(this);
	  
	  gbc.fill  = GridBagConstraints.VERTICAL;
	  gbc.gridx = 1; gbc.gridy=0;
	  ver=new ScaleSlider(Scrollbar.VERTICAL);
	  add(ver,gbc);
	  ver.addMouseListener(this);
	  
	  gbc.fill  = GridBagConstraints.BOTH;
	  gbc.gridx = 0; gbc.gridy=0;
	  gbc.weightx = 1; gbc.weighty=1;
	  stl=new StlPanel();
	  ScalePanel scalePanel=new ScalePanel(hor,ver,stl);
	  add(scalePanel,gbc);
	  scalePanel.setStatusDisplay(this);
	  
	  ColorMap cm=new ColorMap();
	  cm.addBreak( 0,  0,  0, 55,    70,255,  0,  0); //Blue to red
	  cm.addBreak(70,255,  0,  0,   100,255,255,255); //red to white
	  //cm.addBreak(51,0,255,  0,   100,255, 55,30); //red to green (POOR FOR COLORBLIND PEOPLE)
	  cm.addBreak(101,0,255,0, 255,0,255,0); //Overflow-- green
	  stl.setColorMap(cm.getColorModel());
	  
	  stl.setData(7000);
	  
		double horSize=Analysis.getTotalTime();
		double verSize=Analysis.getNumProcessors();
		scalePanel.setScales(horSize,verSize);
	
	double hMin=scalePanel.toSlider(1.0/horSize);
	double hMax=scalePanel.toSlider(0.01);//0.1ms fills screen
	hor.setMin(hMin); hor.setMax(hMax);
	hor.setValue(hMin);
	hor.setTicks(Math.floor(hMin),1);
	
	double vMin=scalePanel.toSlider(1.0/verSize);
	double vMax=scalePanel.toSlider(1.0);//One processor fills screen
	ver.setMin(vMin); ver.setMax(vMax);
	ver.setValue(vMin);
	ver.setTicks(Math.floor(vMin),1);
   }               
   private void createMenus()
   {
        MenuBar mbar = new MenuBar();

        mbar.add(Util.makeMenu("File", new Object[]
        {
                 "Close"
        }, this));
	setMenuBar(mbar);
   } 
	public void mouseClicked(MouseEvent evt)
	  {}
   	public void mouseEntered(MouseEvent evt)
	{
		Object src=evt.getComponent();
		if (src==hor) setStatus("Click or drag to set the horizontal zoom");
		if (src==ver) setStatus("Click or drag to set the vertical zoom");
	}
	public void mouseExited(MouseEvent evt)
	{
		setStatus("");//Clear the old message
	}
	public void mousePressed(MouseEvent evt)
	  {}
	public void mouseReleased(MouseEvent evt)
	  {}
   public void setStatus(String msg) {
   	status.setText(msg);
   }   
}
