package projections.gui;

import projections.gui.graph.*;

import java.awt.*;
import javax.swing.*;

public class PoseRTDopDisplayPanel extends JPanel
    implements PopUpAble
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private GraphPanel graphPanel;
    private JPanel controlPanel;
    private Graph graph;
    
    public PoseRTDopDisplayPanel() {
        createLayout();
    }

    private void createLayout() {
	GridBagConstraints gbc = new GridBagConstraints();
	GridBagLayout gbl = new GridBagLayout();
	
	gbc.fill = GridBagConstraints.BOTH;
	setLayout(gbl);

	graph = new Graph();
	graphPanel = new GraphPanel(graph);

	// control panel items
	controlPanel = new JPanel();
	controlPanel.setLayout(gbl);
	
	Util.gblAdd(this, graphPanel,     gbc, 0,0, 1,1, 1,1, 5,5,5,5);
	Util.gblAdd(this, controlPanel,   gbc, 0,1, 1,1, 1,0);
    }

    public void setGraphData(int[][] data, 
			     long intervalSize, 
			     int startInterval, int endInterval) {
	DataSource2D datasource = 
	    new DataSource2D("Degree of Parallism: " +
			     U.t(startInterval*intervalSize) +
			     " to " +
			     U.t(endInterval*intervalSize),
			     data, this);
	datasource.setColors(new Color[] { Color.green, Color.blue });
	XAxisFixed xaxis = new XAxisFixed("Time: interval size = " +
					  U.t(intervalSize),
					  "Time");
	xaxis.setLimits((double)startInterval*intervalSize,
			(double)intervalSize);
	YAxisAuto yaxis = new YAxisAuto("Number of Simultaneous Events",
					"", datasource);
	graph.setData(datasource,xaxis,yaxis);
    }

    public void refreshGraph() {
	graph.repaint();
    }

    public String[] getPopup(int xVal, int yVal) {
	if ((xVal < 0) || (yVal < 0)) {
	    return null;
	}
	return null;
    }	
}
