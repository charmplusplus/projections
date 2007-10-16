package projections.gui;

import java.awt.*;
import javax.swing.*;

public class BackGroundImagePanel extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Image bgimage = null;
    private boolean tile;

    public BackGroundImagePanel(Image bgimage, boolean tile) {
	this.bgimage = bgimage;
	this.tile = tile;
    }
    
    /**
     *  Wrapper constructor. Default to tiling.
     */
    public BackGroundImagePanel(Image bgimage) {
	this(bgimage, true);
    }

    public void paintComponent(Graphics g)
    {
	super.paintComponent(g);
	if (bgimage!=null)
	    wallPaper(this, g, bgimage);
    }
    
    private void wallPaper(Component component, Graphics  g, Image image)
    {
	Dimension compsize = component.getSize();
	waitForImage(component, image);
	
	int patchW = image.getWidth(component);
	int patchH = image.getHeight(component);

	if (tile) {
	    for (int r=0; r < compsize.width; r += patchW) {
		for(int c=0; c < compsize.height; c += patchH) {
		    g.drawImage(image, r, c, component);
		}
	    }
	} else {
	    // fixed size image.
	    setPreferredSize(new Dimension(patchW, patchH));
	    g.drawImage(image, 0, 0, component);
	}
    }
    
    private void waitForImage(Component component, Image image)
    {
	MediaTracker tracker = new MediaTracker(component);
	try {
	    tracker.addImage(image, 0);
	    tracker.waitForID(0);
	} catch(InterruptedException e) {
	    e.printStackTrace();
	}
    }
}
