package projections.Tools.TopologyDisplay;

import projections.gui.MainWindow;
import projections.gui.ProjectionsWindow;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.awt.BorderLayout;
import java.awt.Color;

import java.io.File;

import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.LineArray;
import javax.media.j3d.QuadArray;
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
	private BranchGroup coordinatesGroup;
	private BranchGroup wrapperGraph;
	private BoundingSphere backgroundBounds;

	private DirectionalLight light;

	private int maxX, minX;
	private int maxY, minY;
	private int maxZ, minZ;
	static final float axisExt = 1.5f;

	private JCheckBoxMenuItem showBoxItem;
	private JCheckBoxMenuItem showCoordItem;

	/************* Initialization *************/

	public void initUI() {
		JMenuBar menuBar = new JMenuBar();

		// set file menu
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		JMenuItem inputFileItem = new JMenuItem("Open File");
		inputFileItem.addActionListener(this);
		fileMenu.add(inputFileItem);

		JMenuItem quitItem = new JMenuItem("Quit");
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
		this.wrapperGraph = new BranchGroup();
		this.objRotate = new TransformGroup();
		this.boxGroup = new BranchGroup();
		this.coordinatesGroup = new BranchGroup();
		this.backgroundBounds = new BoundingSphere(
				  new Point3d(0, 0, 0), 100.0);

		setCapabilities();

		// init more stuffs
		addMouseRotator(scene, objRotate);
		addMouseTranslation(scene, objRotate);
		addMouseZoom(scene, objRotate);

		setViewPlatform();

		this.scene.addChild(wrapperGraph);
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

	/************* Capabilities *************/

	private void setCapabilities() {
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

		wrapperGraph.setCapability(BranchGroup.ALLOW_DETACH);

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
		objRotate.removeAllChildren();
		boxGroup.removeAllChildren();
		coordinatesGroup.removeAllChildren();
	}

	private void RefreshScreen(String filePath) {
		initSceneGraph();
		AddAllPoints(filePath);
		createAxes();
		initBoxGroup();

		if (showBoxItem.isSelected()) {
			this.objRotate.addChild(boxGroup);
		}
		
		wrapperGraph = new BranchGroup();
		wrapperGraph.setCapability(BranchGroup.ALLOW_DETACH);
		wrapperGraph.addChild(this.objRotate);

		this.scene.addChild(wrapperGraph);
	}

	/************* Scene Creation *************/

	private void initSceneGraph() {
		// set up background to be white.
		Background back = new Background();
		back.setCapability(Background.ALLOW_COLOR_WRITE);
		back.setColor(1.0f, 1.0f, 1.0f);
		back.setApplicationBounds(backgroundBounds);
		objRotate.addChild(back);
		
		// set up light.
		TransformGroup transformGroup = new TransformGroup();
		
		Color3f lightColor = new Color3f(1.0f, 1.0f, 1.0f);
		Vector3f lightDir = new Vector3f(0.0f, 0.0f, -1.0f);
		this.light = new DirectionalLight(lightColor, lightDir);
		
		Transform3D transform = new Transform3D();
		transform.setTranslation(new Vector3f(0, 0, 2));
		
		transformGroup.setTransform(transform);
		transformGroup.addChild(light);
		objRotate.addChild(transformGroup);
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

		objRotate.addChild(coordinatesGroup);
		
		// reset light bounds.
		double boundRadius = Math.max(Math.max((maxX - minX), (maxY - minY)), maxZ - minZ) / 2 + 5.0;
		BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), boundRadius);
		this.light.setInfluencingBounds(bounds);
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
		
		Appearance appearance = new Appearance();
		appearance.setColoringAttributes(new ColoringAttributes(new Color3f(lineColor), 1));
		appearance.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.3f));
		
		// XY wall
		QuadArray polygonXY = new QuadArray(4, QuadArray.COORDINATES);
		polygonXY.setCoordinate (0, new Point3f (minX, minY, minZ));
		polygonXY.setCoordinate (1, new Point3f (maxX, minY, minZ));
		polygonXY.setCoordinate (2, new Point3f (maxX, maxY, minZ));
		polygonXY.setCoordinate (3, new Point3f (minX, maxY, minZ));
		
		boxGroup.addChild(new Shape3D(polygonXY, appearance));
		
		// XZ wall
		QuadArray polygonXZ = new QuadArray(4, QuadArray.COORDINATES);
		polygonXZ.setCoordinate (0, new Point3f (maxX, minY, maxZ));
		polygonXZ.setCoordinate (1, new Point3f (maxX, minY, minZ));
		polygonXZ.setCoordinate (2, new Point3f (minX, minY, minZ));
		polygonXZ.setCoordinate (3, new Point3f (minX, minY, maxZ));
		
		boxGroup.addChild(new Shape3D(polygonXZ, appearance));
    	
		// YZ wall
		QuadArray polygonYZ = new QuadArray(4, QuadArray.COORDINATES);
		polygonYZ.setCoordinate (0, new Point3f (minX, minY, maxZ));
		polygonYZ.setCoordinate (1, new Point3f (minX, minY, minZ));
		polygonYZ.setCoordinate (2, new Point3f (minX, maxY, minZ));
		polygonYZ.setCoordinate (3, new Point3f (minX, maxY, maxZ));
		
		boxGroup.addChild(new Shape3D(polygonYZ, appearance));
		
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
	
	/************* Controls *************/
	
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

	/************* Implemented Listeners *************/

	@Override
	public void actionPerformed(ActionEvent ae) {
		JMenuItem source = (JMenuItem) ae.getSource();

		if (source.getText().equals("Open File")) {
			// open a File Chooser
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
		} else {
		
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

