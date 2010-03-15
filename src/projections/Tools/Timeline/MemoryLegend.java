package projections.Tools.Timeline;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import projections.gui.JPanelToImage;

/** Display a legend for the memory usage colors in a new window (clickable to save image to file) */
class MemoryLegend implements MouseListener {
	private BufferedImage image;
	
	MemoryLegend(Data data){
	
		// Create an image
		image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		FontMetrics fm = g.getFontMetrics();

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
		JPanelToImage.saveToFileChooserSelection(image, "Save Legend To PNG or JPG", "TimelineLegend.png");
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
