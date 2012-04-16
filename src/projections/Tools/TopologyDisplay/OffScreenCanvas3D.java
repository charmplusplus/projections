package projections.Tools.TopologyDisplay;

import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;

public class OffScreenCanvas3D extends Canvas3D {
	public OffScreenCanvas3D(GraphicsConfiguration graphicsConfiguration,
			  boolean offScreen) {
		super(graphicsConfiguration, offScreen);
	}

	/**
	 * Create an image and paint the Canvas3D into the image.
	 */
	public BufferedImage generateImage(int width, int height) {
		
		// Create an image for the constructed canvas.
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		ImageComponent2D ic = new ImageComponent2D(ImageComponent.FORMAT_RGB, image);
	
		setOffScreenBuffer(ic);
		renderOffScreenBuffer();
		waitForOffScreenRendering();
		image = getOffScreenBuffer().getImage();
		
		return image;
	}

	public void postSwap() {
		// Do nothing.
	}
}

