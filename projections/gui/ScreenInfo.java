package projections.gui;

import java.awt.*;

/**
 *  Written by Chee Wai Lee
 *  04/19/2002
 *
 *  This is a simple static class that captures screen information about
 *  the system.
 *
 *  Helpful for making the projections gui "friendly" towards different
 *  systems.
 *  
 */
public class ScreenInfo
{
    public static int screenHeight;
    public static int screenWidth;

    private GraphicsEnvironment ge;
    private GraphicsDevice gd;
    private GraphicsConfiguration gc;
    private Rectangle bounds;

    public ScreenInfo() {
	ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	gd = ge.getDefaultScreenDevice();
	gc = gd.getDefaultConfiguration();
	bounds = gc.getBounds();

	screenWidth = bounds.width;
	screenHeight = bounds.height;
    }
}
