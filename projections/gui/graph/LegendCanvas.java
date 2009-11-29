package projections.gui.graph;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import projections.gui.Util;

public class LegendCanvas extends Canvas
    implements MouseListener, ActionListener
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String labels[] = null;
    private Color colorMap[] = null;

    private int sortMap[] = null;
    private int reverseMap[] = null;
    private boolean selection[];
    private boolean showFilter[] = null;

    private FontMetrics fm;
    private int textheight;
    private int maxTextWidth;
//    private Rectangle boundingBox;
//    private ScrollPane sp;
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

	int linecount = 0;
	for (int i=0; i<labels.length; i++) {
	    if (showFilter[sortMap[i]] == true) {
		reverseMap[linecount] = sortMap[i];
		linecount++;
		if (selection[sortMap[i]]) {
		    paintSelected(g, labels[sortMap[i]], 
				  colorMap[sortMap[i]]);
		} else {
		    paintLabel(g, labels[sortMap[i]], 
			       colorMap[sortMap[i]]);
		}
	    }
	}
	setSize(new Dimension(maxTextWidth, posy));
    }

    public void setColors(Color NcolorMap[]) {
	colorMap = NcolorMap;
	repaint();
    }

    /**
     *  This method must be called on an existing display. Otherwise,
     *  bad things happen.
     */
    public void setFilter(boolean NshowFilter[]) {
	showFilter = NshowFilter;
	if (showFilter == null) {
	    defaultShowFilter(labels.length);
	}
	// reverseMap is automatically adjusted at repaint time, so no
	// problems.
	repaint();
    }

    /**
     *  This method must be called on an existing display. Otherwise,
     *  bad things happen.
     */
    public void setSort(int NsortMap[]) {
	sortMap = NsortMap;
	if (sortMap == null) {
	    defaultSortMap(labels.length);
	}
	// reverseMap is automatically adjusted at repaint time, so no
	// problems.
	repaint();
    }

    public void setData(String Nlabels[], Color NcolorMap[],
			boolean NshowFilter[], int NsortMap[]) {
	labels = Nlabels;
	selection = new boolean[labels.length];
	reverseMap = new int[labels.length];
	colorMap = NcolorMap;
	if (NshowFilter == null) {
	    defaultShowFilter(labels.length);
	} else {
	    showFilter = NshowFilter;
	}
	if (NsortMap == null) {
	    defaultSortMap(labels.length);
	} else {
	    sortMap = NsortMap;
	}
	repaint();
    }

    public boolean[] getSelection() {
	// must construct a new copy because this one is being continually
	// used.
	//
	// The selection returned is the UNSORTED selection. The calling
	// method must use the same sortMap to acquire sorted data from
	// the analyzed data object.
	//
	// returns only visible selections to be displayed in graph.

	return Util.andFilters(selection, showFilter);
    }

    public int[] getSort() {
	return sortMap;
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

    /**
     *  For a new show filter
     */
    private void defaultShowFilter(int length) {
	showFilter = new boolean[length];
	for (int i=0; i<length; i++) {
	    showFilter[i] = true;
	}
    }

    /**
     *  For a new sort map
     */
    private void defaultSortMap(int length) {
	sortMap = new int[length];
	for (int i=0; i<length; i++) {
	    sortMap[i] = i;
	}
    }

    // listener functions
    public void actionPerformed(ActionEvent evt) {
	String actionCommand = evt.getActionCommand();

	if (actionCommand.equals("select")) {
	    for (int i=0; i<selection.length; i++) {
		selection[i] = true;
	    }
	    repaint();
	} else if (actionCommand.equals("unselect")) {
	    for (int i=0; i<selection.length; i++) {
		selection[i] = false;
	    }
	    repaint();
	}
    }

    public void mouseClicked(MouseEvent evt) {
	int lineNumber = yPixelToLineNum(evt.getY());

	if (lineNumber != -1) {
	    // toggle the selection
	    if (selection[reverseMap[lineNumber]]) {
		selection[reverseMap[lineNumber]] = false;
	    } else {
		selection[reverseMap[lineNumber]] = true;
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

