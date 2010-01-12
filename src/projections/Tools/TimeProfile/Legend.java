package projections.Tools.TimeProfile;



import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import projections.gui.JPanelToImage;
import projections.gui.MainWindow;

/** Display a legend for the memory usage colors in a new window (clickable to save image to file) */
class Legend implements MouseListener {
	private BufferedImage image;
	Paint fgColor;
	Paint bgColor;
	
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;
	
	
	Legend(){
	
		// Create an image
		image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		FontMetrics fm = g.getFontMetrics();

		bgColor = MainWindow.runObject[myRun].background;
		fgColor = MainWindow.runObject[myRun].foreground;
		
		
		// Clear Background
		g.setPaint(bgColor);
		g.fillRect(0,0,getWidth(), getHeight());
		
		// draw "Legend:"
		String s = "Legend:";
		Font legendFont = new Font("SansSerif", Font.PLAIN, 30); 
		g.setPaint(fgColor);
		g.setFont(legendFont);
		int fw = fm.stringWidth(s);
		g.drawString(s, (getWidth()-fw)/2, 10);

		
		
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
	

	
	private int getWidth(){
		return 600;
	}
	
	private int getHeight(){
		return 500;
	}


	public void mouseClicked(MouseEvent e) {
		JPanelToImage.saveToFileChooserSelection(image, "Save Legend To PNG or JPG", "TimeProfileLegend.png");
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
