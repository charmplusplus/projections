package projections.Tools.TopologyDisplay;

import projections.gui.MainWindow;
import projections.gui.ProjectionsWindow;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.TransformGroup;

import com.sun.j3d.utils.universe.SimpleUniverse;

/**
 * The main window for the TopologyDisplay Projections Tool
 * 
 * This interactive window allows the user to see 3D topology using Java3D.
 *
 * Controls:
 * Rotate: mouse left click and dragging
 * Translate: mouse right click and dragging
 * Zoom in/out: mouse middle click and dragging
 * 
 * @author wang103
 */
public class TopologyDisplayWindow extends ProjectionsWindow {
 	
	static final float pointRadius = 0.13f;
	static final float coneRadius = 0.12f;
	static final float coneHeight = 0.3f;

	private Appearance redAppearance;
	private Appearance greenAppearance;
	private Appearance blueAppearance;
	private Appearance yellowAppearance;

	private SimpleUniverse univserse;
	private BranchGroup scene;
	private TransformGroup objRotate;
	private BranchGroup boxGroup;

	private int maxX, minX;
	private int maxY, minY;
	private int maxZ, minZ;
	static final float axisExt = 1.5f;

	/************* Initialization *************/

	public TopologyDisplayWindow(MainWindow parentWindow) {
		super(parentWindow);

		maxX = 0;
		minX = 0;
		maxY = 0;
		minY = 0;
		maxZ = 0;
		minZ = 0;
		
		showDialog();
	}

	protected void showDialog() {
	
	}


	/************* Scene Creation *************/


	/************* Optional Scene Creation *************/

    
	/************* Controls *************/


	/************* Implemented Listeners *************/


}

