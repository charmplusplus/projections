package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class GraphLegendPanel extends Panel
    implements ActionListener
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

    private Frame parent;
    private GridBagConstraints gbc;
    
    private Button       bSelect;
    private LegendCanvas listPanel;
    private ScrollPane sp;
    
    GraphData data = null;
    private GraphAttributesWindow attributesWindow;
    PrintJob pjob;
    
    private class LegendCanvas extends Canvas
    {
	private FontMetrics fm;
	private int textheight;
	private ScrollPane sp;
	
	private int width, height;
	
	public LegendCanvas(ScrollPane sp)
	{
	    this.sp = sp;
	    setBackground(Color.black);
	}
	
	public void print(Graphics pg)
	{
	    if (data == null)
		return;
	    
	    setBackground(Color.white);
	    pg.clearRect(0, 0, getSize().width, getSize().height);
	    
	    Dimension d = pjob.getPageDimension();
	    
	    int marginLeft;
	    int marginTop;

	    if (d.width < d.height) {
		marginLeft = (int)(0.7 * d.width / 8.5);     
		marginTop  = (int)(0.7 * d.height / 11.0);
	    } else {
		marginLeft = (int)(0.7 * d.width / 11.0);
		marginTop  = (int)(0.7 * d.height / 8.5);
	    }      
	  
	    int printWidth  = d.width  - 2*marginLeft;
	    int printHeight = d.height - 2*marginTop;
	    
	    pg.translate(marginLeft, marginTop);
		   
	    if (fm == null) {
		fm = pg.getFontMetrics(pg.getFont());
		textheight = fm.getHeight()+2;
	    }   
		 
	    height = textheight;
	    pg.setColor(Color.black);
	    String s = "LEGEND";
	    pg.drawString(s, (printWidth - fm.stringWidth(s))/2, height);
		 
	    height += textheight;
	    
	    width = 0;
		 
	    for (int a=0; a<data.onGraph.length; a++) {
		if ((data.ymode == GraphData.MSGS && 
		     data.onGraph[a].ymode != GraphData.TIME) ||
		    (data.ymode == GraphData.TIME && 
		     data.onGraph[a].ymode != GraphData.MSGS)) {  
		    pg.setColor(data.onGraph[a].color);
		    s = data.onGraph[a].name;
		    if (!(data.onGraph[a].type.equals("%") || 
			  data.onGraph[a].type.equals("Msgs"))) {
			s += " " + data.onGraph[a].type;
		    }
		    if (data.onGraph[a].parent != null) {
			s += "(" + data.onGraph[a].parent + ")";
		    }
		    pg.drawString(s, width, height);   
		    height += textheight;
		    if (height > printHeight) {
			if (width == 0) {
			    height = 2*textheight;
			    width  = printWidth / 2;
			} else {
			    pg.dispose();
			    pg = null;
			    pg = pjob.getGraphics();
			    pg.translate(marginLeft, marginTop);
			    height = textheight;
			    width = 0;
			}
		    }             
		}   
	    } 
	    setBackground(Color.black);        
	}   
		
	public void paint(Graphics g)
	{    
	    if (data == null)
		return;
			
	    if (fm == null) {
		fm = g.getFontMetrics(g.getFont());
		textheight = fm.getHeight()+2;
	    }   
		 
	    height = textheight;
	    width = 0;

	    g.setColor(MainWindow.runObject[myRun].background);
	    g.fillRect(0, 0, getSize().width, getSize().height);
	    
	    for (int a=0; a<data.onGraph.length; a++) {
		if ((data.ymode == GraphData.MSGS && 
		     data.onGraph[a].ymode != GraphData.TIME) ||
		    (data.ymode == GraphData.TIME && 
		     data.onGraph[a].ymode != GraphData.MSGS)) {   
		    g.setColor(data.onGraph[a].color);
		    String s = data.onGraph[a].name;
		    if (!(data.onGraph[a].type.equals("%") || 
			  data.onGraph[a].type.equals("Msgs"))) {
			s += " " + data.onGraph[a].type;
		    }
		    if (data.onGraph[a].parent != null) {
			s += "(" + data.onGraph[a].parent + ")";
		    }
		    g.drawString(s, 0, height);   
		    height += textheight;
		    if (fm.stringWidth(s) > width) {
			width = fm.stringWidth(s);
		    }
		}
	    }              
	    if (sp.getViewportSize().width > width) {
		width = sp.getViewportSize().width;
	    }
	    if (sp.getViewportSize().height> height) {
		height = sp.getViewportSize().height;
	    }
	    //setSize(width, height);
	    //sp.validate();
	}                    
    }      
     
    public GraphLegendPanel(Frame parent)
    {
	this.parent = parent;
	setBackground(Color.lightGray);
	  
	sp        = new ScrollPane();
	listPanel = new LegendCanvas(sp);
  
	bSelect   = new Button("Select Display Items");
	  
	sp.add(listPanel);
	sp.setBackground(Color.lightGray);
	bSelect.addActionListener(this);
	
	GridBagLayout gbl = new GridBagLayout();
	gbc = new GridBagConstraints();
	
	setLayout(gbl);
	  
	gbc.fill = GridBagConstraints.BOTH;
	gbc.insets = new Insets(5, 5, 5, 5);
	Util.gblAdd(this,new Label("LEGEND",Label.CENTER), gbc, 0,0, 1,1, 1,0);
	Util.gblAdd(this, sp,      gbc, 0,1, 1,1, 1,10);
	Util.gblAdd(this, bSelect, gbc, 0,2, 1,1, 1,0);
   }   

    public void actionPerformed(ActionEvent evt)
    {
	if (data == null)
	    return;
	 
	Button b = (Button) evt.getSource();
	if (b == bSelect) {
	    if (attributesWindow == null) {
		attributesWindow = new GraphAttributesWindow(parent,data);
	    }
	    attributesWindow.setVisible(true);
	}      
    }   

    public void closeAttributesWindow()
    {
	if (attributesWindow != null) {
	    attributesWindow.setVisible(false);
	    attributesWindow.dispose();
	    attributesWindow = null;
	}
    }   

    // Make sure we are tall and skinny
    public Dimension getMinimumSize() {
	return new Dimension(80,100);
    }
   
    public Dimension getPreferredSize() {
	return new Dimension(140,200);
    }
   
    public void paint(Graphics g)
    {
	g.setColor(Color.lightGray);
	g.fillRect(0, 0, getSize().width, getSize().height);
	g.setColor(Color.black);
	g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
	super.paint(g);
    }   

    public void PrintLegend(Graphics pg, PrintJob pjob)
    {
	this.pjob = pjob;
	listPanel.printAll(pg);  
    }   

    public void setGraphData(GraphData data)
    {
	this.data = data;
    }   

    public void UpdateLegend()
    {
	listPanel.repaint();
	sp.validate();
    }   
}
