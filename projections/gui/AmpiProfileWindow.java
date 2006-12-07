package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.sql.Time;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

//import org.apache.xpath.operations.And;

import projections.analysis.*;
import projections.gui.graph.*;
import projections.misc.*;

public class AmpiProfileWindow extends ProjectionsWindow
    implements ActionListener, ColorSelectable, ChangeListener
{
    private static final int NUM_SYS_EPS = 3;

    private AmpiProfileWindow thisWindow;

    private AmpiProfileData data;
    private boolean colorsSet;
    private Color[] colors; //every color corresponds to a function

    //related with data model of ProfileGraph
    private float[][] dataSource = null;
    private int[][] colorMap = null;
    private String[][] nameMap = null;
    private String [] procNames = null;
    private float [][] accTime = null; // [pe][funcID]

    //temporary data for every single processor
    private float[] sDataSrc = null;
    private int[] sColorMap = null;
    private String[] sNameMap = null;

    //Following varibles are related with responding to user events
    private JTabbedPane tabPane;
    private ProfileGraph displayCanvas;
    private JScrollPane displayPanel;
    private int displayPanelTabIndex;
    private ProfileGraph ampiDisplayCanvas;
    private JScrollPane ampiDisplayPanel;
    private int ampiDisplayPanelTabIndex;


    private JButton btnIncX, btnDecX, btnResX, btnIncY, btnDecY, btnResY;
    private JFloatTextField txtScaleX, txtScaleY;

    //usage greater than "thresh" will be displayed!
    private float thresh;

    private PieChartWindow pieChartWindow;
    private float[] avgData; // [numFunc+1(other)]

    private EntrySelectionDialog entryDialog;

    private boolean ampiTraceOn = false;

    public AmpiProfileWindow(MainWindow parentWindow, Integer myWindowID){
        super(parentWindow, myWindowID);
	thisWindow = this;
        colorsSet = false;
        colors = null;

        thresh = 0.01f;

        if(Analysis.getNumFunctionEvents() > 0)
            ampiTraceOn = true;

	setBackground(Color.lightGray);
	setTitle("Projections AMPI Usage Profile - " + Analysis.getFilename() + ".sts");
	CreateMenus();
	CreateLayout();
	pack();
	showDialog();
    }

    void windowInit() {
	// get new data object
	data = new AmpiProfileData(this);

	// acquire starting data from Analysis
	data.plist = Analysis.getValidProcessorList();
	data.pstring = Analysis.getValidProcessorString();
	data.begintime = 0;
	data.endtime = Analysis.getTotalTime();
    }

    private void CreateMenus(){
	JMenuBar mbar = new JMenuBar();
	mbar.add(Util.makeJMenu("File", new Object[]
                                {
                                    "Select Processors",
                                    "Print Profile",
                                    null,
                                    "Close"
                                },
                                null, this));
        if(ampiTraceOn){
	    /*
            mbar.add(Util.makeJMenu("Tools", new Object[]
                                {
                                    "Pie Chart",
                                    "Change Colors",
                                    "Usage Table",
                                    new String[] {"AMPI", "AMPI Usage Profile"}
                                },
                                null, this));
        } else{
	    */
            mbar.add(Util.makeJMenu("Tools", new Object[]
                                {
				    // "Pie Chart",
                                    "Change Colors",
                                    "Usage Table" 
                                },
                                null, this));
        }

	mbar.add(Util.makeJMenu("Help", new Object[]
                                {
                                    "Index",
                                    "About"
                                },
                                null, this));
	setJMenuBar(mbar);
    }

    private void CreateLayout(){

        JPanel wholePanel = new JPanel();

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;

        //create display canvas
        displayCanvas = new ProfileGraph();

        // this encapsulating panel is required to apply BoxLayout
	// to the scroll pane so that it works correctly.
	//JPanel p = new JPanel();
	//p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
	displayPanel = new JScrollPane(displayCanvas);
	//p.add(displayPanel);
	
        if(ampiTraceOn){
            ampiDisplayCanvas = new ProfileGraph();
            //JPanel ampiP = new JPanel();
            //ampiP.setLayout(new BoxLayout(ampiP, BoxLayout.X_AXIS));
	    ampiDisplayPanel = new JScrollPane(ampiDisplayCanvas);
	    //ampiP.add(ampiDisplayPanel);

            tabPane = new JTabbedPane();
            tabPane.add("Per Processor",displayPanel);
            tabPane.add("Per Function", ampiDisplayPanel);
            displayPanelTabIndex = tabPane.indexOfComponent(displayPanel);
            ampiDisplayPanelTabIndex = tabPane.indexOfComponent(ampiDisplayPanel);
            tabPane.addChangeListener(this);
        }

        //create x-y scale panel
        JPanel xScalePanel = new JPanel();
	xScalePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "x-scale"));
        xScalePanel.setLayout(gbl);

        btnDecX = new JButton("<<");
	JLabel lScaleX  = new JLabel("X-Axis Scale: ", SwingConstants.CENTER);
        txtScaleX = new JFloatTextField(1, 5);
        btnIncX = new JButton(">>");
        btnResX = new JButton("Reset");
        btnDecX.addActionListener(this);
        txtScaleX.addActionListener(this);
        btnIncX.addActionListener(this);
        btnResX.addActionListener(this);

        Util.gblAdd(xScalePanel, btnDecX,  gbc, 0,0, 1,1, 0,0);
	Util.gblAdd(xScalePanel, lScaleX,     gbc, 1,0, 1,1, 0,0);
	Util.gblAdd(xScalePanel, txtScaleX, gbc, 2,0, 1,1, 1,0);
	Util.gblAdd(xScalePanel, btnIncX,  gbc, 3,0, 1,1, 0,0);
	Util.gblAdd(xScalePanel, btnResX,     gbc, 4,0, 1,1, 0,0);

        JPanel yScalePanel = new JPanel();
        yScalePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "y-scale"));
        yScalePanel.setLayout(gbl);

        btnDecY = new JButton("<<");
        JLabel lScaleY  = new JLabel("Y-Axis Scale: ", SwingConstants.CENTER);
        txtScaleY = new JFloatTextField(1, 5);
        btnIncY = new JButton(">>");
        btnResY = new JButton("Reset");
        btnDecY.addActionListener(this);
        txtScaleY.addActionListener(this);
        btnIncY.addActionListener(this);
        btnResY.addActionListener(this);

        Util.gblAdd(yScalePanel, btnDecY,  gbc, 0,0, 1,1, 0,0);
        Util.gblAdd(yScalePanel, lScaleY,     gbc, 1,0, 1,1, 0,0);
        Util.gblAdd(yScalePanel, txtScaleY, gbc, 2,0, 1,1, 1,0);
        Util.gblAdd(yScalePanel, btnIncY,  gbc, 3,0, 1,1, 0,0);
        Util.gblAdd(yScalePanel, btnResY,     gbc, 4,0, 1,1, 0,0);

        //add dispalyPanel and x-y scale panel to the window
        Container wholeContainer = getContentPane();
        wholeContainer.setLayout(gbl);
        if(ampiTraceOn){
            Util.gblAdd(wholeContainer, tabPane, gbc, 0,0, 2,1, 1,1, 5,5,5,5);
        } else {
            Util.gblAdd(wholeContainer, displayPanel, gbc, 0,0, 2,1, 1,1, 5,5,5,5);
        }

        Util.gblAdd(wholeContainer, xScalePanel, gbc, 0,1, 1,1, 1,0, 2,2,2,2);
        Util.gblAdd(wholeContainer, yScalePanel, gbc, 1,1, 1,1, 1,0, 2,2,2,2);
    }

    public void showDialog(){
	if (dialog == null) {
	    if ((!Analysis.hasLogData()) && (!Analysis.hasSumDetailData())) {
		dialog = new RangeDialog(this, "AMPI Usage Profile", true);
	    } else {
		dialog = new RangeDialog(this, "AMPI Usage Profile");
	    }
	} else {
	    setDialogData();
	}

	dialog.displayDialog();
        if (!dialog.isCancelled()) {
	    getDialogData();
	    final Thread t = new Thread() {
                    public void run() {
			readAmpiUsageData();
                        //setDisplayProfileData();
                        if (ampiTraceOn) {
                            setDisplayProfileData();
			    setAmpiDisplayProfileData();
			}
			setLocationRelativeTo(parentWindow);
                        setVisible(true);
                    }
		};
	    t.start();
	}
    }

    public void getDialogData() {
	data.plist = dialog.getValidProcessors();
	data.pstring = dialog.getValidProcessorString();
	data.begintime = dialog.getStartTime();
	data.endtime = dialog.getEndTime();
    }

    public void setDialogData() {
	dialog.setValidProcessors(data.plist);
	dialog.setStartTime(data.begintime);
	dialog.setEndTime(data.endtime);
	super.setDialogData();
    }

    public void showWindow() {
	// do nothing for now
    }


    public void actionPerformed(ActionEvent evt){
        // get recorded values
	float oldScaleX = txtScaleX.getValue();
	float oldScaleY = txtScaleY.getValue();
	// clean current slate
	float scaleX = 0;
	float scaleY = 0;

	if (evt.getSource() instanceof JButton) {
	    JButton b = (JButton) evt.getSource();
 	    if (b == btnDecX) {
		scaleX = (float)((int)(oldScaleX * 4)-1)/4;
		if (scaleX < 1.0)
		    scaleX = (float)1.0;
	    } else if (b == btnIncX) {
		scaleX = (float)((int)(oldScaleX * 4)+1)/4;
	    } else if (b == btnResX) {
		scaleX = (float)1.0;
	    } else if (b == btnDecY) {
		scaleY = (float)((int)(oldScaleY * 4)-1)/4;
		if (scaleY < 1.0)
		    scaleY = (float)1.0;
	    } else if (b == btnIncY) {
		scaleY = (float)((int)(oldScaleY * 4)+1)/4;
	    } else if (b == btnResY) {
		scaleY = (float)1.0;
	    }
	    // minimum value is 1.0, this is used to test if
	    // the which flag was set.
	    if ((scaleX != oldScaleX) && (scaleX > 0.0)) {
		txtScaleX.setText("" + scaleX);
                if(ampiTraceOn){
                    if(tabPane.getSelectedIndex() == displayPanelTabIndex){
                        displayCanvas.setScaleX((double)scaleX);
                    } else if (tabPane.getSelectedIndex() == ampiDisplayPanelTabIndex) {
                        ampiDisplayCanvas.setScaleX((double)scaleX);
                    }
                } else {
                    displayCanvas.setScaleX((double)scaleX);
                }
	    }
	    if ((scaleY != oldScaleY) && (scaleY > 0.0)) {
		txtScaleY.setText("" + scaleY);
                if(ampiTraceOn){
                    if(tabPane.getSelectedIndex() == displayPanelTabIndex){
                        displayCanvas.setScaleY((double)scaleY);
                    } else if (tabPane.getSelectedIndex() == ampiDisplayPanelTabIndex) {
                        ampiDisplayCanvas.setScaleY((double)scaleY);
                    }
                } else {
                    displayCanvas.setScaleY((double)scaleY);
                }
	    }
	} else if (evt.getSource() instanceof JFloatTextField) {
	    JFloatTextField field = (JFloatTextField)evt.getSource();
	    // we really won't know if the value has changed or not,
	    // hence the conservative approach.
	    if (field == txtScaleX) {
		scaleX = oldScaleX;
		if(ampiTraceOn){
                    if(tabPane.getSelectedIndex() == displayPanelTabIndex){
                        displayCanvas.setScaleX((double)scaleX);
                    } else if (tabPane.getSelectedIndex() == ampiDisplayPanelTabIndex) {
                        ampiDisplayCanvas.setScaleX((double)scaleX);
                    }
                } else {
                    displayCanvas.setScaleX((double)scaleX);
                }
	    } else if (field == txtScaleY) {
		scaleY = oldScaleY;
		if(ampiTraceOn){
                    if(tabPane.getSelectedIndex() == displayPanelTabIndex){
                        displayCanvas.setScaleY((double)scaleY);
                    } else if (tabPane.getSelectedIndex() == ampiDisplayPanelTabIndex) {
                        ampiDisplayCanvas.setScaleY((double)scaleY);
                    }
                } else {
                    displayCanvas.setScaleY((double)scaleY);
                }
	    }
	} else if(evt.getSource() instanceof JMenuItem) {
            String arg = ((JMenuItem)evt.getSource()).getText();
	    if (arg.equals("Close")) {
		close();
	    } else if(arg.equals("Select Processors")) {
		showDialog();
		/*	    } else if(arg.equals("Pie Chart")){
                pieChartWindow =
		    new PieChartWindow(parentWindow, avgData,
				       avgData.length, thresh, colors);
		*/
            } else if(arg.equals("Change Colors")) {
		showChangeColorDialog();
            } else if (arg.equals("Usage Table")){
                showUsageTable();
            } else if (arg.equals("Usage Profile")) {
                showAMPIUsageProfile();
            }
        }

    }

    public void stateChanged(ChangeEvent e){
        if(e.getSource() == tabPane){
            if(tabPane.getSelectedIndex() == displayPanelTabIndex){
                txtScaleX.setText(displayCanvas.getScaleX()+"");
                txtScaleY.setText(displayCanvas.getScaleY()+"");
            } else if(tabPane.getSelectedIndex() == ampiDisplayPanelTabIndex){
                txtScaleX.setText(ampiDisplayCanvas.getScaleX()+"");
                txtScaleY.setText(ampiDisplayCanvas.getScaleY()+"");
            }
        }
    }


    public void applyDialogColors() {
	int numFunc = Analysis.getNumFunctionEvents();

        System.out.println(colors[numFunc]);
        displayCanvas.setDisplayDataSource(dataSource, colorMap, colors, nameMap);
        displayCanvas.repaint();
    }

    public void showChangeColorDialog() {
	int numFunc = Analysis.getNumFunctionEvents();
        if (entryDialog == null) {
            String typeLabelStrings[] = {"Functions"};

            boolean existsArray[][] =
                new boolean[1][numFunc+1];
            for (int i=1; i<numFunc+1; i++) {
                existsArray[0][i] = true;
            }

            boolean stateArray[][] =
                new boolean[1][numFunc+1];
            for (int i=1; i<numFunc; i++) {
                stateArray[0][i] = true;
            }

            String entryNames[] =
                new String[numFunc+1];
            for (int i=1; i<numFunc; i++) {
                entryNames[i] =
                    Analysis.getFunctionName(i);
            }
            entryNames[numFunc] = "OTHER";

            /**
             * Reason why I need create a 2D new color array:
             * 1. EntrySelectionDialog's constructor needs a 2D color array (I don't know the reason
             * as I haven't read the source code for this class)
             * 2. The 1D length is set to 1 because of in the original ProfileWindow version, this is
             * set to 1. The reason is not stated in the original version. And I haven't figure out the
             * exact reason for this. I really doubt it is for compatibility of the EntrySelectionDialog
             * class or other old classes.
             */
            Color[][] newColors = new Color[1][];
            newColors[0] = colors;

            entryDialog = new EntrySelectionDialog(this, this, typeLabelStrings, stateArray, newColors,existsArray, entryNames);
	}
        entryDialog.showDialog();
    }

    private void showUsageTable(){
        if(dataSource==null) return;

        JFrame usageFrame = new JFrame();
        usageFrame.setTitle("Entry Points Usage Percent Table");
        usageFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        String[] tHeading ={"Proc#","Entry Name","Usage Percent (%)"};
        int totalEntry=0;
        //skip column "0" as it is for average usage
        for(int i=1; i<dataSource.length; i++)
            totalEntry += dataSource[i].length;
        Object[][] tData = new Object[totalEntry][];
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(3);
        //fill up tData
        int entryCnt=0;
        for(int i=1; i<dataSource.length; i++){
            for(int j=0; j<dataSource[i].length; j++){
                tData[entryCnt] = new Object[3];
                tData[entryCnt][0] = procNames[i];
                tData[entryCnt][1] = nameMap[i][j];
                tData[entryCnt][2] = df.format(dataSource[i][j])+"%";
                entryCnt++;
            }
        }

        JTable t = new JTable(tData, tHeading);
        //t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane sp = new JScrollPane(t);

        /*
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(sp);*/
        usageFrame.getContentPane().add(sp);

        usageFrame.setLocationRelativeTo(parentWindow);
        usageFrame.setSize(500,250);
        //usageFrame.pack();
        usageFrame.setVisible(true);
    }

    private void showAMPIUsageProfile(){
        /* Console version
        int curPe = -1;
        data.plist.reset();
        Vector ampiProcessVec = new Vector();
        while(data.plist.hasMoreElements()){
            curPe = data.plist.nextElement();
            ampiProcessVec.clear();
            Analysis.createAMPIUsage(curPe,data.begintime,data.endtime,ampiProcessVec);
            System.out.println("Processor: "+curPe+":: ampiProcess#"+ampiProcessVec.size());
            for(int i=0; i<ampiProcessVec.size(); i++){
                AmpiProcessProfile p = (AmpiProcessProfile)ampiProcessVec.get(i);
                System.out.println("Processing total execution time: "+p.getAccExecTime());
                Stack stk = p.getFinalCallFuncStack();
                for(Enumeration e=stk.elements(); e.hasMoreElements();){
                    AmpiFunctionData d = (AmpiFunctionData)(e.nextElement());
                    System.out.println(d+"::"+Analysis.getFunctionName(d.FunctionID));
                }
            }
        }*/

        JFrame profileFrame = new JFrame();
        profileFrame.setTitle("AMPI Function Profile Table");
        profileFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        String[] tHeading ={"Processor#","Function Name","Source File Name","Line#","%/Total","%/Process"};

        /**
         * Fisrt get whole data for displaying. It is inefficient considering the case when there are huge
         * content to display. The better way is to displaying the data on the table at the same time analyzing
         * the data. This could be later implemented!
         */
        int curPe = -1;
        data.plist.reset();
        Vector[] ampiProcessVec = new Vector[data.plist.size()];
        int pCnt=0;
        int totalLine=0;
        while(data.plist.hasMoreElements()){
            curPe = data.plist.nextElement();
            ampiProcessVec[pCnt] = new Vector();
            Analysis.createAMPIUsage(curPe,data.begintime,data.endtime,ampiProcessVec[pCnt]);
            Vector v = ampiProcessVec[pCnt];
            for(int i=0; i<v.size(); i++){
                AmpiProcessProfile p = (AmpiProcessProfile)v.get(i);
                totalLine += p.getFinalCallFuncStack().size();
            }
            pCnt++;
        }

        //formating data to display
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(3);
        long totalExecTime = data.endtime - data.begintime;
        Object[][] tData = new Object[totalLine][];
        curPe = -1;
        data.plist.reset();
        pCnt=0;
        int lineCnt=0;
        while(data.plist.hasMoreElements()){
            curPe = data.plist.nextElement();
            Vector v = ampiProcessVec[pCnt++];
            for(int i=0; i<v.size(); i++){
                AmpiProcessProfile p = (AmpiProcessProfile)v.get(i);
                long processTotalExecTime = p.getAccExecTime();
                Stack stk = p.getFinalCallFuncStack();
                for(Enumeration e=stk.elements(); e.hasMoreElements();){
                    AmpiFunctionData d = (AmpiFunctionData)(e.nextElement());
                    tData[lineCnt] = new Object[tHeading.length];
                    tData[lineCnt][0] = ""+curPe;
                    tData[lineCnt][1] = Analysis.getFunctionName(d.FunctionID);
                    tData[lineCnt][2] = d.sourceFileName;
                    tData[lineCnt][3] = ""+d.LineNo;
                    tData[lineCnt][4] = df.format(d.getAccExecTime()/(double)totalExecTime*100)+"%";
                    tData[lineCnt][5] = df.format(d.getAccExecTime()/(double)processTotalExecTime*100)+"%";
                    lineCnt++;
                }
            }
        }

        JTable t = new JTable(tData, tHeading);
        //t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane sp = new JScrollPane(t);

        profileFrame.getContentPane().add(sp);

        profileFrame.setLocationRelativeTo(this);
        profileFrame.setSize(500,250);
        profileFrame.setVisible(true);
   }

    private void readAmpiUsageData(){
	int numPes = data.plist.size();
	accTime = new float[numPes][]; //[numFunc+1] and we need [1..numFunc-1]
	int progressCount = 0;
	int curPe = 0;
	UsageCalc u = new UsageCalc();
        ProgressMonitor progressBar =
	    new ProgressMonitor(this,
				"Computing Usage Values",
				"", 0, data.numPs);
	data.plist.reset();
	while (data.plist.hasMoreElements()) {
	    curPe = data.plist.nextElement();
	    if (!progressBar.isCanceled()) {
                progressBar.setNote("[PE: " + curPe + " ] Computing Average.");
		progressBar.setProgress(progressCount);
	    } else {
		break;
	    }
	    accTime[progressCount] = u.ampiUsage(curPe, data.begintime, data.endtime,
					 Analysis.getVersion());
	    progressCount++;
	}
	progressBar.close();
    }

    private void setAmpiDisplayProfileData(){
        int curPe = -1;
	int numPes = data.plist.size();
	int numFunc = Analysis.getNumFunctionEvents();
        String[] xNames = new String[numFunc-1];
        Vector ampiProcess = null;
        int pCnt=0;

	// [numFunc-1][1]
        float[][] ampiDataSrc = new float[numFunc-1][];
        String[][] ampiFuncNameMap = new String[numFunc-1][];
	double avgScale=1.0/numPes;

        long totalExecTime = data.endtime - data.begintime;
	long totalExecTimeAll = totalExecTime * data.numPs;

	// first compute summed usage across processors
	for(int i=1;i<numFunc;i++){
	    ampiDataSrc[i-1] = new float[1];
	    for(int j=0;j<numPes;j++){
		ampiDataSrc[i-1][0] += accTime[j][i]*avgScale;
	    }
	    xNames[i-1] = Analysis.getFunctionName(i);
	    ampiFuncNameMap[i-1] = new String[1];
	    ampiFuncNameMap[i-1][0] = xNames[i-1];
	}

        //secondly, create the color map (using the easiest color mapping creation)
        int colorNum = numFunc-1;
        Color[] ampiFuncColors = ColorManager.createColorMap(colorNum);
        int[][] ampiFuncColorMap = new int [colorNum][];
        for(int i=0; i<colorNum; i++){
            ampiFuncColorMap[i] = new int[1];
	    ampiFuncColorMap[i][0] = i;
        }

        //set ampi's profile graph parameters!
        String[] gTitles = new String[2];
        gTitles[0] = "Profile of Usage per Functions in AMPI programs "+data.pstring;
        gTitles[1] = "(Time "+data.begintime/(float)1000+" ~ "+data.endtime/(float)1000+" ms)";
        ampiDisplayCanvas.setGraphTiltes(gTitles);

        ampiDisplayCanvas.setXAxis("","",xNames);
        ampiDisplayCanvas.setYAxis("Usage Percent % (over processor)");
        ampiDisplayCanvas.setDisplayDataSource(ampiDataSrc, ampiFuncColorMap, ampiFuncColors, ampiFuncNameMap);
        ampiDisplayCanvas.repaint();
    }

    private void setDisplayProfileData(){
        createDisplayDataSource();

        //testing the data sources
        /*
         for(int x=0; x<dataSource.length; x++){
            System.out.println("Processor "+x);
            for(int y=0; y<dataSource[x].length; y++){
                System.out.println("#"+y+nameMap[x][y]+": "+dataSource[x][y]+"%");
            }
            System.out.println();
        }*/


        String[] gTitles = new String[2];
        gTitles[0] = "Profile of Usage for Processors "+data.pstring;
        gTitles[1] = "(Time "+data.begintime/(float)1000+" ~ "+data.endtime/(float)1000+" ms)";
        displayCanvas.setGraphTiltes(gTitles);

        String[] xNames = new String[data.plist.size()+1];
        xNames[0] = "Avg";
        data.plist.reset();
        int cnt=1;
        while(data.plist.hasMoreElements()){
            xNames[cnt++] = ""+data.plist.nextElement();
        }

        procNames = xNames; //store this in order for the usage of usage table

        displayCanvas.setXAxis("","",xNames);
        displayCanvas.setYAxis("Usage Percent %");
        displayCanvas.setDisplayDataSource(dataSource, colorMap, colors, nameMap);
        displayCanvas.repaint();
    }

    private void createDisplayDataSource(){
        // extra column is that of the average data.
        int procCnt = data.plist.size();
	data.numPs = procCnt;
	int numFunc = Analysis.getNumFunctionEvents();

        dataSource = new float[procCnt+1][];
        colorMap = new int[procCnt+1][];
        nameMap = new String[procCnt+1][];
	long totalExecTime = data.endtime - data.begintime;

	avgData = new float[numFunc+1];
        for (int i =0;i<numFunc+1;i++) {
	    avgData[i] = 0.0f;
	}

	double avgScale=1.0/data.plist.size();

	int progressCount = 0;
        int curPe = -1;
        ProgressMonitor progressBar =
	    new ProgressMonitor(this,
				"Computing Usage Values",
				"", 0, data.numPs);

	// Profile really should be cleanly rewritten.
	// split the original loop:
	// Phase 1a - compute average work
	// Phase 1b - assign colors based on average work
	// Phase 2 - create display data sources

	// Phase 1a: compute average work
	progressCount = 0;
	//System.out.println("bound ("+accTime.length+","+accTime[0].length+") and will go ("+procCnt+","+avgData.length+")");
	for(int j=0;j<procCnt;j++){
	    for (int i=0;i<avgData.length;i++) {
		avgData[i]+=(float)(accTime[j][i]*avgScale);
	    }
	}

	// Phase1b: Assigning colors based on the average usage}
	if (!colorsSet) {
	    Color[] funcColors = ColorManager.createColorMap(numFunc);
            colors = new Color[numFunc+1];
            for(int i=1; i<numFunc; i++)
                colors[i] = funcColors[i];
            colors[numFunc] = Color.white;  // OTHER
            colorsSet = true;
	}

	// Phase 2: create display data source
        //first create average one
        createSingleProcSource(avgData,-1);
        dataSource[0] = sDataSrc;
        colorMap[0] = sColorMap;
        nameMap[0] = sNameMap;

        progressCount = 0;
	data.plist.reset();
	while (data.plist.hasMoreElements()) {
	    curPe = data.plist.nextElement();
	    if (!progressBar.isCanceled()) {
		progressBar.setNote("[PE: " + curPe +
				    " ] Reading Entry Point Usage.");
		progressBar.setProgress(progressCount);
	    } else {
		break;
	    }
	    createSingleProcSource(accTime[progressCount], curPe);

            //The 0 column is left for the average one
            progressCount++;
            dataSource[progressCount] = sDataSrc;
            colorMap[progressCount] = sColorMap;
            nameMap[progressCount] = sNameMap;
	}
	progressBar.close();
    }

    public void createSingleProcSource(float[] rawData, int procNum){
        //fisrt compute number of significant sections
	int numFunc = Analysis.getNumFunctionEvents()-1;
        float[] dSrc = new float[numFunc+1];
        int[] cMap = new int[numFunc+1];
        String[] nMap = new String[numFunc+1];

        sDataSrc = dSrc;
        sColorMap = cMap;
        sNameMap = nMap;

        DecimalFormat format_ = new DecimalFormat();
	format_.setMaximumFractionDigits(5);
	format_.setMinimumFractionDigits(5);

        int funcIdx;
        float usage;
        String[] funcNames = Analysis.getFunctionNames();
        for(funcIdx=0; funcIdx<numFunc+1; funcIdx++){
            usage = rawData[funcIdx+1];
            if(usage<=0) continue;
            dSrc[funcIdx] = usage;
            cMap[funcIdx] = funcIdx+1;
            if(funcIdx==numFunc){
                nMap[funcIdx] = "OTHER";
            }else{
                nMap[funcIdx] = Analysis.getFunctionName(funcIdx+1);
            }
        }
    }

    public void MakePOArray(long bt, long et)
    {
    }

    private void setScales()
    {
    }

    private void setSizes()
    {
    }

    public int getHSBValue()
    {
        return 0;
    }

    public int getVSBValue()
    {
        return 0;
    }
}
