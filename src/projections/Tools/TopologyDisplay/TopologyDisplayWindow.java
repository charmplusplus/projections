package projections.Tools.TopologyDisplay;

import projections.gui.MainWindow;
import projections.gui.ProjectionsWindow;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.awt.BorderLayout;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;

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
public class TopologyDisplayWindow extends ProjectionsWindow 
		implements ItemListener {
 	
	static final float pointRadius = 0.13f;
	static final float coneRadius = 0.12f;
	static final float coneHeight = 0.3f;

	private Appearance redAppearance;
	private Appearance greenAppearance;
	private Appearance blueAppearance;

	private SimpleUniverse universe;
	private BranchGroup scene;
	private TransformGroup objRotate;
	private BranchGroup boxGroup;

	private int maxX, minX;
	private int maxY, minY;
	private int maxZ, minZ;
	static final float axisExt = 1.5f;

	/************* Initialization *************/

	public void initUI() {
		JMenuBar menuBar = new JMenuBar();

		// set file menu
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		JMenuItem inputFileItem = new JMenuItem("Input File");
		inputFileItem.addActionListener(this);
		fileMenu.add(inputFileItem);

		JMenuItem quitItem = new JMenuItem("Quit");
		quitItem.addActionListener(this);
		fileMenu.add(quitItem);

		// set options menu
		JMenu optionsMenu = new JMenu("Options");
		menuBar.add(optionsMenu);

		JCheckBoxMenuItem showBoxItem = new JCheckBoxMenuItem("Show Box");
		showBoxItem.setSelected(true);
		showBoxItem.addItemListener(this);
		optionsMenu.add(showBoxItem);

		JCheckBoxMenuItem showCoordItem = new JCheckBoxMenuItem("Show Coordinates");
		showCoordItem.addItemListener(this);
		optionsMenu.add(showCoordItem);

		// set screenshot menu
		JMenu screenshotMenu = new JMenu("Screenshot");
		menuBar.add(screenshotMenu);

		JMenuItem screenshotItem = new JMenuItem("Take Screenshot");
		screenshotItem.addActionListener(this);
		screenshotMenu.add(screenshotItem);

		this.setJMenuBar(menuBar);
	}

	public void initCommonAppearance() {
		// red appearance
		redAppearance = new Appearance();
		Color3f redColor = new Color3f(1.0f, 0.0f, 0.0f);
		ColoringAttributes redCA = new ColoringAttributes(redColor, 1);
		redAppearance.setColoringAttributes(redCA);

		// green appearance
		greenAppearance = new Appearance();
		Color3f greenColor = new Color3f(0.0f, 1.0f, 0.0f);
		ColoringAttributes greenCA = new ColoringAttributes(greenColor, 1);
		greenAppearance.setColoringAttributes(greenCA);

		// blue appearance
		blueAppearance = new Appearance();
		Color3f blueColor = new Color3f(0.0f, 0.0f, 1.0f);
		ColoringAttributes blueCA = new ColoringAttributes(blueColor, 1);
		blueAppearance.setColoringAttributes(blueCA);
	}

	public TopologyDisplayWindow(MainWindow parentWindow) {
		super(parentWindow);

		setTitle("Projections 3D Topology");
		setSize(500, 500);

		maxX = 0;
		minX = 0;
		maxY = 0;
		minY = 0;
		maxZ = 0;
		minZ = 0;

		this.initCommonAppearance();
		this.initUI();

		setLayout(new BorderLayout());
		
		Canvas3D canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
		add("Center", canvas);

		// initialize the universe and scene etc...
		this.universe = new SimpleUniverse(canvas);
		this.scene = new BranchGroup();
		this.objRotate = new TransformGroup();
		this.boxGroup = new BranchGroup();

		BoundingSphere backgroundBounds = new BoundingSphere(
				  new Point3d(0, 0, 0), 100.0);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_READ);

		this.createSceneGraph(backgroundBounds);	// add points
		this.createAxes();								// add axes
		this.initBoxGroup();								// init box

		// init more stuffs
		addMouseRotator(scene, objRotate, backgroundBounds);
		addMouseTranslation(scene, objRotate, backgroundBounds);
		addMouseZoom(scene, objRotate, backgroundBounds);

		setViewPlatform();
		
		// add everything together
		this.objRotate.addChild(boxGroup);
		this.scene.addChild(objRotate);
		this.scene.compile();
		this.universe.addBranchGraph(scene);

		showDialog();
	}

	private void setViewPlatform() {
		// set the initial location of the view/camera.
		ViewingPlatform vp = universe.getViewingPlatform();
		TransformGroup viewTransformGroup = vp.getMultiTransformGroup().getTransformGroup(0);

		Transform3D viewTransform3D = new Transform3D();
		viewTransformGroup.getTransform(viewTransform3D);

		viewTransform3D.setTranslation(new Vector3f(0.0f, 0.0f, 20.0f));
		viewTransformGroup.setTransform(viewTransform3D);
	}

	protected void showDialog() {
		this.setVisible(true);
	}

	/************* Scene Creation *************/

	private void createSceneGraph(BoundingSphere backgroundBounds) {
	
	}

	private void createAxes() {
	}

	/************* Optional Scene Creation *************/

	private void initBoxGroup() {
	}
    
	/************* Controls *************/

	private void addMouseRotator(BranchGroup scene, TransformGroup objGroup, BoundingSphere bounds) {
		// Rotation.
		MouseRotate myMouseRotate = new MouseRotate();
		myMouseRotate.setTransformGroup(objGroup);
		myMouseRotate.setSchedulingBounds(bounds);
		scene.addChild(myMouseRotate);
	}
	
	private void addMouseTranslation(BranchGroup scene, TransformGroup objGroup, BoundingSphere bounds) {
		// Translation.
		MouseTranslate translateBehavior = new MouseTranslate();
		translateBehavior.setTransformGroup(objGroup);
		translateBehavior.setSchedulingBounds(bounds);
		scene.addChild(translateBehavior);
	}
	
	private void addMouseZoom(BranchGroup scene, TransformGroup objGroup, BoundingSphere bounds) {
		// Zoom in/out.
		MouseZoom zoomBehavior = new MouseZoom();
		zoomBehavior.setTransformGroup(objGroup);
		zoomBehavior.setSchedulingBounds(bounds);
		scene.addChild(zoomBehavior);
	}

	/************* Implemented Listeners *************/

	@Override
	public void itemStateChanged(ItemEvent e) {
		JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();

		if (source.getText().equals("Show Box")) {
		
		} else if (source.getText().equals("Show Coordinates")) {
		
		}
	}
}

