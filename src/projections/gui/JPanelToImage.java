package projections.gui;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.freehep.graphicsio.AbstractVectorGraphicsIO;
import org.freehep.graphicsio.pdf.PDFGraphics2D;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.jfree.chart.JFreeChart;

/**
 * Renders & Saves images for any displayed JPanel. The JPanel must already be laid out.
 *
 * The save dialog offers PDF (the default) and SVG as well as the raster formats
 * PNG and JPG. Panels that draw themselves are written out as real vector
 * graphics; anything handed over as an already rendered BufferedImage is embedded
 * as a bitmap instead.
 *
 * The FreeHEP writers are driven directly rather than through FreeHEP's own
 * ExportDialog. That dialog registers its file types in a javax.imageio
 * ServiceRegistry, which Java 9 and later refuse to build for non-ImageIO
 * categories, so on a current JVM it throws before it can be shown.
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

	/** Let the user save the panel into a file whose format they choose in the export dialog. */
	public static void saveToFileChooserSelection(final Container panelToRender, final String dialogTitle, final String defaultFilename){
		onEventThread(new Runnable(){
			public void run(){
				showExportDialog(panelToRender, dialogTitle, defaultFilename);
			}
		});
	}

	/** Let the user save an already rendered image into a file whose format they choose in the
	 *  export dialog. The image is drawn by a panel so that it goes through the same dialog as
	 *  everything else, which means it can be written into a PDF as well as into a PNG or JPG. */
	public static void saveToFileChooserSelection(final BufferedImage image, final String dialogTitle, final String defaultFilename){
		onEventThread(new Runnable(){
			public void run(){
				showExportDialog(new ImagePanel(image), dialogTitle, defaultFilename);
			}
		});
	}

	/** Let the user save a chart into a file whose format they choose in the export dialog.
	 *  The chart is drawn straight into the output rather than by way of its ChartPanel,
	 *  because ChartPanel paints itself through an offscreen buffer, which would leave a
	 *  PDF holding nothing but an embedded bitmap. */
	public static void saveToFileChooserSelection(final JFreeChart chart, final int width, final int height, final String dialogTitle, final String defaultFilename){
		onEventThread(new Runnable(){
			public void run(){
				showExportDialog(new ChartPanel(chart, width, height), dialogTitle, defaultFilename);
			}
		});
	}

	/** The formats offered by the save dialog, in the order they are listed. */
	private static final String[][] FORMATS = {
		{ "pdf", "PDF document" },
		{ "svg", "SVG image" },
		{ "png", "PNG image" },
		{ "jpg", "JPEG image" },
	};

	/** Ask the user for a file and a format, and write the panel into it. */
	private static void showExportDialog(Container panelToRender, String dialogTitle, String defaultFilename){
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(dialogTitle);
		// Every file has to be written by one of the format writers, so there is no "all files"
		fc.setAcceptAllFileFilterUsed(false);

		String defaultExtension = extensionOf(new File(defaultFilename));
		for(String[] format : FORMATS){
			FormatFilter filter = new FormatFilter(format[0], format[1]);
			fc.addChoosableFileFilter(filter);
			if(format[0].equals(defaultExtension))
				fc.setFileFilter(filter);
		}
		fc.setSelectedFile(new File(defaultFilename));

		if(fc.showSaveDialog(panelToRender) != JFileChooser.APPROVE_OPTION)
			return;

		File file = fc.getSelectedFile();
		String extension = extensionOf(file);
		// If the typed name has no extension we recognise, go with the format picked in the list
		if(!isKnownFormat(extension)){
			extension = ((FormatFilter) fc.getFileFilter()).extension;
			file = new File(file.getPath() + "." + extension);
		}

		if(file.exists()){
			int overwrite = JOptionPane.showConfirmDialog(panelToRender,
					file.getName() + " already exists. Overwrite it?",
					dialogTitle, JOptionPane.YES_NO_OPTION);
			if(overwrite != JOptionPane.YES_OPTION)
				return;
		}

		try {
			write(panelToRender, file, extension);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(panelToRender,
					"Could not save " + file + ":\n" + e.getLocalizedMessage(),
					"Error saving file", JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Write the panel into the file in the given format. */
	private static void write(Container panelToRender, File file, String extension) throws IOException {
		Dimension size = panelToRender.getSize();
		// A panel that was never added to a window may not have been laid out yet
		if(size.width <= 0 || size.height <= 0)
			size = panelToRender.getPreferredSize();

		if(extension.equals("png") || extension.equals("jpg")){
			BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			// JPG cannot store transparency, and a panel need not paint every pixel
			g.setColor(Color.white);
			g.fillRect(0, 0, size.width, size.height);
			panelToRender.print(g);
			g.dispose();
			ImageIO.write(image, extension, file);
			return;
		}

		AbstractVectorGraphicsIO g;
		if(extension.equals("svg")){
			g = new SVGGraphics2D(file, size);
		} else {
			g = new PDFGraphics2D(file, size);
			// Make the page exactly as big as the drawing, so that the result has no margins
			Properties properties = new Properties();
			properties.setProperty(PDFGraphics2D.PAGE_SIZE, PDFGraphics2D.CUSTOM_PAGE_SIZE);
			properties.setProperty(PDFGraphics2D.CUSTOM_PAGE_SIZE, size.width + ", " + size.height);
			properties.setProperty(PDFGraphics2D.PAGE_MARGINS, "0, 0, 0, 0");
			g.setProperties(properties);
		}
		g.startExport();
		// print() rather than paint(), so that Swing's double buffering is switched off and the
		// panel draws straight into the vector output instead of into an offscreen bitmap
		panelToRender.print(g);
		g.endExport();
	}

	private static boolean isKnownFormat(String extension){
		for(String[] format : FORMATS)
			if(format[0].equals(extension))
				return true;
		return false;
	}

	/** Get the lower case extension of a file, with jpeg treated as jpg. */
	private static String extensionOf(File f){
		String name = f.getName();
		int dot = name.lastIndexOf('.');
		if(dot <= 0 || dot == name.length()-1)
			return "";
		String extension = name.substring(dot+1).toLowerCase();
		return extension.equals("jpeg") ? "jpg" : extension;
	}

	/** Lets the user pick one of the output formats in the file chooser. */
	private static class FormatFilter extends FileFilter {
		final String extension;
		private final String description;

		FormatFilter(String extension, String description){
			this.extension = extension;
			this.description = description;
		}

		public boolean accept(File f){
			return f.isDirectory() || extensionOf(f).equals(extension);
		}

		public String getDescription(){
			return description + " (*." + extension + ")";
		}
	}

	/** The export dialog is modal, so it has to be put up on the event dispatch thread. Some of
	 *  the tools ask for a save from a worker thread while they are still loading their data. */
	private static void onEventThread(Runnable r){
		if(SwingUtilities.isEventDispatchThread())
			r.run();
		else
			SwingUtilities.invokeLater(r);
	}

	/** Draws an already rendered image at its natural size. */
	private static class ImagePanel extends JPanel {
		private final BufferedImage image;

		ImagePanel(BufferedImage image){
			this.image = image;
			Dimension size = new Dimension(image.getWidth(), image.getHeight());
			setPreferredSize(size);
			// The panel is never added to a window, so it has to be sized by hand
			// for the export dialog to know how big the drawing is.
			setSize(size);
		}

		public void paintComponent(Graphics g){
			g.drawImage(image, 0, 0, null);
		}
	}

	/** Draws a chart at a given size, without the buffering that org.jfree.chart.ChartPanel does. */
	private static class ChartPanel extends JPanel {
		private final JFreeChart chart;

		ChartPanel(JFreeChart chart, int width, int height){
			this.chart = chart;
			Dimension size = new Dimension(width, height);
			setPreferredSize(size);
			// The panel is never added to a window, so it has to be sized by hand
			// for the export dialog to know how big the drawing is.
			setSize(size);
		}

		public void paintComponent(Graphics g){
			chart.draw((Graphics2D) g, new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
		}
	}


}
