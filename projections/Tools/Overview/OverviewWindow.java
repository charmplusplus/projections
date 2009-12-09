package projections.Tools.Overview;

/**
 * Overview, aka Small Time Line (Stl) Display Panel
 * Orion Sky Lawlor, olawlor@acm.org, 2/9/2001
 * Isaac, 2009
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;

import projections.gui.ColorMap;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.ProjectionsWindow;
import projections.gui.RangeDialog;
import projections.gui.ScalePanel;
import projections.gui.ScaleSlider;
import projections.gui.Util;

public class OverviewWindow extends ProjectionsWindow
implements MouseListener, ActionListener, ScalePanel.StatusDisplay
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	private ScaleSlider hor,ver;
	JRadioButton colorByEntryMethod;

	JRadioButton colorByUtil;

	private ScalePanel scalePanel;
	OverviewPanel stl;
	private Label status;
	// Modified to display data by entry method color. Mode panel.
	public static final int MODE_UTILIZATION = 0;
	public static final int MODE_EP = 1;

	private ColorMap utilColorMap;

	OverviewWindow thisWindow;

	
	public OverviewWindow(MainWindow mainWindow)
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

		stl=new OverviewPanel();
		scalePanel=new ScalePanel(hor,ver,stl);

		gbc.fill = GridBagConstraints.BOTH;
		Util.gblAdd(displayPanel, scalePanel, gbc, 0,0, 1,1, 1,1);
		gbc.fill = GridBagConstraints.VERTICAL;
		Util.gblAdd(displayPanel, ver,        gbc, 1,0, 1,1, 0,1);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		Util.gblAdd(displayPanel, hor,        gbc, 0,1, 1,1, 1,0);
		Util.gblAdd(displayPanel, status,     gbc, 0,2, 1,1, 1,0);

		gbc.fill = GridBagConstraints.BOTH;
		Util.gblAdd(windowPane, displayPanel, gbc, 0,0, 1,1, 1,1, 1,1,1,1);
		scalePanel.setStatusDisplay(this);
		
		
		colorByEntryMethod = new JRadioButton("Entry Method");
		colorByUtil = new JRadioButton("Utilization");
		colorByEntryMethod.setSelected(true);
	    //Group the radio buttons.
	    ButtonGroup group = new ButtonGroup();
	    group.add(colorByEntryMethod);
	    group.add(colorByUtil);
	    colorByEntryMethod.addActionListener(this);
	    colorByUtil.addActionListener(this);
	    JPanel radioPanel = new JPanel();
	    radioPanel.setLayout(new GridBagLayout());   
		Util.gblAdd(radioPanel, new JLabel("Color By:"), gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(radioPanel, colorByEntryMethod, gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(radioPanel, colorByUtil, gbc, 2,0, 1,1, 1,1);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		Util.gblAdd(windowPane, radioPanel, gbc, 0,3, 1,1, 0,0, 1,1,1,1);


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
	
	OverviewDialogExtension toolSpecificPanel;

	public void showDialog()
	{
		try {
			if (dialog == null) {
				toolSpecificPanel = new OverviewDialogExtension();
				dialog = new RangeDialog(this, "Select Range", toolSpecificPanel, false);
			}
			dialog.displayDialog();
			if (!dialog.isCancelled()) {
				final OrderedIntList pes = dialog.getSelectedProcessors();
				final long startTime = dialog.getStartTime();
				final long endTime = dialog.getEndTime();
				thisWindow.setVisible(false);
				thisWindow.setStlPanelData(startTime, endTime, pes);

				final SwingWorker worker = new SwingWorker() {
					public Object doInBackground() {
						stl.setRanges(pes,startTime,endTime);
						stl.loadData(toolSpecificPanel.cbGenerateImage.isSelected());
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
		if (evt.getSource() == colorByEntryMethod){
			stl.colorByEntry();
		} else if (evt.getSource() == colorByUtil){
			stl.colorByUtil();
		} else if (evt.getSource() instanceof MenuItem) {
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
