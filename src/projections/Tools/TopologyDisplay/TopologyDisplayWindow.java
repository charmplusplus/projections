package projections.Tools.TopologyDisplay;

import projections.gui.OffScreenCanvas3DToImage;
import projections.gui.MainWindow;
import projections.gui.ProjectionsWindow;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;

import java.io.File;

import java.lang.Math;

import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.LineArray;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Screen3D;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Sphere;
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
 * Rotation w/o mouse:
 * Up/Donw: rotate around X axis
 * Left/Right: rotate around Y axis
 * M/N: rotate around Z axis
 *
 * @author wang103
 */
public class TopologyDisplayWindow extends ProjectionsWindow 
		implements ItemListener, KeyListener {
 	
	static final float pointRadius = 0.13f;
	static final float coneRadius = 0.12f;
	static final float coneHeight = 0.3f;

	static final float rotationStepRadian = 0.0174532925f;	// 1 degree
	
	private Appearance redAppearance;
	private Appearance greenAppearance;
	private Appearance blueAppearance;

	private SimpleUniverse universe;
	private BranchGroup scene;
	private TransformGroup objTrans;
	private TransformGroup objRotate;
	private BranchGroup boxGroup;
	private BranchGroup coordinatesGroup;
	private BranchGroup wrapperGraph;
	private BoundingSphere backgroundBounds;

	private DirectionalLight light;

	private int maxX, minX;
	private int maxY, minY;
	private int maxZ, minZ;
	static final float axisExt = 1.0f;
	private Vector3f centerOfCube;

	static int screenshotCount = 0;

	private MainWindow parentWindow;
	private Canvas3D canvas;
	private OffScreenCanvas3D offScreenCanvas;

	private JMenuItem inputFileItem;
	private JMenuItem quitItem;
	private JCheckBoxMenuItem showBoxItem;
	private JCheckBoxMenuItem showCoordItem;
	private JMenuItem screenshotItem;

	/************* Initialization *************/

	public void initUI() {
		JMenuBar menuBar = new JMenuBar();

		// set file menu
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		inputFileItem = new JMenuItem("Open File");
		inputFileItem.addActionListener(this);
		fileMenu.add(inputFileItem);

		quitItem = new JMenuItem("Quit");
		quitItem.addActionListener(this);
		fileMenu.add(quitItem);

		// set options menu
		JMenu optionsMenu = new JMenu("Options");
		menuBar.add(optionsMenu);

		showBoxItem = new JCheckBoxMenuItem("Show Box");
		showBoxItem.setSelected(true);
		showBoxItem.addItemListener(this);
		optionsMenu.add(showBoxItem);

		showCoordItem = new JCheckBoxMenuItem("Show Coordinates");
		showCoordItem.addItemListener(this);
		optionsMenu.add(showCoordItem);

		// set screenshot menu
		JMenu screenshotMenu = new JMenu("Screenshot");
		menuBar.add(screenshotMenu);

		screenshotItem = new JMenuItem("Take Screenshot");
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
		this.parentWindow = parentWindow;

		maxX = 0;
		minX = 0;
		maxY = 0;
		minY = 0;
		maxZ = 0;
		minZ = 0;

		this.initCommonAppearance();
		this.initUI();

		setLayout(new BorderLayout());
		
		canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
		add("Center", canvas);

		// initialize the universe and scene etc...
		this.universe = new SimpleUniverse(canvas);
		this.scene = new BranchGroup();
		this.wrapperGraph = new BranchGroup();
		this.objTrans = new TransformGroup();
		this.objRotate = new TransformGroup();
		this.boxGroup = new BranchGroup();
		this.coordinatesGroup = new BranchGroup();
		this.backgroundBounds = new BoundingSphere(
				  new Point3d(0, 0, 0), 100.0);

		// set up the off screen canvas
		offScreenCanvas = new OffScreenCanvas3D(SimpleUniverse.getPreferredConfiguration(), true);
		Screen3D sOn = canvas.getScreen3D();
		Screen3D sOff = offScreenCanvas.getScreen3D();
		sOff.setSize(sOn.getSize());
		sOff.setPhysicalScreenWidth(sOn.getPhysicalScreenWidth());
		sOff.setPhysicalScreenHeight(sOn.getPhysicalScreenHeight());
		this.universe.getViewer().getView().addCanvas3D(offScreenCanvas);

		setCapabilities();

		// init more stuffs
		addMouseRotator(scene, objTrans);
		addMouseTranslation(scene, objTrans);
		addMouseZoom(scene, objTrans);
		canvas.addKeyListener(this);

		this.scene.addChild(wrapperGraph);
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

		// put camera at the right location
		float cameraZ = maxZ * 1.2f;
		viewTransform3D.setTranslation(new Vector3f(0.0f, 0.0f, cameraZ));

		viewTransformGroup.setTransform(viewTransform3D);

		// set the clipping.
		double backClipDistance = (cameraZ + Math.abs(minZ)) * 5;
		if (backClipDistance < 1.0) {
			backClipDistance = 20.0;
		}
		this.universe.getViewer().getView().setBackClipDistance(backClipDistance);
	}

	protected void showDialog() {
		this.setVisible(true);
	}

	/************* Capabilities *************/

	private void setCapabilities() {
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

		wrapperGraph.setCapability(BranchGroup.ALLOW_DETACH);

		objTrans.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		objTrans.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
		objTrans.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
		objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

		objRotate.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		objRotate.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
		objRotate.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
		objRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
	
		boxGroup.setCapability(BranchGroup.ALLOW_DETACH);

		coordinatesGroup.setCapability(BranchGroup.ALLOW_DETACH);
	}

	/************* Scene Refresh *************/

	private void ClearScreen() {
		maxX = 0;
		minX = 0;
		maxY = 0;
		minY = 0;
		maxZ = 0;
		minZ = 0;

		// remove everything
		scene.removeChild(wrapperGraph);
		wrapperGraph.removeAllChildren();
		objTrans.removeAllChildren();
		objRotate.removeAllChildren();
		boxGroup.removeAllChildren();
		coordinatesGroup.removeAllChildren();
	}

	private void RefreshScreen(String filePath) {
		initSceneGraph();
		AddAllPoints(filePath);
		createAxes();
		initBoxGroup();
		initCenterPoint();
		addLight();

		if (showBoxItem.isSelected()) {
			this.objRotate.addChild(boxGroup);
		}

		// Set the center of rotation to be the center of the cube.
		Transform3D centerTrans = new Transform3D();
		centerTrans.setTranslation(centerOfCube);
		this.objRotate.setTransform(centerTrans);
		
		// Add the rest of scene objects together.
		this.objTrans.addChild(this.objRotate);
		wrapperGraph = new BranchGroup();
		wrapperGraph.setCapability(BranchGroup.ALLOW_DETACH);
		wrapperGraph.addChild(this.objTrans);
		this.scene.addChild(wrapperGraph);

		this.setViewPlatform();
	}

	/************* Scene Creation *************/

	private void addLight() {
		// set up light.
		TransformGroup transformGroup = new TransformGroup();
		
		Color3f lightColor = new Color3f(1.0f, 1.0f, 1.0f);
		Vector3f lightDir = new Vector3f(0.0f, 0.0f, -1.0f);
		this.light = new DirectionalLight(lightColor, lightDir);
		
		Transform3D transform = new Transform3D();
		float cameraZ = maxZ * 1.5f;
		if (cameraZ < 0) {
			cameraZ = 10.0f;
		}
		Vector3f lightLocation = new Vector3f(0.0f, 0.0f, cameraZ);
		Point3d lightPoint = new Point3d(0.0, 0.0f, cameraZ);
		transform.setTranslation(lightLocation);
		
		transformGroup.setTransform(transform);
		transformGroup.addChild(light);
		objRotate.addChild(transformGroup);

		// set light bounds (best estimate).
		double boundRadius = 3.0 * (cameraZ + Math.abs(minZ));
		BoundingSphere bounds = new BoundingSphere(lightPoint, boundRadius);
		this.light.setInfluencingBounds(bounds);
	}

	private void initCenterPoint() {
		float x = maxX - (maxX - minX) / 2.0f;
		float y = maxY - (maxY - minY) / 2.0f;
		float z = maxZ - (maxZ - minZ) / 2.0f;
		
		centerOfCube = new Vector3f(-x, -y, -z);
	}

	private void initSceneGraph() {
		// set up background to be white.
		Background back = new Background();
		back.setCapability(Background.ALLOW_COLOR_WRITE);
		back.setColor(1.0f, 1.0f, 1.0f);
		back.setApplicationBounds(backgroundBounds);
		objRotate.addChild(back);
	}

	private void AddAllPoints(String filePath) {
		// add coordinates.
		TextIO.readFile(filePath);
		
		// ignore first 4 lines.
		TextIO.getln();
		TextIO.getln();
		TextIO.getln();
		TextIO.getln();
		
		// start reading actual data.
		while (TextIO.eof() == false) {
			TextIO.getInt();		// ignore the rank
			TextIO.getChar();		// ignore '-' char
			
			int x = TextIO.getInt();
			int y = TextIO.getInt();
			int z = TextIO.getInt();
			
			this.AddPoint(x, y, z);
			
			TextIO.getln();			// ignore the rest of line
			TextIO.skipBlanks();
		}

		// debugging only!
		initCenterPoint();
		TransformGroup transformGroup = new TransformGroup();
		Transform3D transform = new Transform3D();
		Vector3f center = Util.neg(centerOfCube);
		transform.setTranslation(center);
		transformGroup.setTransform(transform);
		Sphere point = new Sphere(0.75f);
		transformGroup.addChild(point);
		coordinatesGroup.addChild(transformGroup);
		// debugging end!

		objRotate.addChild(coordinatesGroup);
	}

	private void AddPoint(int x, int y, int z) {
		// update the bounds.
		if (x > maxX) {
			maxX = x;
		} else if (x < minX) {
			minX = x;
		}
		if (y > maxY) {
			maxY = y;
		} else if (y < minY) {
			minY = y;
		}
		if (z > maxZ) {
			maxZ = z;
		} else if (z < minZ) {
			minZ = z;
		}
		
		TransformGroup transformGroup = new TransformGroup();
		
		Transform3D transform = new Transform3D();
		transform.setTranslation(new Vector3f(x, y, z));		
		transformGroup.setTransform(transform);
		
		Sphere point = new Sphere(pointRadius);
		transformGroup.addChild(point);
		
		coordinatesGroup.addChild(transformGroup);
	}

	private void createAxes() {
		// create X axis.
		LineArray axisXLines = new LineArray(2, LineArray.COORDINATES | LineArray.COLOR_3);
		axisXLines.setColor(0, new Color3f(1.0f, 0.0f, 0.0f));
		axisXLines.setColor(1, new Color3f(1.0f, 0.0f, 0.0f));
		objRotate.addChild(new Shape3D(axisXLines));

		axisXLines.setCoordinate(0, new Point3f(this.minX - axisExt, 0.0f, 0.0f));
		axisXLines.setCoordinate(1, new Point3f(this.maxX + axisExt, 0.0f, 0.0f));

		// add cone.
		Cone xAxisCone = new Cone(coneRadius, coneHeight);
		xAxisCone.setAppearance(redAppearance);

		TransformGroup xTransformGroup = new TransformGroup();
		Transform3D xTransform = new Transform3D();
		xTransform.setRotation(new AxisAngle4f(new Vector3f(0, 0, 1), -1.57079633f));
		xTransform.setTranslation(new Vector3f(this.maxX + axisExt, 0.0f, 0.0f));
		xTransformGroup.setTransform(xTransform);
		xTransformGroup.addChild(xAxisCone);
		objRotate.addChild(xTransformGroup);
		
		// create Y axis.
		LineArray axisYLines = new LineArray(2, LineArray.COORDINATES | LineArray.COLOR_3);
		axisYLines.setColor(0, new Color3f(0.0f, 1.0f, 0.0f));
		axisYLines.setColor(1, new Color3f(0.0f, 1.0f, 0.0f));
		objRotate.addChild(new Shape3D(axisYLines));

		axisYLines.setCoordinate(0, new Point3f(0.0f, this.minY - axisExt, 0.0f));
		axisYLines.setCoordinate(1, new Point3f(0.0f, this.maxY + axisExt, 0.0f));

		// add cone.
		Cone yAxisCone = new Cone(coneRadius, coneHeight);
		yAxisCone.setAppearance(greenAppearance);

		TransformGroup yTransformGroup = new TransformGroup();
		Transform3D yTransform = new Transform3D();
		yTransform.setTranslation(new Vector3f(0.0f, this.maxY + axisExt, 0.0f));
		yTransformGroup.setTransform(yTransform);
		yTransformGroup.addChild(yAxisCone);
		objRotate.addChild(yTransformGroup);

		// create Z axis.
		LineArray axisZLines = new LineArray(10, LineArray.COORDINATES | LineArray.COLOR_3);
		axisZLines.setColor(0, new Color3f(0.0f, 0.0f, 1.0f));
		axisZLines.setColor(1, new Color3f(0.0f, 0.0f, 1.0f));
		objRotate.addChild(new Shape3D(axisZLines));

		axisZLines.setCoordinate(0, new Point3f(0.0f, 0.0f, this.minZ - axisExt));
		axisZLines.setCoordinate(1, new Point3f(0.0f, 0.0f, this.maxZ + axisExt));

		// add cone.
		Cone zAxisCone = new Cone(coneRadius, coneHeight);
		zAxisCone.setAppearance(blueAppearance);

		TransformGroup zTransformGroup = new TransformGroup();
		Transform3D zTransform = new Transform3D();
		zTransform.setRotation(new AxisAngle4f(new Vector3f(1, 0, 0), 1.57079633f));
		zTransform.setTranslation(new Vector3f(0.0f, 0.0f, this.maxZ + axisExt));
		zTransformGroup.setTransform(zTransform);
		zTransformGroup.addChild(zAxisCone);
		objRotate.addChild(zTransformGroup);
	}

	/************* Optional Scene Creation *************/

	private void initBoxGroup() {
		Color lineColor = Color.BLACK;
		Color3f xyWallColor = new Color3f(0.2f, 0.2f, 0.2f);
		Color3f yzWallColor = new Color3f(0.4f, 0.4f, 0.4f);
		Color3f xzWallColor = new Color3f(0.6f, 0.6f, 0.6f);

		Appearance xyAppearance = new Appearance();
		xyAppearance.setColoringAttributes(new ColoringAttributes(xyWallColor, 1));
		xyAppearance.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.3f));
	
		Appearance yzAppearance = new Appearance();
		yzAppearance.setColoringAttributes(new ColoringAttributes(yzWallColor, 1));
		yzAppearance.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.3f));
	
		Appearance xzAppearance = new Appearance();
		xzAppearance.setColoringAttributes(new ColoringAttributes(xzWallColor, 1));
		xzAppearance.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.3f));
				
		// XY wall
		QuadArray polygonXY = new QuadArray(4, QuadArray.COORDINATES);
		polygonXY.setCoordinate (0, new Point3f (minX, minY, minZ));
		polygonXY.setCoordinate (1, new Point3f (maxX, minY, minZ));
		polygonXY.setCoordinate (2, new Point3f (maxX, maxY, minZ));
		polygonXY.setCoordinate (3, new Point3f (minX, maxY, minZ));
		
		boxGroup.addChild(new Shape3D(polygonXY, xyAppearance));
		
		// XZ wall
		QuadArray polygonXZ = new QuadArray(4, QuadArray.COORDINATES);
		polygonXZ.setCoordinate (0, new Point3f (maxX, minY, maxZ));
		polygonXZ.setCoordinate (1, new Point3f (maxX, minY, minZ));
		polygonXZ.setCoordinate (2, new Point3f (minX, minY, minZ));
		polygonXZ.setCoordinate (3, new Point3f (minX, minY, maxZ));
		
		boxGroup.addChild(new Shape3D(polygonXZ, xzAppearance));
    	
		// YZ wall
		QuadArray polygonYZ = new QuadArray(4, QuadArray.COORDINATES);
		polygonYZ.setCoordinate (0, new Point3f (minX, minY, maxZ));
		polygonYZ.setCoordinate (1, new Point3f (minX, minY, minZ));
		polygonYZ.setCoordinate (2, new Point3f (minX, maxY, minZ));
		polygonYZ.setCoordinate (3, new Point3f (minX, maxY, maxZ));
		
		boxGroup.addChild(new Shape3D(polygonYZ, yzAppearance));
		
		// along X-axis.
		for (int x = (int) this.minX; x <= (int) this.maxX; x++) {
			LineArray lines = new LineArray(4, LineArray.COORDINATES | LineArray.COLOR_3);
			lines.setColor(0, new Color3f(lineColor));
			lines.setColor(1, new Color3f(lineColor));
			lines.setColor(2, new Color3f(lineColor));
			lines.setColor(3, new Color3f(lineColor));
			boxGroup.addChild(new Shape3D(lines));
		    
			// y
			lines.setCoordinate(0, new Point3f(x, minY, minZ));
			lines.setCoordinate(1, new Point3f(x, minY, maxZ));
			// z
			lines.setCoordinate(2, new Point3f(x, minY, minZ));
			lines.setCoordinate(3, new Point3f(x, maxY, minZ));
		}
		
		// along Y-axis.
		for (int y = (int) this.minY; y <= (int) this.maxY; y++) {
			LineArray lines = new LineArray(4, LineArray.COORDINATES | LineArray.COLOR_3);
			lines.setColor(0, new Color3f(lineColor));
			lines.setColor(1, new Color3f(lineColor));
			lines.setColor(2, new Color3f(lineColor));
			lines.setColor(3, new Color3f(lineColor));
			boxGroup.addChild(new Shape3D(lines));
		    
			// x
			lines.setCoordinate(0, new Point3f(minX, y, minZ));
			lines.setCoordinate(1, new Point3f(minX, y, maxZ));
			// z
			lines.setCoordinate(2, new Point3f(minX, y, minZ));
			lines.setCoordinate(3, new Point3f(maxX, y, minZ));
		}
		
		// along Z-axis.
		for (int z = (int) this.minZ; z <= (int) this.maxZ; z++) {
			LineArray lines = new LineArray(4, LineArray.COORDINATES | LineArray.COLOR_3);
			lines.setColor(0, new Color3f(lineColor));
			lines.setColor(1, new Color3f(lineColor));
			lines.setColor(2, new Color3f(lineColor));
			lines.setColor(3, new Color3f(lineColor));
			boxGroup.addChild(new Shape3D(lines));
		    
			// x
			lines.setCoordinate(0, new Point3f(minX, minY, z));
			lines.setCoordinate(1, new Point3f(minX, maxY, z));
			// y
			lines.setCoordinate(2, new Point3f(minX, minY, z));
			lines.setCoordinate(3, new Point3f(maxX, minY, z));
		}
	}
	
	/************* Mouse Controls *************/
	
	private void addMouseRotator(BranchGroup scene, TransformGroup objGroup) {
		// Rotation.
		MouseRotate myMouseRotate = new MouseRotate();
		myMouseRotate.setTransformGroup(objGroup);
		myMouseRotate.setSchedulingBounds(backgroundBounds);
		scene.addChild(myMouseRotate);
	}
	
	private void addMouseTranslation(BranchGroup scene, TransformGroup objGroup) {
		// Translation.
		MouseTranslate translateBehavior = new MouseTranslate();
		translateBehavior.setTransformGroup(objGroup);
		translateBehavior.setSchedulingBounds(backgroundBounds);
		scene.addChild(translateBehavior);
	}
	
	private void addMouseZoom(BranchGroup scene, TransformGroup objGroup) {
		// Zoom in/out.
		MouseZoom zoomBehavior = new MouseZoom();
		zoomBehavior.setTransformGroup(objGroup);
		zoomBehavior.setSchedulingBounds(backgroundBounds);
		scene.addChild(zoomBehavior);
	}

	/************* Keyboard Control *************/

	@Override
	public void keyPressed(KeyEvent e) {
		// Rotate 'objRotate' by rotating 'objTrans'.
		// 'objRotate' is already centered at the center of the cube, so
		// no need to translate!

		float rotateX = 0.0f;
		float rotateY = 0.0f;
		float rotateZ = 0.0f;

 		// Retrieve the old translation.
		Transform3D oldTransform = new Transform3D();
		objTrans.getTransform(oldTransform);

		// Perform rotation.
		switch(e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				rotateY = -rotationStepRadian;
				break;
			case KeyEvent.VK_RIGHT:
				rotateY = rotationStepRadian;
				break;
			case KeyEvent.VK_UP:
				rotateX = -rotationStepRadian;
				break;
			case KeyEvent.VK_DOWN:
				rotateX = rotationStepRadian;
				break;
			case KeyEvent.VK_N:
				rotateZ = rotationStepRadian;
				break;
			case KeyEvent.VK_M:
				rotateZ = -rotationStepRadian;
				break;
		}

		Transform3D xRotation = new Transform3D();
		xRotation.rotX(rotateX);
		Transform3D yRotation = new Transform3D();
		yRotation.rotY(rotateY);
		Transform3D zRotation = new Transform3D();
		zRotation.rotZ(rotateZ);

		oldTransform.mul(xRotation);
		oldTransform.mul(yRotation);
		oldTransform.mul(zRotation);

		objTrans.setTransform(oldTransform);
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// Not used.
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// Not used.
	}

	/************* Implemented Listeners *************/

	@Override
	public void actionPerformed(ActionEvent ae) {
		JMenuItem source = (JMenuItem) ae.getSource();

		if (source == inputFileItem) {
			// Open a file.
			JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
			fc.setFileFilter(new TopologyFileFilter());

			int returnVal = fc.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				
				String absFilePath = file.getAbsolutePath();
		
				// clear the screen, and load the newly selected file
				ClearScreen();
				RefreshScreen(absFilePath);
			}
		} else if (source == quitItem) {
			// Exit the window.
			this.close();
		} else if (source == screenshotItem) { 
			// Take a screenshot of visible window.
			screenshotCount++;
			String fileName = String.format("./TopologyScreenshot%03d.png", screenshotCount);

			Point loc = canvas.getLocationOnScreen();
			offScreenCanvas.setOffScreenLocation(loc);

			OffScreenCanvas3DToImage.saveToFileChooserSelection(offScreenCanvas,
					  "Save Topology Image", fileName, canvas.getSize().width,
					  canvas.getSize().height);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();

		if (source == showBoxItem) {
		
		} else if (source == showCoordItem) {
		
		}
	}
}

