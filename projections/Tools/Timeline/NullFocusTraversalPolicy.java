package projections.Tools.Timeline;

import java.awt.Component;
import java.awt.Container;


/** A class that implements how the main panel should be focused. This is mostly an optimization to 
 * stop the JVM from taking forever to layout all the objects and associate spatial data structures for
 *  traversing the objects with keyboard input. 
 *  
 *  @author idooley2
 *  */

public class NullFocusTraversalPolicy extends java.awt.FocusTraversalPolicy{

	public Component getComponentAfter(Container container, Component component) {
		return null;
	}

	public Component getComponentBefore(Container container, Component component) {
		return null;
	}

	public Component getDefaultComponent(Container container) {
		return null;
	}

	public Component getFirstComponent(Container container) {
		return null;
	}

	public Component getLastComponent(Container container) {
		return null;
	}
	
	
}
