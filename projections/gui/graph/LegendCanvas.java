package projections.gui.graph;

import projections.gui.*;

import java.awt.*;
import java.awt.event.*;

public class LegendCanvas extends Canvas
    implements MouseListener
{
    private String labels[] = null;
    private Color colorMap[] = null;

    private boolean selection[];

    private FontMetrics fm;
    private int textheight;
    private int maxTextWidth;
    private Rectangle boundingBox;
    private ScrollPane sp;
    private int posx;
    private int posy;

    /**
     *  pre-condition: Nlabels.length == NcolorMap.length
     */
    public LegendCanvas() {
	setSize(getPreferredSize());

	setBackground(Color.black);
	setForeground(Color.white);

	addMouseListener(this);
    }

    public void paint(Graphics g)
    {
	if (labels == null) {
	    return;
	}
	fm = g.getFontMetrics(g.getFont());
	textheight = fm.getHeight();
	maxTextWidth = 10;

	posx = 10;
	posy = 10;

	for (int i=0; i<labels.length; i++) {
	    if (selection[i]) {
		paintSelected(g, labels[i], colorMap[i]);
	    } else {
		paintLabel(g, labels[i], colorMap[i]);
	    }
	}
	setSize(new Dimension(maxTextWidth, posy));
    }

    public void setColors(Color NcolorMap[]) {
	colorMap = NcolorMap;
	repaint();
    }

    public void setData(String Nlabels[], Color NcolorMap[]) {
	labels = Nlabels;
	selection = new boolean[labels.length];
	colorMap = NcolorMap;
	repaint();
    }

    public boolean[] getSelection() {
	// must construct a new copy because this one is being continually
	// used.
	boolean returnSelection[];

	returnSelection = new boolean[selection.length];
	for (int i=0; i<selection.length; i++) {
	    returnSelection[i] = selection[i];
	}
	return returnSelection;
    }

    public Dimension getMinimumSize() {
	return new Dimension(100, 200);
    }

    public Dimension getPreferredSize() {
	return new Dimension(200, 400);
    }

    private void paintLabel(Graphics g, String label, Color color) {
	Color originalColor;
	originalColor = g.getColor();

	// draw rectangle followed by string
	g.setColor(color);
	g.fillRect(posx, posy, textheight*2, textheight);
	g.drawString(label, posx + textheight*3, posy + textheight);
	if (posx + textheight*3 + fm.stringWidth(label) > maxTextWidth) {
	    maxTextWidth = posx + textheight*3 + fm.stringWidth(label);
	}
	g.setColor(getBackground());
	g.drawRect(posx+1, posy+1, (textheight-1)*2-1, textheight-2-1);
	posy += textheight*3/2;
	g.setColor(originalColor);
    }

    private void paintSelected(Graphics g, String label, Color color) {
	Color originalColor;
	originalColor = g.getColor();

	// draw inversed checked rectangle followed by string using a BRIGHTER
	// version of color
	g.setColor(color.brighter());
	g.drawRect(posx, posy, textheight*2, textheight);
	g.drawLine(posx, posy, posx+textheight*2, posy+textheight);
	g.drawLine(posx+textheight*2, posy, posx, posy+textheight);
	g.drawString(label, posx + textheight*3, posy + textheight);
	if (posx + textheight*3 + fm.stringWidth(label) > maxTextWidth) {
	    maxTextWidth = posx + textheight*3 + fm.stringWidth(label);
	}
	posy += textheight*3/2;
	g.setColor(originalColor);
    }

    private int yPixelToLineNum(int yPixel) {
	int offset = 10;
	int lineBlock;

	lineBlock = (yPixel - offset) / (textheight*3/2);
	if ((yPixel - offset) > (lineBlock*(textheight*3/2) + textheight)) {
	    // outside of the actual rectangle
	    return -1;
	} else {
	    return lineBlock;
	}
    }

    // listener functions
    public void mouseClicked(MouseEvent evt) {
	int lineNumber = yPixelToLineNum(evt.getY());

	if (lineNumber != -1) {
	    // toggle the selection
	    if (selection[lineNumber]) {
		selection[lineNumber] = false;
	    } else {
		selection[lineNumber] = true;
	    }
	    repaint();
	}
    }

    public void mouseEntered(MouseEvent evt) {
    }

    public void mouseExited(MouseEvent evt) {
    }

    public void mousePressed(MouseEvent evt) {
    }

    public void mouseReleased(MouseEvent evt) {
    }

}

