package projections.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

/** A small little JPanel that is painted a specified color */
public class ColorPanel extends JPanel {

	private int w, h;
	private Color background;
	private Color foreground;
	private int wDefault = 40;
	private int hDefault = 20;
	private boolean highlighted = false;

	public ColorPanel(Color f)
	{
		background = Color.black;
		foreground = f;
		w = wDefault;
		h = hDefault;
	}


	public Color getColor()
	{
		return foreground;
	}   
	public Dimension getMinimumSize()
	{
		return new Dimension(w, h);
	}   
	
	public Dimension getPreferredSize()
	{
		return new Dimension(w, h);
	}   
	

	public void mouseExited(MouseEvent evt)
	{
		if(highlighted)
		{
			highlighted = false;
			repaint();
		}   
	}   

	public void paintComponent(Graphics g) {
		g.setColor(background);
		g.fillRect(0, 0, w, h);

		g.setColor(Color.gray);
		g.drawLine(0, 0, w, 0);
		g.drawLine(0, 1, w, 1);
		g.drawLine(0, 0, 0, h);
		g.drawLine(1, 0, 1, h);

		g.setColor(Color.white);
		g.drawLine(0, h, w, h);
		g.drawLine(0, h-1, w, h-1);
		g.drawLine(w, h, w, 0);
		g.drawLine(w-1, h, w-1, 0);

		g.setColor(foreground);
		g.fillRect(5, 5, w-10, h-10);

	}   

	public void setColor(Color c)
	{
		foreground = c;
		repaint();
	}

}
