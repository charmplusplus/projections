package projections.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;

import javax.swing.JPanel;

class BackGroundImagePanel extends JPanel {
  
	private Image bgimage = null;
    private boolean tile;

    protected BackGroundImagePanel(Image bgimage, boolean tile) {
	this.bgimage = bgimage;
	this.tile = tile;
    }
    
    /**
     *  Wrapper constructor. Default to tiling.
     */
    protected BackGroundImagePanel(Image bgimage) {
	this(bgimage, true);
    }

    public void paintComponent(Graphics g)
    {
	super.paintComponent(g);
	if (bgimage!=null) {
	    wallPaper(this, g, bgimage);
	}
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
	    // draw center scaled image.
	    double imgAspect = patchH*1.0/patchW;
	    double pnlAspect = compsize.height*1.0/compsize.width;

	    if (pnlAspect > imgAspect) {
		// panel has more height than width compared to image
		Image scaledImg = 
		    image.getScaledInstance(compsize.width, -1, 
					    Image.SCALE_SMOOTH);
		waitForImage(component, scaledImg);
		int scaledImgH = scaledImg.getHeight(component);
		int heightOffset = (compsize.height - scaledImgH)/2;
		g.drawImage(scaledImg, 0, heightOffset, component);
	    } else {
		Image scaledImg =
		    image.getScaledInstance(-1, compsize.height, 
					    Image.SCALE_SMOOTH);
		waitForImage(component, scaledImg);
		int scaledImgW = scaledImg.getWidth(component);
		int widthOffset = (compsize.width - scaledImgW)/2;
		g.drawImage(scaledImg, widthOffset, 0, component);
	    }
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
