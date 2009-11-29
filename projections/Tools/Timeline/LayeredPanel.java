package projections.Tools.Timeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Dimension;

import javax.swing.*;

/** A scrollable layered panel which will contain a main panel and one or more overlay layers
 * 
 * Depth=10 corresponds to the main panel
 * Depth=20 corresponds to the overlayed display for the selection
 * 
 * The size for this panel is acquired from the main panel it contains
 * 
 */

public class LayeredPanel extends JPanel  implements Scrollable
{
	
	private Data data; // Probably unneeded, but I think the layout manager may use this
	
	JLayeredPane jLayeredPane = null;
	
	JPanel mainPanel=null;
	JPanel overlayPanel=null;
	
	public LayeredPanel(Data data, JPanel main, JPanel overlay, LayoutManager lay)
	{
		this.data = data;
		this.mainPanel = main;
		this.overlayPanel = overlay;
		
		jLayeredPane = new JLayeredPane();
		jLayeredPane.setLayout(lay);
		
		// Add the layers to the JLayeredPane
		jLayeredPane.add(main, new Integer(10));
		jLayeredPane.add(overlay,  new Integer(20));
		
		// Add the layered panel to me
		this.setLayout(new BorderLayout());
		this.add(jLayeredPane, BorderLayout.CENTER);
		
	}   

	public void repaint(){
		super.repaint();
		if(mainPanel != null)
			mainPanel.repaint();
		if(overlayPanel != null)
			overlayPanel.repaint();
		if(jLayeredPane != null)
			jLayeredPane.repaint();
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}

	
	public Dimension getPreferredSize() {
		if(mainPanel != null){
			return new Dimension(getWidth(), mainPanel.getPreferredSize().height);
		} else {
			return new Dimension(getWidth(),getHeight());
		}
	}
	
	
	public Dimension getMinimumSize() {
		return mainPanel.getPreferredSize();
	}
	

	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction){
		return 5;
	}

	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction){
		return 10;
	}

	public Dimension getPreferredScrollableViewportSize(){
		return getPreferredSize();
	}

	public boolean getScrollableTracksViewportWidth(){
		return false;
	}

	public boolean getScrollableTracksViewportHeight(){
		return false;
	}
	
}
