package projections.gui.graph;
 
import projections.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GraphPanel extends JPanel
    implements ActionListener
{
    private static final Color BACKGROUND = Color.black;
    private static final Color FOREGROUND = Color.white;
    
    private JPanel mainPanel; 
    private JScrollPane displayPanel;
    private Graph displayCanvas;

    private JButton             bIncreaseX;
    private JButton             bDecreaseX;
    private JButton             bResetX;
    private JRadioButton        cbLineGraph;
    private JRadioButton        cbBarGraph;
    private JLabel              lScale;
    private FloatTextField     scaleField;
    
    public GraphPanel(Graph g)
    {
	setBackground(Color.lightGray);
	displayCanvas = g;		
	createLayout();
    }

    private void createLayout() {
	
	lScale  = new JLabel("X-Axis Scale: ", SwingConstants.CENTER);
	
	scaleField = new FloatTextField(1, 5);
	scaleField.addActionListener(this);
	
	bDecreaseX = new JButton("<<");
	bIncreaseX = new JButton(">>");
	bResetX    = new JButton("Reset");
	bIncreaseX.addActionListener(this);
	bDecreaseX.addActionListener(this);
	bResetX.addActionListener(this);
	
	ButtonGroup cbgGraphType = new ButtonGroup();
	cbLineGraph = new JRadioButton("Line Graph");
	cbLineGraph.setActionCommand("line");
	cbBarGraph  = new JRadioButton("Bar Graph",  true);
	cbBarGraph.setActionCommand("bar");
	cbgGraphType.add(cbLineGraph);
	cbgGraphType.add(cbBarGraph);

	cbLineGraph.addActionListener(this);
	cbBarGraph.addActionListener(this);
	
	// making sure the states are consistent
	if (displayCanvas.getGraphType() == Graph.LINE) {
	    cbLineGraph.setSelected(true); 
	}
	
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	
	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(gbl);
	gbc.fill = GridBagConstraints.NONE;
	Util.gblAdd(buttonPanel, cbLineGraph, gbc, 0,0, 1,1, 1,0);
	Util.gblAdd(buttonPanel, cbBarGraph,  gbc, 1,0, 1,1, 1,0);
	Util.gblAdd(buttonPanel, bDecreaseX,  gbc, 2,0, 1,1, 1,0);
	Util.gblAdd(buttonPanel, lScale,      gbc, 3,0, 1,1, 1,0);
	Util.gblAdd(buttonPanel, scaleField,  gbc, 4,0, 1,1, 1,0);
	Util.gblAdd(buttonPanel, bIncreaseX,  gbc, 5,0, 1,1, 1,0);
	Util.gblAdd(buttonPanel, bResetX,     gbc, 6,0, 1,1, 1,0);
	
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
	float oldScale = scaleField.getValue();
	float scale = 0;
	
	if (evt.getSource() instanceof JButton) {
	    JButton b = (JButton) evt.getSource();
 	    if (b == bDecreaseX) {
		scale = (float)((int)(oldScale * 4)-1)/4;
		if (scale < 1.0)
		    scale = (float)1.0;
	    } else if (b == bIncreaseX) {
		scale = (float)((int)(oldScale * 4)+1)/4;
	    } else if (b == bResetX) {
		scale = (float)1.0;
	    }
	    if (scale != oldScale) {
		scaleField.setText("" + scale);
		displayCanvas.setScale((double)scale); 
	    }
	} else if (evt.getSource() instanceof FloatTextField) {
	    // we really won't know if the value has changed or not,
	    // hence the conservative approach.
	    scale = oldScale;
	    displayCanvas.setScale((double)scale);
	} else if (evt.getSource() instanceof JRadioButton) {
	    if (evt.getActionCommand().equals("line")) {
		displayCanvas.setGraphType(Graph.LINE);
	    } else if (evt.getActionCommand().equals("bar")) {
		displayCanvas.setGraphType(Graph.BAR);
	    }
	}
    }

   public static void main(String [] args){
        JFrame f = new JFrame();
        JPanel mainPanel;  			//Panel();
	
        double data[][]={{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4},{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4},{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4},{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4},{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4},{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4},{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4},{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4},{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4},{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4},{20,2100,49,3},{25,34,8,10},{23,20,54,3},{2000,27,4,40},{25,21,7,4},{20,21,8,10},{24,26,44,4},{22,26,20,5},{29,29,5,20},{20,21,8,7},{24,20,10,3},{21,25,6,8},{34,23,11,11},{20,20,20,20},{27,25,4,5},{21,20,5,7},{21,24,5,8},{26,22,5,3},{26,29,7,10},{29,20,8,6},{21,24,9,4}};
 
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
        g.setGraphType(Graph.LINE);
        g.setBarGraphType(Graph.UNSTACKED);
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
