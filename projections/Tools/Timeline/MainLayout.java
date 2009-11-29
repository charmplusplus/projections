/**
 * 
 */
package projections.Tools.Timeline;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * @author idooley2
 * 
 * The layoutManager for the MainPanel 
 *
 */
public class MainLayout implements LayoutManager {

	private Data data; // desired sizes are in here

	/** Layout that respects the size of the parent container */
	public MainLayout(Data data_) {
		data = data_;
	}



	public void addLayoutComponent(String name, Component comp) {
		// Typically this isn't used unless strings are used with add
	}


	/* Required by LayoutManager. */
	/*
	 * This is called when the panel is first displayed,
	 * and every time its size changes.
	 * 
	 * When this is used in a JScrollPane, this will be revalidated
	 * and layed out again, but the preferred size for the axis
	 * doesn't get updated until this is revalidated.
	 * 
	 */
	public void layoutContainer(Container parent) {

		int width;
		int height;

		Insets insets = parent.getInsets();
		width = parent.getWidth() - (insets.left + insets.right);
		height = parent.getHeight() - (insets.top + insets.bottom);

		int nComps = parent.getComponentCount();
		for (int i = 0 ; i < nComps ; i++) {
			Component c = parent.getComponent(i);
			// Determine if the component is a EntryMethodObject

			
			if ( c instanceof EntryMethodObject ) {
				((EntryMethodObject)c).setLocationAndSize((width)); // setBounds on child
			} 
			else if ( c instanceof UserEventObject ) {
				((UserEventObject)c).setLocationAndSize(data, (width));	// setBounds on child	
			} 
			else if ( c instanceof MainPanelForeground ) {
				((MainPanelForeground)c).setBounds(0, 0, width, height );
			} 
			else if ( c instanceof MainPanelBackground ) {
				((MainPanelBackground)c).setBounds(0, 0, width, height );
			} 
			else {
//				System.out.println("MainLayout found unknown type of child object");
			}	

		}

	}

	public Dimension minimumLayoutSize(Container parent) {
		return new Dimension(300,300);
	}

	public Dimension preferredLayoutSize(Container parent) {
		return new Dimension(300,300);
	}


	public void removeLayoutComponent(Component comp) {
		// TODO Auto-generated method stub
	}

	public String toString() {
		return getClass().getName();
	}



}
