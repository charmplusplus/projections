package projections.gui;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

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
class ScreenInfo
{
    protected static int screenHeight;
    protected static int screenWidth;

    private static GraphicsEnvironment ge;
    private static GraphicsDevice gd;
    private static GraphicsConfiguration gc;
    private static Rectangle bounds;

    protected static void init() {
	ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	gd = ge.getDefaultScreenDevice();
	gc = gd.getDefaultConfiguration();
	bounds = gc.getBounds();

	screenWidth = bounds.width;
	screenHeight = bounds.height;
    }
}
