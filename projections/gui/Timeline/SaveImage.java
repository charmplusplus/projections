package projections.gui.Timeline;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
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


	private BufferedImage generateImage(JPanel panelToRender){
		//		 Create an image for the constructed panel.
		int width = panelToRender.getWidth();
		int height = panelToRender.getHeight();

		System.out.println("Saving timeline image of size "+width+"x"+height);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		panelToRender.paint(g);
		g.dispose();
		return image;
	}

	public void saveImage(String filename, String format, RenderedImage image){

		try {
			ImageIO.write(image, format, new File(filename));
		}
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}

	public void saveToFileChooserSelection(JPanel panelToRender){
		BufferedImage image = generateImage(panelToRender);
		
		try{	
			// Create a file chooser so the user can choose where to save the image
			JFileChooser fc = new JFileChooser();
			ImageFilter imageFilter = new ImageFilter();
			fc.setFileFilter(imageFilter);
			fc.setSelectedFile(new File("./TimelineScreenshot.png"));

			int returnVal = fc.showSaveDialog(null);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();

				if(imageFilter.isJPEG(file))       		
					saveImage(file.getCanonicalPath(), "jpg", image);

				if(imageFilter.isPNG(file))       		
					saveImage(file.getCanonicalPath(), "png", image);

			} else {
				// Save command cancelled by user
			}
		} catch (IOException e){
			JOptionPane.showMessageDialog(null, this, "Error occurred while saving file:" + e.getLocalizedMessage(), 0);
		}	
	}

}
