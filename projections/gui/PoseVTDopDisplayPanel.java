package projections.gui;

import projections.gui.graph.*;

import java.awt.*;
import javax.swing.*;

public class PoseVTDopDisplayPanel extends JPanel
    implements PopUpAble
{
    private GraphPanel graphPanel;
    private JPanel controlPanel;
    private Graph graph;
    
    public PoseVTDopDisplayPanel() {
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

    public void setGraphData(int[] data, 
			     long intervalSize, 
			     int startInterval, int endInterval) {
	DataSource1D datasource = 
	    new DataSource1D("Degree of Parallism (Model): " +
			     (startInterval*intervalSize) +
			     " to " +
			     (endInterval*intervalSize),
			     data, this);
	datasource.setColors(new Color[] { Color.cyan });
	XAxisFixed xaxis = new XAxisFixed("Virtual Time: interval size = " +
					  intervalSize,
					  "");
	xaxis.setLimits((double)startInterval*intervalSize,
			(double)intervalSize);
	YAxisAuto yaxis = 
	    new YAxisAuto("Number of Simultaneous Model Events",
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
