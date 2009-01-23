package projections.gui.Timeline;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Saves images that are painted by any given JPanel
 * 
 * @note This should be used to print a timeline or to print a timeline portion inside other visualization tools.
 */

public class SaveImage {
	
	private JPanel view;
	private BufferedImage image;
	
	/** Create the scrollable panel with the three provided panels. */
	public SaveImage(JPanel view_) {
		view = view_;
	}

	private void generateImage(){
//		 Create an image for the constructed panel.
		int width = view.getWidth();
        int height = view.getHeight();

        System.out.println("Saving timeline image of size "+width+"x"+height);
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        view.paint(g);
        g.dispose();
	}
	
//	public Image getImage(){
//		generateImage();
//		return image;
//	}
	
	public void saveImagePNG(String filename){
		generateImage();
		
        try {
          ImageIO.write(image, "png", new File(filename));
        }
        catch(IOException ioe) {
          System.out.println(ioe.getMessage());
        }
	}
	
	public void saveImageJPEG(String filename){
		generateImage();
		
        try {
          ImageIO.write(image, "jpg", new File(filename));
        }
        catch(IOException ioe) {
          System.out.println(ioe.getMessage());
        }
	}
	
};
