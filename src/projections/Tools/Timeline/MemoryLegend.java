package projections.Tools.Timeline;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import projections.gui.JPanelToImage;

/** Display a legend for the memory usage colors in a new window (clickable to save image to file) */
class MemoryLegend implements MouseListener {
	private BufferedImage image;
	private final Data data;

	MemoryLegend(Data data){
		this.data = data;

		// Create an image
		image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		draw(g);

		// Display the thing
		ImageIcon imageIcon = new ImageIcon(image);
		JFrame f = new JFrame();
		JLabel l = new JLabel(imageIcon);
		l.addMouseListener(this);
		f.getContentPane().add(l);
		f.pack();
		f.setVisible(true);

		g.dispose();

	}

	/** Paint the legend. Kept separate from the displayed image so that the same drawing can
	 *  also go straight into a PDF, which keeps it sharp at any zoom rather than a bitmap. */
	private void draw(Graphics2D g){
		// Draw the legend bar
		g.setColor(data.getBackgroundColor());
		g.fillRect(0,0,getWidth(), getHeight());

		for(int i=barLeftMargin(); i<getWidth()-barRightMargin(); i++){
			float normalizedValue = (float)(i-barLeftMargin())/(float)(getWidth()-barRightMargin()-barLeftMargin());
			Color c = Color.getHSBColor(0.6f-normalizedValue*0.65f, 1.0f, 1.0f);
			g.setColor(c);
			g.fillRect(i, barTopMargin(), 1, getHeight()-barTopMargin()-barBottomMargin());
		}


		// draw the text
		g.setColor(data.getForegroundColor());
		Font numberFont = new Font("SansSerif", Font.PLAIN, 15);
		g.setFont(numberFont);
		// Measure with the font actually used for the labels, so that they are centred the
		// same way on screen and in an exported PDF
		FontMetrics fm = g.getFontMetrics();
		int numLabels = 4;
		int pxBetweenLabels = (getWidth()-barRightMargin()-barLeftMargin()) / (numLabels-1);
		float memUsageBetweenLabels =  (float)((data.maxMemBColorRange() - data.minMemBColorRange()) / (numLabels-1));
		for(int j=0; j<numLabels; j++){
			float memUsageB =(data.minMemBColorRange() + j * memUsageBetweenLabels);
			int memUsageMB = (int) (memUsageB / 1024 / 1024);
			String s = "" + memUsageMB + " MB";
			int x = barLeftMargin() + j*pxBetweenLabels;
			int xtext = x - fm.stringWidth(s)/2;
			g.drawString(s, xtext, bottomLabelBaseline());
			g.fillRect(x-1,getHeight()-barBottomMargin(),3,7);
		}

		// draw the "Legend:"
		Font legendFont = new Font("SansSerif", Font.PLAIN, 30);
		g.setColor(data.getForegroundColor());
		g.setFont(legendFont);
		g.drawString("Legend:", 10, getHeight()/2+legendFont.getSize()/2);
	}

	/** A panel that redraws the legend, used to export it as a PDF or an image. */
	private class MemoryLegendPanel extends JPanel {
		MemoryLegendPanel(){
			Dimension size = new Dimension(MemoryLegend.this.getWidth(), MemoryLegend.this.getHeight());
			setPreferredSize(size);
			// Never added to a window, so it has to be sized by hand before being exported
			setSize(size);
		}

		public void paintComponent(Graphics g){
			draw((Graphics2D) g);
		}
	}

	private int barTopMargin(){
		return 20;
	}

	private int barBottomMargin(){
		return 40;
	}

	private int barLeftMargin(){
		return 175;
	}

	private int barRightMargin(){
		return 75;
	}

	
	private int bottomLabelBaseline(){
		return getHeight()-10;
	}
	
	private int getWidth(){
		return 600;
	}
	
	private int getHeight(){
		return 100;
	}


	public void mouseClicked(MouseEvent e) {
		JPanelToImage.saveToFileChooserSelection(new MemoryLegendPanel(), "Save Legend To PDF or Image", "TimelineLegend.pdf");
	}


	public void mouseEntered(MouseEvent e) {
	}


	public void mouseExited(MouseEvent e) {
	}


	public void mousePressed(MouseEvent e) {
	}


	public void mouseReleased(MouseEvent e) {
	}
	
}
