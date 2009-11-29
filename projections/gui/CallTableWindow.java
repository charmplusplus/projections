package projections.gui;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import projections.analysis.CallTable;

public class CallTableWindow extends ProjectionsWindow
    implements ItemListener
{

	CallTableWindow      thisWindow;    
    
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;
    
    CallTableTextArea    textArea;
    private Label                lTitle;
    private Panel                titlePanel;

    private JPanel	         mainPanel;
    private JPanel	         checkBoxPanel1;
    private JPanel	         checkBoxPanel2;
    private JPanel               controlPanel;

    private JButton              setRanges;
    
    private CheckboxGroup        cbg1;
    private Checkbox	         epDetailOn;
    private Checkbox	         epDetailOff;
    
    private CheckboxGroup	 cbg2;
    private Checkbox		 statsOn;
    private Checkbox		 statsOff;
    
    public OrderedIntList        validPEs;
    public long                  startTime;
    public long                  endTime;
    
    CallTable            ct;
    
    boolean              epDetailToggle;
    boolean              statsToggle;
  

    public CallTableWindow(MainWindow parentWindow ) {
    	super(parentWindow);
    	thisWindow = this;

    	setBackground(Color.lightGray);
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
	if(e.getSource() instanceof JButton) {
	    JButton b = (JButton)e.getSource();
	    
	    if(b == setRanges)
		showDialog();
        } else if (e.getSource() instanceof JMenuItem) {
            String arg = ((JMenuItem)e.getSource()).getText();
            if (arg.equals("Close")) {
                close();
            } else if(arg.equals("Select Processors")) {
                showDialog();
            }
        }
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

    protected void createMenus(){
        JMenuBar mbar = new JMenuBar();
        mbar.add(Util.makeJMenu("File", new Object[]
            {
                "Select Processors",
                null,
                                    "Close"
            },
                                null, this));
        mbar.add(Util.makeJMenu("Tools", new Object[]
            {
                "Change Colors",
            },
                                null, this));
        mbar.add(Util.makeJMenu("Help", new Object[]
            {
                "Index",
                                    "About"
            },
                                null, this));
        setJMenuBar(mbar);
    }

    private void CreateLayout()
    {  
	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  mainPanel.setLayout(gbl);
	  
	  textArea = new CallTableTextArea();
	  
	  titlePanel = new Panel();
	  titlePanel.setBackground(Color.black);
	  lTitle = new Label("CALL TABLE", Label.CENTER);
	  lTitle.setForeground(Color.white);
	  lTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
	  titlePanel.add(lTitle);

	  // checkbox panel 1 items
	  cbg1 = new CheckboxGroup();
	  epDetailOff = new Checkbox("EP Detail Off", cbg1, true);
	  epDetailOff.addItemListener(this);
	  epDetailOn = new Checkbox("EP Detail On", cbg1, false);
	  epDetailOn.addItemListener(this);
	  checkBoxPanel1 = new JPanel();
	  Util.gblAdd(checkBoxPanel1, epDetailOff, gbc, 0,0, 1,1, 1,1);
          Util.gblAdd(checkBoxPanel1, epDetailOn, gbc, 1,0, 1,1, 1,1);

	  // checkbox panel 2 items
	  cbg2 = new CheckboxGroup();
	  statsOff = new Checkbox("Statistics Off", cbg2, true);
	  statsOff.addItemListener(this);
	  statsOn = new Checkbox("Statistics On", cbg2, false);
	  statsOn.addItemListener(this);
	  checkBoxPanel2 = new JPanel();
          Util.gblAdd(checkBoxPanel2, statsOff, gbc, 0,0, 1,1, 1,1);
	  Util.gblAdd(checkBoxPanel2, statsOn, gbc, 1,0, 1,1, 1,1);
	  
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
	  Util.gblAdd(mainPanel, checkBoxPanel1,  gbc, 0,3, 1,1, 0,0);
	  Util.gblAdd(mainPanel, checkBoxPanel2,  gbc, 0,4, 1,1, 0,0);
	  Util.gblAdd(mainPanel, controlPanel,   gbc, 0,5, 1,0, 0,0);
    }

    public void itemStateChanged(ItemEvent ae){
	if(ae.getSource() instanceof Checkbox){
	    setCursor(new Cursor(Cursor.WAIT_CURSOR));
	    Checkbox cb = (Checkbox)ae.getSource();
	    setCheckboxData(cb);
	    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
    }

    public void setCheckboxData(Checkbox cb) {
	if (cb == epDetailOff) {
	    epDetailToggle = false;
	    textArea.setText(ct.getCallTableText(epDetailToggle, statsToggle));
	}
	else if (cb == epDetailOn) {
	    epDetailToggle = true;
            textArea.setText(ct.getCallTableText(epDetailToggle, statsToggle));
	}
	else if (cb == statsOff) {
	    statsToggle = false;
	    textArea.setText(ct.getCallTableText(epDetailToggle, statsToggle));
	}
	else if (cb == statsOn) {
	    statsToggle = true;
	    textArea.setText(ct.getCallTableText(epDetailToggle, statsToggle));
	}
    }


}
