package projections.Tools.Timeline;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;


/** A class that renders a JPanel that is composed of images rendered from 4 other panels in a 2x2 grid */
class Render2by2PanelGrid extends JPanel {
	private BufferedImage NW;
	private BufferedImage NE;
	private BufferedImage SW;
	private BufferedImage SE;

	private int widthWest;
	private int widthEast;
	private int heightNorth;
	private int heightSouth;

	private BufferedImage generateImage(JPanel panelToRender){
		if(panelToRender != null){
			//		 Create an image for the constructed panel.
			int width = panelToRender.getWidth();
			int height = panelToRender.getHeight();

			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			panelToRender.paint(g);
			g.dispose();
			return image;
		} else {
			return null;
		}
	}

	protected Render2by2PanelGrid(JPanel NWpanel, JPanel NEpanel, JPanel SWpanel, JPanel SEpanel){
		NW = generateImage(NWpanel);
		NE = generateImage(NEpanel);
		SW = generateImage(SWpanel);
		SE = generateImage(SEpanel);
		widthWest = SW.getWidth();
		widthEast = SE.getWidth();
		heightNorth = NE.getHeight();
		heightSouth = SE.getHeight();
	}

	public int getWidth(){
		return widthWest + widthEast;
	}

	public int getHeight(){
		return heightNorth + heightSouth;
	}


	/** Paint the saved image representations of all 4 panels */
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		g.drawImage(NW, 0, 0, null);
		g.drawImage(NE, widthWest, 0, null);
		g.drawImage(SW, 0, heightNorth, null);
		g.drawImage(SE, widthWest, heightNorth, null);

	}
}
