package projections.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/* **************************************************
 * MainSummaryGraphPanel.java
 * Chee Wai Lee - 10/29/2002
 *
 * This is a tabbed panel that manages (in the future)
 * multiple runs loaded into projections.
 *
 * **************************************************/

public class MainSummaryGraphPanel extends JTabbedPane {
    // it is initially empty and is added to by MainWindow's open file(s)
    // menu option.

    private MainWindow parent;
    private MainRunStatusPanel statusPanel;
    private JPanel fillerPanel;
    private boolean empty;

    public MainSummaryGraphPanel(MainWindow parent,
				 MainRunStatusPanel statusPanel) {
	super();
	this.parent = parent;
	this.statusPanel = statusPanel;

	addChangeListener(this.statusPanel);
	fillerPanel = new JPanel();
	fillerPanel.setBackground(Color.black);
	fillerPanel.setPreferredSize(new Dimension(ScreenInfo.screenWidth/3, 
						   ScreenInfo.screenHeight/3));
	setEmpty();
    }

    // overrides the multiple addition interface of JTabbedPane with
    // a simple add method that simply adds each projections run
    // to the end of the list of tabbed panels.
    public void add(String title, Component panel, String tooltip) {
	if (empty) {
	    remove(0);
	    empty = false;
	}
	insertTab(title, null, panel, tooltip, getTabCount());
	setForegroundAt(0, Color.black);
	((JComponent)panel).revalidate();
	parent.validate();
    }

    public void removeCurrent() {
	remove(getSelectedIndex());
	if (getTabCount() == 0) {
	    setEmpty();
	}
    }

    public void removeAll() {
	int startTabCount = getTabCount();

	for (int i=0;i<startTabCount;i++) {
	    removeCurrent();
	}
    }

    public boolean isEmpty() {
	return empty;
    }

    private void setEmpty() {
	empty = true;
	// bypass standard add function.
	insertTab("No Summary Data Available", null, fillerPanel, 
		  "No Summary Data loaded", 0);
	// make this an obvious warning color
	setForegroundAt(0, Color.red);
	setSelectedIndex(0);
	fillerPanel.revalidate();
	parent.validate();
    }

    // setting it's minimum size requirments 1/3 of screen width & height
    public Dimension getMinimumSize() {
	return new Dimension(ScreenInfo.screenWidth/3, 
			     ScreenInfo.screenHeight/3);
    }

}
