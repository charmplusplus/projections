package projections.Tools.Timeline;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * @author idooley2
 * 
 * The layoutManager for the NonScrollingPanel 
 *
 */
class NonScrollingLayout implements LayoutManager {

	private AxisPanel axisPanel = null;
	private LabelPanel labelPanel = null;
	private MainPanel mainPanel = null;
	
	/** Layout that respects the size of the parent container */
	public NonScrollingLayout() {
	
	}

	
	void setLabel(LabelPanel l){
		labelPanel = l;
	}
	
	void setAxis(AxisPanel a){
		axisPanel = a;
	}
	
	void setMain(MainPanel m){
		mainPanel = m;
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

		Insets insets = parent.getInsets();
		width = parent.getWidth() - (insets.left + insets.right);
		height = parent.getHeight() - (insets.top + insets.bottom);

//		System.out.println("layoutContainer() to new size "+width+"x"+height);
	
		int column1Width;
		int column2Width;
		
		int row1Height;
		int row2Height;
		
		if(labelPanel==null){
			column1Width=0;
		} else {
			Dimension labelSize = labelPanel.getPreferredSize();
			column1Width = labelSize.width;
		}
		
		if(axisPanel==null){
			row1Height = 0;
		} else {
			Dimension axisSize = axisPanel.getPreferredSize();
			row1Height = axisSize.height;
		}
	
				
		column2Width = width-column1Width;
		row2Height = height-row1Height;
		
		if(axisPanel!=null){
			axisPanel.setBounds(column1Width,0,column2Width,row1Height);
			axisPanel.invalidate();
			axisPanel.repaint();
		}
			
		if(labelPanel!=null){
			labelPanel.setBounds(0,row1Height,column1Width,row2Height);
			labelPanel.invalidate();
			labelPanel.repaint();
		}
		
//		System.out.println("column1Width="+column1Width+" column2Width="+column2Width+" row1Height="+row1Height+" row2Height="+row2Height);

		mainPanel.setBounds(column1Width,row1Height,column2Width,row2Height);
		mainPanel.invalidate();
		mainPanel.repaint();
	}

	public Dimension minimumLayoutSize(Container parent) {
		return new Dimension(0,0);
	}

	public Dimension preferredLayoutSize(Container parent) {
		return new Dimension(parent.getWidth(),parent.getHeight());
	}


	public void removeLayoutComponent(Component comp) {
		// TODO Auto-generated method stub
	}

	public String toString() {
		return getClass().getName();
	}



}
