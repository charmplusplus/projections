package projections.Tools.TopologyDisplay;

import projections.gui.MainWindow;
import projections.gui.ProjectionsWindow;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.TransformGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.vecmath.Color3f;

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
public class TopologyDisplayWindow extends ProjectionsWindow 
		implements ItemListener {
 	
	static final float pointRadius = 0.13f;
	static final float coneRadius = 0.12f;
	static final float coneHeight = 0.3f;

	private Appearance redAppearance;
	private Appearance greenAppearance;
	private Appearance blueAppearance;

	private SimpleUniverse univserse;
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

		showDialog();
	}

	protected void showDialog() {
		this.setVisible(true);
	}


	/************* Scene Creation *************/


	/************* Optional Scene Creation *************/

    
	/************* Controls *************/


	/************* Implemented Listeners *************/

	@Override
	public void itemStateChanged(ItemEvent e) {
		JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();

		if (source.getText().equals("Show Box")) {
		
		} else if (source.getText().equals("Show Coordinates")) {
		
		}
	}
}

