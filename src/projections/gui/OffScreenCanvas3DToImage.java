package projections.gui;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import projections.Tools.Timeline.ImageFilter;
import projections.Tools.TopologyDisplay.OffScreenCanvas3D;

/**
 * Renders and Saves images for any displayed OffScreenCanvas3D.
 */
public class OffScreenCanvas3DToImage {
	/**
	 * Create an image and paint the Canvas3D into the image.
	 */
	public static BufferedImage generateImage(OffScreenCanvas3D canvasToRender, int width, int height){
		return canvasToRender.generateImage(width, height);
	}

	/**
	 * Save an image into a file.
	 */
	private static void saveImage(String filename, String format, RenderedImage image){

		try {
			ImageIO.write(image, format, new File(filename));
		}
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}

	/**
	 * Generate an image of the canvas and save it into a file chosen by the user in a 
	 * file chooser dialog box.
	 */
	public static void saveToFileChooserSelection(OffScreenCanvas3D canvasToRender, String dialogTitle, String fileName,
			  														int width, int height) {
		BufferedImage image = generateImage(canvasToRender, width, height);
		saveToFileChooserSelection(image, dialogTitle, fileName);
	}

	/**
	 * Generate an image of the canvas and save it into a file chosen by the user in a 
	 * file chooser dialog box.
	 */
	public static void saveToFileChooserSelection(final BufferedImage image, final String dialogTitle, final String fileName){
		
		final SwingWorker worker = new SwingWorker() {
			public Object doInBackground() {
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
					fc.setSelectedFile(new File(fileName));
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
				return null;
			}
			public void done() {
			}
		};
		worker.execute();
	}
}

