package projections.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import java.awt.Insets;
import java.util.SortedSet;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;

import projections.analysis.CallTable;

class CallTableWindow extends ProjectionsWindow
    implements ActionListener
{

    private CallTableWindow      thisWindow;
    
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    private int myRun = 0;
    
    private CallTableTextArea    textArea;
    private JLabel               lTitle;
    private JPanel               titlePanel;

    private JPanel	         mainPanel;
    private JPanel		 buttonPanel1;
    private JPanel		 buttonPanel2;
    private JPanel               controlPanel;

    private JButton              setRanges;

    private ButtonGroup		 bg1;
    private JRadioButton	 epDetailOn;
    private JRadioButton	 epDetailOff;

    private ButtonGroup		 bg2;
    private JRadioButton	 statsOn;
    private JRadioButton	 statsOff;
    
    private SortedSet<Integer>   validPEs;
    private long                 startTime;
    private long                 endTime;
    
    private CallTable            ct;
    
    private boolean              epDetailToggle;
    private boolean              statsToggle;

    private JButton		 bSourceEntryColor;
    private JButton		 bDestEntryColor;
    private JButton		 bStatsColor;
    private JButton		 bBackgroundColor;

    protected CallTableWindow(MainWindow parentWindow ) {
    	super(parentWindow);
    	thisWindow = this;

	setBackground(Color.black);
    	setTitle("Projections Call Table - " + MainWindow.runObject[myRun].getFilename() + ".sts");

    	mainPanel = new JPanel();
    	epDetailToggle = false;
    	statsToggle = false;
    	setLayout(mainPanel);
    	createMenus();
    	CreateLayout();
    	pack();
    	showDialog();
    	setVisible(true);
    }

    public void actionPerformed(ActionEvent e)
    {
	if(e.getSource() instanceof JButton)
	{
	    JButton b = (JButton)e.getSource();
	    
	    if(b == setRanges)
		showDialog();
	    else if (b == bSourceEntryColor)
	    {
		JColorChooser colorChooser = new JColorChooser();
		Color c = JColorChooser.showDialog(bSourceEntryColor,
				"Choose Source Entry Point Color",
				textArea.getSourceEntryColor());
		if (c != null) textArea.setSourceEntryColor(c);
	    }
	    else if (b == bDestEntryColor)
	    {
		JColorChooser colorChooser = new JColorChooser();
		Color c = JColorChooser.showDialog(bDestEntryColor,
				"Choose Destination Entry Point Color",
				textArea.getDestEntryColor());
		if (c != null) textArea.setDestEntryColor(c);
	    }
	    else if (b == bStatsColor)
	    {
		JColorChooser colorChooser = new JColorChooser();
		Color c = JColorChooser.showDialog(bStatsColor,
				"Choose Statistics Color",
				textArea.getStatsColor());
		if (c != null) textArea.setStatsColor(c);
	    }
	    else if (b == bBackgroundColor)
	    {
		JColorChooser colorChooser = new JColorChooser();
		Color c = JColorChooser.showDialog(bBackgroundColor,
				"Choose Background Color",
				getBackground());
		if (c != null)
		{
			setBackground(c);
			titlePanel.setBackground(c);
			if (c.equals(Color.white)) lTitle.setForeground(Color.black);
			if (c.equals(Color.black)) lTitle.setForeground(Color.white);
			textArea.setBackground(c);
			textArea.setTextAreaBackgroundColor();
		}
	    }
        }
	else if (e.getSource() instanceof JMenuItem)
	{
            String arg = ((JMenuItem)e.getSource()).getText();
            if (arg.equals("Close")) {
                close();
            } else if(arg.equals("Select Processors")) {
                showDialog();
            } else if(arg.equals("Change Colors")) {
		changeColors();
	    }
        }
	else if (e.getSource() instanceof JRadioButton)
	{
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		if (e.getSource() == statsOff)
		{
			statsToggle = false;
			textArea.setText(ct.getCallTableText(epDetailToggle, statsToggle));
		}
		else if (e.getSource() == statsOn)
		{
			statsToggle = true;
			textArea.setText(ct.getCallTableText(epDetailToggle, statsToggle));
		}
		else if (e.getSource() == epDetailOff)
		{
			epDetailToggle = false;
			textArea.setText(ct.getCallTableText(epDetailToggle, statsToggle));
		}
		else if (e.getSource() == epDetailOn)
		{
			epDetailToggle = true;
			textArea.setText(ct.getCallTableText(epDetailToggle, statsToggle));
		}
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
    }

    public void changeColors()
    {
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.fill = GridBagConstraints.BOTH;
	gbc.insets = new Insets(2, 2, 2, 2);

	bSourceEntryColor = new JButton("Change Source Entry Point Color");
	bSourceEntryColor.addActionListener(this);

	bDestEntryColor = new JButton("Change Destination Entry Point Color");
	bDestEntryColor.addActionListener(this);

	bStatsColor = new JButton("Change Statistics Color");
	bStatsColor.addActionListener(this);

	bBackgroundColor = new JButton("Change Background Color");
	bBackgroundColor.addActionListener(this);

	JPanel colorPanel = new JPanel();
	colorPanel.setLayout(gbl);
	colorPanel.add(bSourceEntryColor);
	colorPanel.add(bDestEntryColor);
	colorPanel.add(bStatsColor);
	colorPanel.add(bBackgroundColor);

	Util.gblAdd(colorPanel, bSourceEntryColor,     gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(colorPanel, bDestEntryColor,       gbc, 0,2, 1,1, 1,1);
	Util.gblAdd(colorPanel, bStatsColor,           gbc, 0,3, 1,1, 1,1);
	Util.gblAdd(colorPanel, bBackgroundColor,      gbc, 0,4, 1,1, 1,1);

	JFrame colorFrame = new JFrame("Call Table - Change Colors");
	colorFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	colorFrame.add(colorPanel);
	colorFrame.pack();
	colorFrame.setVisible(true);
    }
    
    public void showDialog() {
	if (dialog == null) {
		dialog = new RangeDialog(this, "select Range", null, false);
	}
	
	dialog.displayDialog();
	if (!dialog.isCancelled()) {
		validPEs = dialog.getSelectedProcessors();
		startTime = dialog.getStartTime();
		endTime = dialog.getEndTime();
		final SwingWorker worker = new SwingWorker() {
			public Object doInBackground() {
				ct = new CallTable(startTime, endTime, validPEs);
				ct.GatherData(thisWindow);
				textArea.setText(ct.getCallTableText(epDetailToggle, statsToggle));
				return null;
			}
			public void done() {
			}
		};
		worker.execute();
	}
    }

    private void createMenus(){
        JMenuBar mbar = new JMenuBar();
        mbar.add(Util.makeJMenu("File", new Object[]
            {
                "Select Processors",
                null,
                                    "Close"
            },
                                this));
        mbar.add(Util.makeJMenu("Tools", new Object[]
            {
                "Change Colors",
            },
                                this));
     
        setJMenuBar(mbar);
    }

    private void CreateLayout()
    {  
	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  mainPanel.setLayout(gbl);
	  
	  textArea = new CallTableTextArea();
	  
	  titlePanel = new JPanel();
	  titlePanel.setBackground(Color.black);
	  lTitle = new JLabel("CALL TABLE", JLabel.CENTER);
	  lTitle.setForeground(Color.white);
	  lTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
	  titlePanel.add(lTitle);

	  // buttonPanel1 items
	  bg1 = new ButtonGroup();
	  epDetailOff = new JRadioButton("EP Detail Off", true);
	  epDetailOff.addActionListener(this);
	  epDetailOn = new JRadioButton("EP Detail On", false);
	  epDetailOn.addActionListener(this);
	  bg1.add(epDetailOff);
	  bg1.add(epDetailOn);
	  buttonPanel1 = new JPanel();
	  Util.gblAdd(buttonPanel1, epDetailOff, gbc, 0,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel1, epDetailOn, gbc, 1,0, 1,1, 1,1);

	  // buttonPanel2 items
	  bg2 = new ButtonGroup();
	  statsOff = new JRadioButton("Statistics Off", true);
	  statsOff.addActionListener(this);
	  bg2.add(statsOff);
	  statsOn = new JRadioButton("Statistics On", false);
	  statsOn.addActionListener(this);
	  bg2.add(statsOn);
	  buttonPanel2 = new JPanel();
	  Util.gblAdd(buttonPanel2, statsOff, gbc, 0,0, 1,1, 1,1);
	  Util.gblAdd(buttonPanel2, statsOn, gbc, 1,0, 1,1, 1,1);
	  
	  // control panel items
	  setRanges = new JButton("Select New Range");
	  setRanges.addActionListener(this);
	  controlPanel = new JPanel();
	  controlPanel.setLayout(gbl);
	  Util.gblAdd(controlPanel, setRanges, gbc, 0,0, 1,1, 0,0);
	  
	  //Util.gblAdd(p, checkBoxPanel,  gbc, 0,2, 1,1, 1,1, 0,5,5,5);
	  //Util.gblAdd(p, controlPanel,   gbc, 0,3, 1,1, 1,1, 0,5,5,5);
	  
	  Util.gblAdd(mainPanel, titlePanel,     gbc, 0,1, 1,1, 0,0);
	  Util.gblAdd(mainPanel, textArea,       gbc, 0,2, 1,1, 1,1);
	  Util.gblAdd(mainPanel, buttonPanel1,   gbc, 0,3, 1,1, 0,0);
	  Util.gblAdd(mainPanel, buttonPanel2,   gbc, 0,4, 1,1, 0,0);
	  Util.gblAdd(mainPanel, controlPanel,   gbc, 0,5, 1,0, 0,0);
    }

}
