package projections.gui;

import projections.gui.graph.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  AreaGraphPanel
 *  adapted by Chee Wai Lee from GraphPanel
 *  3/6/2003
 *
 *  Wondering if there should be some kind of inheritance tree for this
 *  set of GUI objects.
 */
public class AreaGraphPanel extends JPanel
    implements ActionListener, AdjustmentListener
{
    private static final Color BACKGROUND = Color.black;
    private static final Color FOREGROUND = Color.white;

    private JPanel mainPanel; 
    private JScrollPane displayPanel;
    private Graph displayCanvas;

    // X-scale control
    private Button bIncreaseX;
    private Button bDecreaseX;
    private Button bResetX;

    // Y-scale control
    private Button bIncreaseY;
    private Button bDecreaseY;
    private Button bResetY;

    private Label xScaleLabel;
    private Label yScaleLabel;
    private FloatTextField xScaleField;
    private FloatTextField yScaleField;

    public AreaGraphPanel(Graph graphPanel)
    {
	addComponentListener(new ComponentAdapter()
	    {
		public void componentResized(ComponentEvent e)
		{
		    if (mainPanel != null)
                        {
			    setAllBounds();
			    displayCanvas.repaint();
                        }
		}
	    });
 	setBackground(BACKGROUND);
	displayCanvas = graphPanel;
	displayCanvas.setGraphType(Graph.AREA);
	createLayout();
    }
    
    private void createLayout() {
	
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
 
	gbc.fill = GridBagConstraints.NONE;

	// X-scale control group
	JPanel xScalePanel = new JPanel();
	xScalePanel.setLayout(gbl);

	xScaleLabel = new Label("X-Axis Scale: ", Label.CENTER);
	
	xScaleField = new FloatTextField(1, 5);
	xScaleField.addActionListener(this);
	xScaleField.setText("1.0"); 
	
	bDecreaseX = new Button("<<");
	bIncreaseX = new Button(">>");
	bResetX    = new Button("Reset");
	bIncreaseX.addActionListener(this);
	bDecreaseX.addActionListener(this);
	bResetX.addActionListener(this);
	
	Util.gblAdd(xScalePanel, bDecreaseX,  gbc, 0,0, 1,1, 1,0);
	Util.gblAdd(xScalePanel, xScaleLabel, gbc, 1,0, 1,1, 1,0);
	Util.gblAdd(xScalePanel, xScaleField, gbc, 2,0, 1,1, 1,0);
	Util.gblAdd(xScalePanel, bIncreaseX,  gbc, 3,0, 1,1, 1,0);
	Util.gblAdd(xScalePanel, bResetX,     gbc, 4,0, 1,1, 1,0);

	xScalePanel.setBorder(BorderFactory.createLineBorder(Color.black));
	
	// Y-scale control group
	JPanel yScalePanel = new JPanel();
	yScalePanel.setLayout(gbl);

	yScaleLabel = new Label("Y-Axis Scale: ", Label.CENTER);
	
	yScaleField = new FloatTextField(1, 5);
	yScaleField.addActionListener(this);
	yScaleField.setText("1.0"); 
	
	bDecreaseY = new Button("<<");
	bIncreaseY = new Button(">>");
	bResetY    = new Button("Reset");
	bIncreaseY.addActionListener(this);
	bDecreaseY.addActionListener(this);
	bResetY.addActionListener(this);
	
	Util.gblAdd(yScalePanel, bDecreaseY,  gbc, 0,0, 1,1, 1,0);
	Util.gblAdd(yScalePanel, yScaleLabel, gbc, 1,0, 1,1, 1,0);
	Util.gblAdd(yScalePanel, yScaleField, gbc, 2,0, 1,1, 1,0);
	Util.gblAdd(yScalePanel, bIncreaseY,  gbc, 3,0, 1,1, 1,0);
	Util.gblAdd(yScalePanel, bResetY,     gbc, 4,0, 1,1, 1,0);

	yScalePanel.setBorder(BorderFactory.createLineBorder(Color.black));

	setLayout(gbl);

	gbc.fill = GridBagConstraints.BOTH;

	mainPanel = new JPanel();
	mainPanel.setLayout(gbl);
	displayPanel = new JScrollPane(displayCanvas);
	Util.gblAdd(mainPanel, displayPanel, gbc, 0,0, 1,1, 1,1, 0,0,0,0);

	Util.gblAdd(this, mainPanel,   gbc, 0,0, 2,1, 1,1, 10,10,10,10);
	Util.gblAdd(this, xScalePanel, gbc, 0,1, 1,1, 1,0, 2,2,2,2);
	Util.gblAdd(this, yScalePanel, gbc, 1,1, 1,1, 1,0, 2,2,2,2);
    }
    
    //Make sure we aren't made too tiny
    public Dimension getMinimumSize() {return new Dimension(150,100);}
    public Dimension getPreferredSize() {return new Dimension(450,350);}
    
    public void actionPerformed(ActionEvent evt)
    {
	float xScale = xScaleField.getValue();
	float yScale = yScaleField.getValue();
	
	if (evt.getSource() instanceof Button) {
	    Button b = (Button) evt.getSource();
	    
	    if (b == bDecreaseX) {
		xScale = (float)((int)(xScale * 4)-1)/4;
		if (xScale < 1.0) {
		    xScale = (float)1.0;
		}
	    } else if (b == bIncreaseX) {
		xScale = (float)((int)(xScale * 4)+1)/4;
	    } else if (b == bResetX) {
		xScale = (float)1.0;
	    } else if (b == bDecreaseY) {
		yScale = (float)((int)(yScale * 4)-1)/4;
		if (yScale < 1.0) {
		    yScale = (float)1.0;
		}
	    } else if (b == bIncreaseY) {
		yScale = (float)((int)(yScale * 4)+1)/4;
	    } else if (b == bResetY) {
		yScale = (float)1.0;
	    }
	    xScaleField.setText("" + xScale);
	    yScaleField.setText("" + yScale);
	}
	displayCanvas.setScale((double)xScale); 
	// **CW** for now, do nothing with the y scale.
	setAllBounds();
	displayCanvas.repaint();
    }
    
    public void adjustmentValueChanged(AdjustmentEvent evt)
    {
	displayCanvas.repaint();
    }

    public void setAllBounds()
    {
	//// set the sizes
	int mpw, mph, sbh, dcw, dch;
	mpw = mainPanel.getSize().width;
	mph = mainPanel.getSize().height;
	if(mpw == 0 || mph == 0)
	    return;
	
	sbh = 20;
	
	dcw = mpw-30;
	dch = mph - 30 - sbh;
	
	// --> set the bounds
	// must set the bounds for the axes before the display canvas so that
	// the scales are set appropriately.
	
	displayCanvas.setBounds(30, 30, dcw, dch);
    }

    public void setData(DataSource dataSource, XAxis xAxis, YAxis yAxis) {
	displayCanvas.setData(dataSource, xAxis, yAxis);
	displayCanvas.setGraphType(Graph.AREA);
	repaint();
    }

   public static void main(String [] args){
        JFrame f = new JFrame();
        JPanel mainPanel;  			//Panel();
	
        double data[][]={{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4}};
 
        f.addWindowListener(new WindowAdapter()
          {
                 public void windowClosing(WindowEvent e)
                 {
                        System.exit(0);
                 }
          });
 
        DataSource ds=new DataSource2D("Histogram",data);
        XAxis xa=new XAxisFixed("Entry Point Execution Time","ms");
        YAxis ya=new YAxisAuto("Count","",ds);
        Graph g=new Graph();
        g.setGraphType(Graph.AREA);
        g.setData(ds,xa,ya);
        mainPanel = new AreaGraphPanel(g);
	JMenuBar mbar = new JMenuBar();
	f.setJMenuBar(mbar);
	JMenu fileMenu = new JMenu("File");
	mbar.add(fileMenu);
	JMenuItem trialMenuItem = new JMenuItem("Trial Item");
	fileMenu.add(trialMenuItem);

	f.setContentPane(mainPanel);

        f.pack();
        f.setSize(800,600);
        f.setTitle("Projections");
        f.setVisible(true);

   }
}
