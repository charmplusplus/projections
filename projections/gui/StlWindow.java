package projections.gui;

/**
 * Small Time Line (Stl) Display Panel
 * Orion Sky Lawlor, olawlor@acm.org, 2/9/2001
 *
 * A Stl compresses an entire parallel run into a single
 * image by coding processor utilization as color.
 * Since images are assembled pixel-by-pixel, this is
 * much faster than a timeline.
 */

import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;

public class StlWindow extends ProjectionsWindow
implements MouseListener, ActionListener, ScalePanel.StatusDisplay, 
ItemListener
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	private ScaleSlider hor,ver;
	private ScalePanel scalePanel;
	StlPanel stl;
	private Label status;
	// Modified to display data by entry method color. Mode panel.
	public static final int MODE_UTILIZATION = 0;
	public static final int MODE_EP = 1;

	private int mode = MODE_UTILIZATION;

	private JPanel modePanel;
	private ButtonGroup modeGroup;
	JRadioButton utilizationMode;
	private JRadioButton epMode;

	private ColorMap utilColorMap;

	StlWindow thisWindow;

	
	public StlWindow(MainWindow mainWindow)
	{
		super(mainWindow);
		thisWindow = this;

		setForeground(Color.lightGray);
		setTitle("Projections Overview - " + MainWindow.runObject[myRun].getFilename() + ".sts");

		createMenus();
		createLayout();
		pack();
		showDialog();
	}

	private void createLayout()
	{
		GridBagLayout      gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		Container windowPane = this.getContentPane();
		windowPane.setLayout(gbl);

		JPanel displayPanel = new JPanel();
		displayPanel.setLayout(gbl);

		status = new Label("");
		status.setBackground(Color.black);
		status.setForeground(Color.lightGray);

		hor=new ScaleSlider(Scrollbar.HORIZONTAL);
		hor.addMouseListener(this);

		ver=new ScaleSlider(Scrollbar.VERTICAL);
		ver.addMouseListener(this);

		stl=new StlPanel(thisWindow);
		scalePanel=new ScalePanel(hor,ver,stl);

		gbc.fill = GridBagConstraints.BOTH;
		Util.gblAdd(displayPanel, scalePanel, gbc, 0,0, 1,1, 1,1);
		gbc.fill = GridBagConstraints.VERTICAL;
		Util.gblAdd(displayPanel, ver,        gbc, 1,0, 1,1, 0,1);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		Util.gblAdd(displayPanel, hor,        gbc, 0,1, 1,1, 1,0);
		Util.gblAdd(displayPanel, status,     gbc, 0,2, 1,1, 1,0);

		// create mode panel
		modePanel = new JPanel();
		modeGroup = new ButtonGroup();
		utilizationMode = new JRadioButton("Utilization", true);
		utilizationMode.addItemListener(this);
		epMode = new JRadioButton("By EP Colors", false);
		epMode.addItemListener(this);
		modeGroup.add(utilizationMode);
		modeGroup.add(epMode);

		if (!MainWindow.runObject[myRun].hasLogData()) {
			epMode.setEnabled(false);
		}

		gbc.fill = GridBagConstraints.HORIZONTAL;
		Util.gblAdd(modePanel, utilizationMode, gbc, 0,0, 1,1, 1,1, 1,1,1,1);
		Util.gblAdd(modePanel, epMode,          gbc, 1,0, 1,1, 1,1, 1,1,1,1);

		gbc.fill = GridBagConstraints.BOTH;
		Util.gblAdd(windowPane, displayPanel, gbc, 0,0, 1,1, 1,1, 1,1,1,1);
		Util.gblAdd(windowPane, modePanel,    gbc, 0,1, 1,1, 1,0, 1,1,1,1);
		scalePanel.setStatusDisplay(this);

		// Establishing the Utilization-only color map. 
		// This never changes from the get-go, so there's no reason (like 
		// in the previous code) to keep resetting it.
		utilColorMap = new ColorMap();
		// sets bluish background for zero values and range to bright Red
		// for value 70.
		// WAY TO READ THIS:  VAL  RED  GRN  BLU
		utilColorMap.addBreak(0,   0,   0,   55, 
				70,  255, 0,   0); 
		// Range from bright Red to White from 70 to 100
		utilColorMap.addBreak(70,  255, 0,   0, 
				100, 255, 255, 255);
		// 12/9/2004 - new semantics. Anything that's not a valid utilization
		//             is blue (200 shades to indicate intensity) for 
		//             Idle time.
		utilColorMap.addBreak(101, 0,   0,   55, 
				201, 0,   0,   255);
		// everything else is green (should not happen).
		utilColorMap.addBreak(202, 0,   255, 0,
				255, 0,   255, 0);
		stl.setColorMap(utilColorMap);
	}  

	void setStlPanelData(long startTime, long endTime, OrderedIntList pes){
		double horSize, verSize;
		if (pes == null) {
			horSize=MainWindow.runObject[myRun].getTotalTime();
			verSize=MainWindow.runObject[myRun].getNumProcessors();
		} else {	
			horSize = endTime-startTime;
			if(horSize <= 0)
				horSize = MainWindow.runObject[myRun].getTotalTime();
			verSize = pes.size();
		}	 
		scalePanel.setScales(horSize,verSize);

		double hMin=scalePanel.toSlider(1.0/horSize);
		double hMax=scalePanel.toSlider(0.01);//0.1ms fills screen
		hor.setMin(hMin); hor.setMax(hMax);
		hor.setValue(hMin);
		hor.setTicks(Math.floor(hMin),1);

		double vMin=scalePanel.toSlider(1.0/verSize);
		double vMax=scalePanel.toSlider(1.0);//One processor fills screen
		ver.setMin(vMin); ver.setMax(vMax);
		ver.setValue(vMin);
		ver.setTicks(Math.floor(vMin),1);
	}

	private void createMenus()
	{
		MenuBar mbar = new MenuBar();

		mbar.add(Util.makeMenu("File", new Object[]
		                                          {
				"Close"
		                                          }, this));
		mbar.add(Util.makeMenu("Modify", new Object[]
		                                            {
				"Set Range"
		                                            }, this));
		setMenuBar(mbar);
	} 

	public void showDialog()
	{
		try {
			if (dialog == null) {
				dialog = new RangeDialog(this, "Select Range", null, false);
			}

			dialog.displayDialog();
			if (!dialog.isCancelled()) {
				final OrderedIntList pes = dialog.getSelectedProcessors();
				final long startTime = dialog.getStartTime();
				final long endTime = dialog.getEndTime();
				final SwingWorker worker = new SwingWorker() {
					public Object doInBackground() {
						thisWindow.setVisible(false);
						thisWindow.setStlPanelData(startTime, endTime, pes);
						stl.resetMode();
						utilizationMode.setSelected(true);
						stl.setData(pes,startTime,endTime); 
						return null;
					}
					public void done() {
						thisWindow.setVisible(true);
						thisWindow.repaint();
					}
				};
				worker.execute();
			}
		} catch (Exception e) { 
			e.printStackTrace();
		}
	}


	public void actionPerformed(ActionEvent evt)
	{
		if (evt.getSource() instanceof MenuItem) {
			MenuItem mi = (MenuItem)evt.getSource();
			String arg = mi.getLabel();
			if(arg.equals("Close"))  {
				close();
			}
			if (arg.equals("Set Range")) {
				showDialog();
			}
		}
	}  

	public void itemStateChanged(ItemEvent evt) {
		if (evt.getStateChange() == ItemEvent.SELECTED) {
			JRadioButton button = (JRadioButton)evt.getItemSelectable();
			if (button == utilizationMode) {
				mode = MODE_UTILIZATION;
			} else if (button == epMode) {
				mode = MODE_EP;
			}
			// handle the effects of a mode change
			stl.setMode(mode);
		}
	}

	public void mouseClicked(MouseEvent evt) {
	}

	public void mouseEntered(MouseEvent evt) {
		Object src=evt.getComponent();
		if (src==hor) setStatus("Click or drag to set the horizontal zoom");
		if (src==ver) setStatus("Click or drag to set the vertical zoom");
	}

	public void mouseExited(MouseEvent evt) {
		setStatus("");//Clear the old message
	}

	public void mousePressed(MouseEvent evt) {
	}

	public void mouseReleased(MouseEvent evt) {
	}

	public void setStatus(String msg) {
		status.setText(msg);
	}



}
