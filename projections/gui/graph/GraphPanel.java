package projections.gui.graph;
 
import projections.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GraphPanel extends JPanel
    implements ActionListener, ItemListener
{
    private static final Color BACKGROUND = Color.black;
    private static final Color FOREGROUND = Color.white;
    
    private JPanel mainPanel; 
    private JScrollPane displayPanel;
    private Graph displayCanvas;

    private Button             bIncreaseX;
    private Button             bDecreaseX;
    private Button             bResetX;
    private Checkbox           cbLineGraph;
    private Checkbox           cbBarGraph;
    private Label              lScale;
    private FloatTextField     scaleField;
    
    public GraphPanel(Graph g)
    {
	addComponentListener(new ComponentAdapter()
	    {
		public void componentResized(ComponentEvent e) {
		    if (mainPanel != null) {
			displayCanvas.repaint();
		    }
		}
	    });
	
	setBackground(Color.lightGray);
	displayCanvas = g;		
	createLayout();
    }

    private void createLayout() {
	
	lScale  = new Label("X-Axis Scale: ", Label.CENTER);
	
	scaleField = new FloatTextField(1, 5);
	scaleField.addActionListener(this);
	
	bDecreaseX = new Button("<<");
	bIncreaseX = new Button(">>");
	bResetX    = new Button("Reset");
	bIncreaseX.addActionListener(this);
	bDecreaseX.addActionListener(this);
	bResetX.addActionListener(this);
	
	CheckboxGroup cbgGraphType = new CheckboxGroup();
	cbLineGraph = new Checkbox("Line Graph", false,  cbgGraphType);
	cbBarGraph  = new Checkbox("Bar Graph",  true, cbgGraphType);
	cbLineGraph.addItemListener(this);
	cbBarGraph.addItemListener(this);
	
	if (displayCanvas.getGraphType() == Graph.LINE) {
	    cbLineGraph.setState(true); 
	    cbBarGraph.setState(false);
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
	
	displayPanel = new JScrollPane(displayCanvas);
	Util.gblAdd(mainPanel, displayPanel, gbc, 0,0, 1,1, 1,1);
	
	Util.gblAdd(this, mainPanel,   gbc, 0,0, 1,1, 1,1, 5,5,5,5);
	Util.gblAdd(this, buttonPanel, gbc, 0,1, 1,1, 1,0, 2,2,2,2);
    }
    
    //Make sure we aren't made too tiny
    public Dimension getMinimumSize() {return new Dimension(500,400);}
    public Dimension getPreferredSize() {return new Dimension(500,400);}

    public void actionPerformed(ActionEvent evt)
    {
	float oldScale = scaleField.getValue();
	float scale = 0;
	
	if (evt.getSource() instanceof Button) {
	    Button b = (Button) evt.getSource();
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
	}
    }

    public void itemStateChanged(ItemEvent evt)
    {
	Checkbox c = (Checkbox) evt.getSource();
	if (c == cbLineGraph) {
	    displayCanvas.setGraphType(Graph.LINE);
	} else if (c == cbBarGraph) {
	    displayCanvas.setGraphType(Graph.BAR);
	}
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
        f.setSize(800,600);
        f.setTitle("Projections");
        f.setVisible(true);

	}
}
