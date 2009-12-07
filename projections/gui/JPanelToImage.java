package projections.gui;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import projections.Tools.Timeline.ImageFilter;

/**
 * Renders & Saves images for any displayed JPanel. The JPanel must already be layed out.
 */

public class JPanelToImage {


	/** Create an image and paint the panel into the image. */
	public static BufferedImage generateImage(JPanel panelToRender){
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

	/** Save an image into a file. */
	public static void saveImage(String filename, String format, RenderedImage image){

		try {
			ImageIO.write(image, format, new File(filename));
		}
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}

	
	/** Generate an image of the panel and save it into a file chosen by the user in a file chooser dialog box. */
	public static void saveToFileChooserSelection(JPanel panelToRender, String dialogTitle, String defaultFilename){
		BufferedImage image = generateImage(panelToRender);
		saveToFileChooserSelection(image, dialogTitle, defaultFilename);
	}
	
	/** Generate an image of the panel and save it into a file chosen by the user in a file chooser dialog box. */
	public static void saveToFileChooserSelection(BufferedImage image, String dialogTitle, String defaultFilename){
		try{	
			// Create a small JPanel with a preview of the image
			ImageIcon icon;
			if(image.getWidth() < image.getHeight()){
				// Tall images should be scaled to be 200 px tall
				icon = new ImageIcon(image.getScaledInstance(-1, 200, Image.SCALE_SMOOTH ));
			} else {
				// Wide images should be scaled to be 200 px wide
				icon = new ImageIcon(image.getScaledInstance(200, -1, Image.SCALE_SMOOTH ));
			}
			
			JLabel miniPicture = new JLabel(icon);			
			JPanel previewPanel = new JPanel();
			previewPanel.setLayout(new BorderLayout());
			previewPanel.add(new JLabel("Preview:"), BorderLayout.NORTH);			
			previewPanel.add(miniPicture, BorderLayout.CENTER);
			
			// Create a file chooser so the user can choose where to save the image
			JFileChooser fc = new JFileChooser();
			ImageFilter imageFilter = new ImageFilter();
			fc.setFileFilter(imageFilter);
			fc.setSelectedFile(new File(defaultFilename));
			fc.setAccessory(previewPanel);
			fc.setDialogTitle(dialogTitle);

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
			JOptionPane.showMessageDialog(null, null, "Error occurred while saving file:" + e.getLocalizedMessage(), 0);
		}	
	}
	

}
