package projections.gui;

import javax.swing.*;
import java.awt.*;

/* *******************************************
 * MainWindowPanel.java
 * Chee Wai Lee - 10/29/2002
 *
 * Created only for the purpose of painting
 * the traditional projections wallpaper in
 * a JFrame's content pane.
 * *******************************************/

public class MainWindowPanel extends JPanel {
    private Image bgimage = null;

    public MainWindowPanel(Image bgimage) {
	this.bgimage = bgimage;
    }

    public void paintComponent(Graphics g)
    {
	super.paintComponent(g);
        if (bgimage!=null) 
	    Util.wallPaper(this, g, bgimage);
    }
}

