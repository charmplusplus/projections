package projections.Tools.TimeProfile;



import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import projections.gui.JPanelToImage;
import projections.gui.MainWindow;

/** Display a legend in a new window (clickable to save image to file) */
class Legend implements MouseListener {
	private BufferedImage image;
	private Paint fgColor;
	private Paint bgColor;
	
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;
	
	private List<String> names;
	
	private Font namesFont;
	private Font legendFont;
	
	Legend(String title, List<String> names, List<Paint> paints){
		this.names = names;
		
		namesFont = new Font("SansSerif", Font.PLAIN, fontSizeNames() ); 
		legendFont = new Font("SansSerif", Font.BOLD, fontSizeLegend() ); 
		
		// Create an image
		image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();

		bgColor = MainWindow.runObject[myRun].background;
		fgColor = MainWindow.runObject[myRun].foreground;
				
		// Clear Background
		g.setPaint(bgColor);
		g.fillRect(0,0,getWidth(), getHeight());
		g.setPaint(fgColor);
		g.drawRect(0,0,getWidth()-1, getHeight()-1);
	
	
		// draw "Legend:"
		g.setPaint(fgColor);
		g.setFont(legendFont);
		FontMetrics fm = g.getFontMetrics();
		int fw = fm.stringWidth(title);
		g.drawString(title, (getWidth()-fw)/2, baselineLegend() );

		
		// Draw in the names of the entry methods
		g.setFont(namesFont);

		for(int i=0; i<names.size(); i++){
			String name = names.get(i);
			Paint paint = paints.get(i);
			
			int topPixel = topMargin() + i*lineSpacingNames();
			int textBaseline = topPixel + lineSpacingNames()/2 + fontSizeNames()/2;
			
			// Draw colored box
			g.setPaint(paint);
			g.fillRect(getWidth()-rightMargin()-boxMarginR()-boxWidth(),topPixel+boxMarginsTB(),boxWidth(), lineSpacingNames()-boxMarginsTB()*2);

			// Draw entry method name
			g.setPaint(fgColor);
			g.drawString(name, leftMargin(), textBaseline );
		
		}
		
		
		
		
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

	private int fontSizeLegend(){
		return 30;
	}

	private int baselineLegend(){
		return (topMargin()+fontSizeLegend())/2;
	}
	
	/// Space reserved at top for the title
	private int topMargin(){
		return (int) (1.5 * fontSizeLegend());
	}
	
	private int fontSizeNames(){
		return 14;
	}

	private int leftMargin(){
		return 5;
	}

	private int rightMargin(){
		return 5;
	}

	/// Top and bottom margins for the box
	private int boxMarginsTB(){
		return 3;
	}

	/// Left and right margins for the box
	private int boxMarginL(){
		return 15;
	}

	private int boxMarginR(){
		return 5;
	}

	
	private int boxWidth(){
		return 25;
	}

	
	/// Space allocated vertically for each name&box
	private int lineSpacingNames(){
		return fontSizeNames() + 10;
	}
	
	
	private int getWidth(){
		// Make a fake image to determine font sizing
		BufferedImage img = new BufferedImage(100,100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setFont(namesFont);
		FontMetrics fm = g.getFontMetrics();
		
		int maxw = 0;
		for(int i=0; i<names.size(); i++){
			int w = fm.stringWidth(names.get(i));
			if(w > maxw)
				maxw = w;
		}
		return leftMargin() + maxw + boxMarginL() + boxWidth() + boxMarginR() + rightMargin();
	}
	
	private int getHeight(){
		return topMargin() + lineSpacingNames() * names.size();
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
