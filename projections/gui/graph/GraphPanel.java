package projections.gui.graph;
 
import projections.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GraphPanel extends JPanel
    implements ActionListener, ItemListener
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    static int myRun = 0;

    private JPanel mainPanel; 
    private JScrollPane displayPanel;
    private Graph displayCanvas;

    private JRadioButton        cbLineGraph;
    private JRadioButton        cbBarGraph;
    private JRadioButton        cbAreaGraph;
    private JCheckBox           cbStacked;

    private JButton             bIncreaseX;
    private JButton             bDecreaseX;
    private JButton             bResetX;
    private JLabel              lScaleX;
    private JFloatTextField     scaleFieldX;

    private JButton             bIncreaseY;
    private JButton             bDecreaseY;
    private JButton             bResetY;
    private JLabel              lScaleY;
    private JFloatTextField     scaleFieldY;

    public GraphPanel(Graph g)
    {
	setBackground(Color.lightGray);
	displayCanvas = g;		
	createLayout();
    }

    private void createLayout() {
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	
	// Graph Type Panel
	JPanel graphTypePanel = new JPanel();
	graphTypePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "graph type"));

	graphTypePanel.setLayout(gbl);
	gbc.fill = GridBagConstraints.HORIZONTAL;

	ButtonGroup cbgGraphType = new ButtonGroup();
	cbLineGraph = new JRadioButton("Line Graph");
	cbLineGraph.setActionCommand("line");
	cbBarGraph  = new JRadioButton("Bar Graph",  true);
	cbBarGraph.setActionCommand("bar");
	cbAreaGraph = new JRadioButton("Area Graph");
	cbAreaGraph.setActionCommand("area");

	cbStacked = new JCheckBox("Stacked");
	// **CW** need to find a way to make this consistent with Graph.java
	cbStacked.setSelected(true);

	cbgGraphType.add(cbLineGraph);
	cbgGraphType.add(cbBarGraph);
	cbgGraphType.add(cbAreaGraph);

	cbLineGraph.addActionListener(this);
	cbBarGraph.addActionListener(this);
	cbAreaGraph.addActionListener(this);

	cbStacked.addItemListener(this);

	Util.gblAdd(graphTypePanel, cbLineGraph, gbc, 0,0, 1,1, 1,0);
	Util.gblAdd(graphTypePanel, cbBarGraph,  gbc, 1,0, 1,1, 1,0);
	Util.gblAdd(graphTypePanel, cbAreaGraph, gbc, 2,0, 1,1, 1,0);
	Util.gblAdd(graphTypePanel, cbStacked,   gbc, 3,0, 1,1, 1,0);

	// making sure the states are consistent
	if (displayCanvas.getGraphType() == Graph.LINE) {
	    cbLineGraph.setSelected(true); 
	}
	
	// the X scale tools
	JPanel xScalePanel = new JPanel();
	xScalePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "x-scale"));
	lScaleX  = new JLabel("X-Axis Scale: ", SwingConstants.CENTER);
	scaleFieldX = new JFloatTextField(1, 5);
	scaleFieldX.addActionListener(this);
	
	bDecreaseX = new JButton("<<");
	bIncreaseX = new JButton(">>");
	bResetX    = new JButton("Reset");
	bIncreaseX.addActionListener(this);
	bDecreaseX.addActionListener(this);
	bResetX.addActionListener(this);

	xScalePanel.setLayout(gbl);
	gbc.fill = GridBagConstraints.HORIZONTAL;

	Util.gblAdd(xScalePanel, bDecreaseX,  gbc, 0,0, 1,1, 0,0);
	Util.gblAdd(xScalePanel, lScaleX,     gbc, 1,0, 1,1, 0,0);
	Util.gblAdd(xScalePanel, scaleFieldX, gbc, 2,0, 1,1, 1,0);
	Util.gblAdd(xScalePanel, bIncreaseX,  gbc, 3,0, 1,1, 0,0);
	Util.gblAdd(xScalePanel, bResetX,     gbc, 4,0, 1,1, 0,0);

	// the Y scale tools
	JPanel yScalePanel = new JPanel();
	yScalePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "y-scale"));
	lScaleY  = new JLabel("Y-Axis Scale: ", SwingConstants.CENTER);
	scaleFieldY = new JFloatTextField(1, 5);
	scaleFieldY.addActionListener(this);
	
	bDecreaseY = new JButton("<<");
	bIncreaseY = new JButton(">>");
	bResetY    = new JButton("Reset");
	bIncreaseY.addActionListener(this);
	bDecreaseY.addActionListener(this);
	bResetY.addActionListener(this);

	yScalePanel.setLayout(gbl);
	gbc.fill = GridBagConstraints.HORIZONTAL;

	Util.gblAdd(yScalePanel, bDecreaseY,  gbc, 0,0, 1,1, 0,0);
	Util.gblAdd(yScalePanel, lScaleY,     gbc, 1,0, 1,1, 0,0);
	Util.gblAdd(yScalePanel, scaleFieldY, gbc, 2,0, 1,1, 1,0);
	Util.gblAdd(yScalePanel, bIncreaseY,  gbc, 3,0, 1,1, 0,0);
	Util.gblAdd(yScalePanel, bResetY,     gbc, 4,0, 1,1, 0,0);

	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(gbl);
	gbc.fill = GridBagConstraints.BOTH;

	Util.gblAdd(buttonPanel, graphTypePanel, gbc, 0,0, 2,1, 1,0);
	Util.gblAdd(buttonPanel, xScalePanel,    gbc, 0,1, 1,1, 1,0);
	Util.gblAdd(buttonPanel, yScalePanel,    gbc, 1,1, 1,1, 1,0);
	/////// put it together
	setLayout(gbl);
	gbc.fill = GridBagConstraints.BOTH;
	
	mainPanel = new JPanel();
	mainPanel.setLayout(gbl);
	
	// this encapsulating panel is required to apply BoxLayout
	// to the scroll pane so that it works correctly.
	JPanel p = new JPanel();
	p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
	displayPanel = new JScrollPane(displayCanvas);
	p.add(displayPanel);

	Util.gblAdd(mainPanel, p, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(this, mainPanel,   gbc, 0,0, 1,1, 1,1, 5,5,5,5);
	Util.gblAdd(this, buttonPanel, gbc, 0,1, 1,1, 1,0, 2,2,2,2);
    }

    public void actionPerformed(ActionEvent evt)
    {
	// get recorded values
	float oldScaleX = scaleFieldX.getValue();
	float oldScaleY = scaleFieldY.getValue();
	// clean current slate
	float scaleX = 0;
	float scaleY = 0;
	
	if (evt.getSource() instanceof JButton) {
	    JButton b = (JButton) evt.getSource();
 	    if (b == bDecreaseX) {
		scaleX = (float)((int)(oldScaleX * 4)-1)/4;
		if (scaleX < 1.0)
		    scaleX = (float)1.0;
	    } else if (b == bIncreaseX) {
		scaleX = (float)((int)(oldScaleX * 4)+1)/4;
	    } else if (b == bResetX) {
		scaleX = (float)1.0;
	    } else if (b == bDecreaseY) {
		scaleY = (float)((int)(oldScaleY * 4)-1)/4;
		if (scaleY < 1.0)
		    scaleY = (float)1.0;
	    } else if (b == bIncreaseY) {
		scaleY = (float)((int)(oldScaleY * 4)+1)/4;
	    } else if (b == bResetY) {
		scaleY = (float)1.0;
	    }
	    // minimum value is 1.0, this is used to test if
	    // the which flag was set.
	    if ((scaleX != oldScaleX) && (scaleX > 0.0)) {
		scaleFieldX.setText("" + scaleX);
		displayCanvas.setScaleX(scaleX); 
	    }
	    if ((scaleY != oldScaleY) && (scaleY > 0.0)) {
		scaleFieldY.setText("" + scaleY);
		displayCanvas.setScaleY(scaleY); 
	    }
	} else if (evt.getSource() instanceof JFloatTextField) {
	    JFloatTextField field = (JFloatTextField)evt.getSource();
	    // we really won't know if the value has changed or not,
	    // hence the conservative approach.
	    if (field == scaleFieldX) {
		scaleX = oldScaleX;
		displayCanvas.setScaleX(scaleX);
	    } else if (field == scaleFieldY) {
		scaleY = oldScaleY;
		displayCanvas.setScaleY(scaleY);
	    }
	} else if (evt.getSource() instanceof JRadioButton) {
	    if (evt.getActionCommand().equals("line")) {
		displayCanvas.setGraphType(Graph.LINE);
		cbStacked.setEnabled(true);
	    } else if (evt.getActionCommand().equals("bar")) {
		displayCanvas.setGraphType(Graph.BAR);
		cbStacked.setEnabled(true);
	    } else if (evt.getActionCommand().equals("area")) {
		// area graphs are automatically stacked.
		cbStacked.setSelected(true);
		displayCanvas.setStackGraph(true);
		displayCanvas.setGraphType(Graph.AREA);
		// disable the stack checkbox so that the user cannot
		// change this property for now.
		cbStacked.setEnabled(false);
	    }
	}
    }

    public void itemStateChanged(ItemEvent evt) {
	Object source = evt.getItemSelectable();
	if (source == cbStacked) {
	    if (evt.getStateChange() == ItemEvent.SELECTED) {
		displayCanvas.setStackGraph(true);
	    } else {
		displayCanvas.setStackGraph(false);
	    }
	}
    }

   public static void main(String [] args){
        JFrame f = new JFrame();
        JPanel mainPanel;  			//Panel();
	
        int data[]={
	    6,554,612,571,1354,819,385,151,76,54,34,11,2
	};
        f.addWindowListener(new WindowAdapter()
          {
                 public void windowClosing(WindowEvent e)
                 {
                        System.exit(0);
                 }
          });
 
        DataSource ds=new DataSource1D("Histogram",data);
        XAxisFixed xa=new XAxisFixed("Grainsize","ms");
	xa.setLimits(1.0,2.0);
        YAxis ya=new YAxisAuto("Number of Computes","",ds);
	MainWindow.runObject[myRun].foreground = Color.black;
	MainWindow.runObject[myRun].background = Color.white;
        Graph g=new Graph();
        g.setGraphType(Graph.BAR);
        g.setStackGraph(true);
        g.setData(ds,xa,ya);
        mainPanel = new GraphPanel(g);
	JMenuBar mbar = new JMenuBar();
	f.setJMenuBar(mbar);
	JMenu fileMenu = new JMenu("File");
	mbar.add(fileMenu);
	JMenuItem trialMenuItem = new JMenuItem("Trial Item");
	fileMenu.add(trialMenuItem);

        f.getContentPane().add(mainPanel);
        f.pack();
        f.setTitle("Projections");
        f.setVisible(true);
   }
}
