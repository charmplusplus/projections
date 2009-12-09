
package projections.Tools.Timeline;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.JPanel;

/**
 * @author idooley2
 * 
 *  A simple layoutManager for the AxisPanel.
 * 
 *  All children are positioned at 0,0 and 
 *  
 */
class AxisLayout implements LayoutManager {

	private JPanel sizingPanel;
	
	/** Layout that respects the size of the parent container */
	protected AxisLayout(JPanel sizingPanel) {
		this.sizingPanel = sizingPanel;
	}
	

	public void addLayoutComponent(String name, Component comp) {
		// Typically this isn't used unless strings are used with add
	}

	/* Required by LayoutManager. */
	/*
	 * This is called when the panel is first displayed,
	 * and every time its size changes.
	 * Note: You CAN'T assume preferredLayoutSize or
	 * minimumLayoutSize will be called -- in the case
	 * of applets, at least, they probably won't be.
	 */
	public void layoutContainer(Container parent) {
		
		int width;
		int height;

		// The visible portion is this:
		Insets insets = parent.getInsets();
		width = parent.getWidth() - (insets.left + insets.right);
		height = parent.getHeight() - (insets.top + insets.bottom);
	
		
		// The whole panel size is here:

		int nComps = parent.getComponentCount();
		
		for (int i = 0 ; i < nComps ; i++) {
			Component c = parent.getComponent(i);
				c.setBounds(0,0,width,height);
		}

	}

	public Dimension minimumLayoutSize(Container parent) {
		return sizingPanel.getPreferredSize();
	}

	public Dimension preferredLayoutSize(Container parent) {
		return sizingPanel.getPreferredSize();
	}


	public void removeLayoutComponent(Component comp) {
		// TODO Auto-generated method stub
	}

	public String toString() {
		return getClass().getName();
	}



}
