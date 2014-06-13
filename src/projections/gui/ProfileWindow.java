package projections.gui;

import java.io.*;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ProgressMonitor;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import projections.analysis.AmpiFunctionData;
import projections.analysis.AmpiProcessProfile;
import projections.gui.ChooseEntriesWindow;

class ProfileWindow extends ProjectionsWindow
    implements ActionListener, ChangeListener, ColorUpdateNotifier
{

	private static final int NUM_SYS_EPS = 3;

    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    private int myRun = 0;

    private ProfileData data;
    private boolean colorsSet;
    private Color[] colors; //every color corresponds to an entry point

    //related with data model of ProfileGraph
    private float[][] dataSource = null;
    private int[][] colorMap = null;
    private String[][] nameMap = null;
    private String [] procNames = null;
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


    private JButton btnIncX, btnDecX, btnResX, btnIncY, btnDecY, btnResY, btnExportToFile;
    private JFloatTextField txtScaleX, txtScaleY;

    //usage greater than "thresh" will be displayed!
    private float thresh;

//    public PieChartWindow pieChartWindow;
    private float[][] avgData;

//    private EntrySelectionDialog entryDialog;

    private boolean ampiTraceOn = false;

    public ProfileWindow(MainWindow parentWindow){
        super(parentWindow);

        colorsSet = false;
        colors = null;

        thresh = 0.01f;

        if(MainWindow.runObject[myRun].getNumFunctionEvents() > 0)
            ampiTraceOn = true;

	setBackground(Color.lightGray);
	setTitle("Projections Usage Profile - " + MainWindow.runObject[myRun].getFilename() + ".sts");
	CreateMenus();
	CreateLayout();
	pack();
	
	// get new data object
	data = new ProfileData();

	showDialog();
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
                                this));
        if(ampiTraceOn){
            mbar.add(Util.makeJMenu("Tools", new Object[]
                                {
                                    "Pie Chart",
                                    "Change Colors",
                                    "Usage Table",
                                    new String[] {"AMPI", "Usage Profile"}
                                },
                                this));
        } else{
            mbar.add(Util.makeJMenu("Tools", new Object[]
                                {
                                    "Pie Chart",
                                    "Change Colors",
                                    "Usage Table"
                                },
                                this));
        }

	setJMenuBar(mbar);
    }

    private void CreateLayout(){

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
            tabPane.add("Entry Points",displayPanel);
            tabPane.add("AMPI Functions", ampiDisplayPanel);
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
    		if ((!MainWindow.runObject[myRun].hasLogData()) && (!MainWindow.runObject[myRun].hasSumDetailData())) {
    			dialog = new RangeDialog(this, "Usage Profile", null, true);
    		} else {
    			dialog = new RangeDialog(this, "Usage Profile", null, false);
    		}
    	} 

    	dialog.displayDialog();
    	if (!dialog.isCancelled()) {
    		data.plist = dialog.getSelectedProcessors();
    		data.begintime = dialog.getStartTime();
    		data.endtime = dialog.getEndTime();
    		final Thread t = new Thread() {
    			public void run() {
    				setDisplayProfileData();
    				if (ampiTraceOn) {
    					setAmpiDisplayProfileData();
    				}
    				setLocationRelativeTo(parentWindow);
    				setVisible(true);
    			}
    		};
    		t.start();
    	}
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
	    } else if (b == btnExportToFile) {
         try{
             // Create file 
             DecimalFormat df = new DecimalFormat();
             df.setMaximumFractionDigits(3);
             double timerange = (data.endtime - data.begintime)*0.001; //ms
             FileWriter fstream = new FileWriter("usagetable.txt");
             BufferedWriter out = new BufferedWriter(fstream);
             out.write("Proc#,\t"+ "Entry Name,\t" + "Usage Percent (%),\t" + "Usage Time(ms)\n");
             for(int i=1; i<dataSource.length; i++){
                 for(int j=0; j<dataSource[i].length; j++){
                     out.write(procNames[i] + ",\t" + nameMap[i][j] + ",\t" + df.format(dataSource[i][j])+"%,\t" + df.format(dataSource[i][j]*0.01*timerange)+"\n"); 
                 }
             }
             out.close();
             System.out.println("Usage table is saved to file usagetable.txt");
         }catch (Exception e){//Catch exception if any
             System.err.println("Error: " + e.getMessage());
         } 
        }
	    // minimum value is 1.0, this is used to test if
	    // the which flag was set.
	    if ((scaleX != oldScaleX) && (scaleX > 0.0)) {
		txtScaleX.setText("" + scaleX);
                if(ampiTraceOn){
                    if(tabPane.getSelectedIndex() == displayPanelTabIndex){
                        displayCanvas.setScaleX(scaleX);
                    } else if (tabPane.getSelectedIndex() == ampiDisplayPanelTabIndex) {
                        ampiDisplayCanvas.setScaleX(scaleX);
                    }
                } else {
                    displayCanvas.setScaleX(scaleX);
                }
	    }
	    if ((scaleY != oldScaleY) && (scaleY > 0.0)) {
		txtScaleY.setText("" + scaleY);
                if(ampiTraceOn){
                    if(tabPane.getSelectedIndex() == displayPanelTabIndex){
                        displayCanvas.setScaleY(scaleY);
                    } else if (tabPane.getSelectedIndex() == ampiDisplayPanelTabIndex) {
                        ampiDisplayCanvas.setScaleY(scaleY);
                    }
                } else {
                    displayCanvas.setScaleY(scaleY);
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
                        displayCanvas.setScaleX(scaleX);
                    } else if (tabPane.getSelectedIndex() == ampiDisplayPanelTabIndex) {
                        ampiDisplayCanvas.setScaleX(scaleX);
                    }
                } else {
                    displayCanvas.setScaleX(scaleX);
                }
	    } else if (field == txtScaleY) {
		scaleY = oldScaleY;
		if(ampiTraceOn){
                    if(tabPane.getSelectedIndex() == displayPanelTabIndex){
                        displayCanvas.setScaleY(scaleY);
                    } else if (tabPane.getSelectedIndex() == ampiDisplayPanelTabIndex) {
                        ampiDisplayCanvas.setScaleY(scaleY);
                    }
                } else {
                    displayCanvas.setScaleY(scaleY);
                }
	    }
	} else if(evt.getSource() instanceof JMenuItem) {
            String arg = ((JMenuItem)evt.getSource()).getText();
	    if (arg.equals("Close")) {
		close();
	    } else if(arg.equals("Select Processors")) {
	    	showDialog();
	    } else if(arg.equals("Pie Chart")){
	    	new PieChartWindow(avgData[0], avgData[0].length, thresh, colors);
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



    private void showChangeColorDialog() {
		new ChooseEntriesWindow(this);
    }

	public void colorsHaveChanged() {
		colorsSet = false;
		setDisplayProfileData();
		if (ampiTraceOn)
		{
    			setAmpiDisplayProfileData();
    		}
	}

    private void showUsageTable(){
        if(dataSource==null) return;

        JFrame usageFrame = new JFrame();
        usageFrame.setTitle("Entry Points Usage Percent Table");
        usageFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        String[] tHeading ={"Proc#","Entry Name","Usage Percent (%)", "Usage Time(ms)"};
        int totalEntry=0;
        //skip column "0" as it is for average usage
        for(int i=1; i<dataSource.length; i++)
            totalEntry += dataSource[i].length;
        Object[][] tData = new Object[totalEntry][];
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(3);
        //fill up tData
        int entryCnt=0;
        double timerange = (data.endtime - data.begintime)*0.001; //ms
        for(int i=1; i<dataSource.length; i++){
            for(int j=0; j<dataSource[i].length; j++){
                tData[entryCnt] = new Object[4];
                tData[entryCnt][0] = procNames[i];
                tData[entryCnt][1] = nameMap[i][j];
                tData[entryCnt][2] = df.format(dataSource[i][j])+"%";
                tData[entryCnt][3] = df.format(dataSource[i][j]*0.01*timerange);
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
        btnExportToFile = new JButton("Export to file");
        btnExportToFile.addActionListener(this); 
        usageFrame.getContentPane().add(BorderLayout.NORTH, btnExportToFile);

        usageFrame.getContentPane().add(BorderLayout.CENTER, sp);

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
            MainWindow.runObject[myRun].createAMPIUsage(curPe,data.begintime,data.endtime,ampiProcessVec);
            System.out.println("Processor: "+curPe+":: ampiProcess#"+ampiProcessVec.size());
            for(int i=0; i<ampiProcessVec.size(); i++){
                AmpiProcessProfile p = (AmpiProcessProfile)ampiProcessVec.get(i);
                System.out.println("Processing total execution time: "+p.getAccExecTime());
                Stack stk = p.getFinalCallFuncStack();
                for(Enumeration e=stk.elements(); e.hasMoreElements();){
                    AmpiFunctionData d = (AmpiFunctionData)(e.nextElement());
                    System.out.println(d+"::"+MainWindow.runObject[myRun].getFunctionName(d.FunctionID));
                }
            }
        }*/

        JFrame profileFrame = new JFrame();
        profileFrame.setTitle("AMPI Function Profile Table");
        profileFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        String[] tHeading ={"Processor#","Function Name","Source File Name","Line#","%/Total","%/Process"};

        /**
         * First get whole data for displaying. It is inefficient considering the case when there are huge
         * content to display. The better way is to displaying the data on the table at the same time analyzing
         * the data. This could be later implemented!
         */
        Vector[] ampiProcessVec = new Vector[data.plist.size()];
        int pCnt=0;
        int totalLine=0;
        
        for(Integer pe : data.plist) {
        	ampiProcessVec[pCnt] = new Vector();
        	MainWindow.runObject[myRun].createAMPIUsage(pe,data.begintime,data.endtime,ampiProcessVec[pCnt]);
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
      
        pCnt=0;
        int lineCnt=0;

        for(Integer pe : data.plist) {
        	Vector v = ampiProcessVec[pCnt++];
            for(int i=0; i<v.size(); i++){
                AmpiProcessProfile p = (AmpiProcessProfile)v.get(i);
                long processTotalExecTime = p.getAccExecTime();
                Stack stk = p.getFinalCallFuncStack();
                for(Enumeration e=stk.elements(); e.hasMoreElements();){
                    AmpiFunctionData d = (AmpiFunctionData)(e.nextElement());
                    tData[lineCnt] = new Object[tHeading.length];
                    tData[lineCnt][0] = ""+pe;
                    tData[lineCnt][1] = MainWindow.runObject[myRun].getFunctionName(d.FunctionID);
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

    private void setAmpiDisplayProfileData(){

        String[] xNames = new String[data.plist.size()];

        Vector ampiProcess = null;
        int pCnt=0;

        float[][] ampiDataSrc = new float[data.plist.size()][];
        String[][] ampiFuncNameMap = new String[data.plist.size()][];

        long totalExecTime = data.endtime - data.begintime;

        //firstly, create the usage percent and every sections' name
        for(Integer pe : data.plist) {
            
            ampiProcess = new Vector();
            MainWindow.runObject[myRun].createAMPIUsage(pe,data.begintime,data.endtime,ampiProcess);
            int total = 0;
            for(int i=0; i<ampiProcess.size(); i++){
                AmpiProcessProfile p = (AmpiProcessProfile)ampiProcess.get(i);
                total += p.getFinalCallFuncStack().size();
            }

            ampiDataSrc[pCnt] = new float[total];
            ampiFuncNameMap[pCnt] = new String[total];
            total = 0;
            for(int i=0; i<ampiProcess.size(); i++){
                AmpiProcessProfile p = (AmpiProcessProfile)ampiProcess.get(i);
                Stack stk = p.getFinalCallFuncStack();
                for(Enumeration e=stk.elements(); e.hasMoreElements();){
                    AmpiFunctionData d = (AmpiFunctionData)(e.nextElement());
                    ampiFuncNameMap[pCnt][total] = MainWindow.runObject[myRun].getFunctionName(d.FunctionID)+"@"+
                        d.sourceFileName+"("+d.LineNo+")";
                    ampiDataSrc[pCnt][total] = d.getAccExecTime()/(float)totalExecTime*100;
                    total++;
                }
            }
            xNames[pCnt] = pe+"";
            pCnt++;
        }

        //secondly, create the color map (using the easiest color mapping creation)
        int colorNum = 0;
        for(int i=0; i<ampiDataSrc.length; i++)
            colorNum += ampiDataSrc[i].length;

        Color[] ampiFuncColors = ColorManager.createColorMap(colorNum);
        int[][] ampiFuncColorMap = new int [ampiDataSrc.length][];
        colorNum = 0;
        for(int i=0; i<ampiDataSrc.length; i++){
            ampiFuncColorMap[i] = new int[ampiDataSrc[i].length];
            for(int j=0; j<ampiDataSrc[i].length; j++)
                ampiFuncColorMap[i][j] = colorNum++;
        }


        //set ampi's profile graph parameters!

        String[] gTitles = new String[2];
        gTitles[0] = "Profile of Usage for Functions in AMPI programs "+data.plist.listToString();
        gTitles[1] = "(Time "+data.begintime/(float)1000+" ~ "+data.endtime/(float)1000+" ms)";
        ampiDisplayCanvas.setGraphTiltes(gTitles);

        ampiDisplayCanvas.setXAxis("",xNames);
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
        gTitles[0] = "Profile of Usage for Processors "+data.plist.listToString();
        gTitles[1] = "(Time "+data.begintime/(float)1000+" ~ "+data.endtime/(float)1000+" ms)";
        displayCanvas.setGraphTiltes(gTitles);

        String[] xNames = new String[data.plist.size()+1];
        xNames[0] = "Avg";
        int cnt=1;
        for(Integer pe : data.plist) {
        	xNames[cnt++] = ""+pe;
        }

        procNames = xNames; //store this in order for the usage of usage table

        displayCanvas.setXAxis("",xNames);
        displayCanvas.setYAxis("Usage Percent %");
        displayCanvas.setDisplayDataSource(dataSource, colorMap, colors, nameMap);
        displayCanvas.repaint();
    }

    private void createDisplayDataSource(){
        // extra column is that of the average data.
        int procCnt = data.plist.size()+1;
	    data.numPs = procCnt;

        dataSource = new float[procCnt][];
        colorMap = new int[procCnt][];
        nameMap = new String[procCnt][];

        int numEPs = MainWindow.runObject[myRun].getNumUserEntries();
        // the first row is for entry method execution time the second is for
        // time spent sending messages in that entry method
        float[][] avg=new float[2][numEPs+NUM_SYS_EPS];
        for (int i =0;i<numEPs+NUM_SYS_EPS;i++) {
            avg[0][i] = 0.0f;
            avg[1][i] = 0.0f;
        }

        avgData = avg; //set instance's avg data

        double avgScale=1.0/data.plist.size();


        int progressCount = 0;
        ProgressMonitor progressBar;

        // Profile really should be cleanly rewritten.
        // // split the original loop:
        // // Phase 1a - compute average work
        // // Phase 1b - assign colors based on average work
        // // Phase 2 - create display data sources


        // Phase 1a: compute average work
        progressBar =
	    new ProgressMonitor(this,
				"Computing Usage Values",
				"", 0, data.numPs);
        
        for(Integer pe : data.plist){
        	if (!progressBar.isCanceled()) {
        		progressBar.setNote("[PE: " + pe + " ] Computing Average.");
        		progressBar.setProgress(progressCount);
        	} else {
        		break;
        	}

        	// the first row is for entry method execution time
        	// the second is for time spent sending messages in
        	// that entry method
        	float cur[][] =
        		MainWindow.runObject[myRun].GetUsageData(pe,data.begintime,data.endtime,data.phaselist);
        	for (int i=0;i<avg[0].length && i<cur[0].length;i++) {
        		avg[0][i]+=(float)(cur[0][i]*avgScale);
        		avg[1][i]+=(float)(cur[1][i]*avgScale);
        	}
        	progressCount++;
        }

	// Phase1b: Assigning colors based on the average usage
        Vector sigElements = new Vector();
	// we only wish to compute for EPs
	for (int i=0; i<numEPs; i++) {
	    // anything greater than 5% is "significant"
	    if (avg[0][i]+avg[1][i] > 1.0) {
		sigElements.add(new Integer(i));
	    }
	}
	// copy to an array for Color assignment (maybe that should be
	// a new interface for color assignment).
	int sigIndices[] = new int[sigElements.size()];
	for (int i=0; i<sigIndices.length; i++) {
	    sigIndices[i] = ((Integer)sigElements.elementAt(i)).intValue();
	}
	if (!colorsSet) {
            //also create colors for "PACKING, UNPACKING, IDLE"
	    Color[] entryColors = MainWindow.runObject[myRun].getEPColorMap();
            colors = new Color[numEPs+NUM_SYS_EPS];
            for(int i=0; i<numEPs; i++)
                colors[i] = entryColors[i];
            colors[numEPs] = Color.black; //PACKING
            colors[numEPs+1] = Color.orange; //UNPACKING
	    Paint p = MainWindow.runObject[myRun].getIdleColor(); //IDLE
	    if (p instanceof GradientPaint) colors[numEPs+2] = ((GradientPaint)p).getColor1();
	    else colors[numEPs+2] = (Color)p;
            colorsSet = true;
	}

	// Phase 2: create display data source
        //first create average one
        createSingleProcSource(avg,-1);
        dataSource[0] = sDataSrc;
        colorMap[0] = sColorMap;
        nameMap[0] = sNameMap;

        progressCount = 0;

        for(Integer pe : data.plist) {

        	if (!progressBar.isCanceled()) {
        		progressBar.setNote("[PE: " + pe + " ] Reading Entry Point Usage.");
        		progressBar.setProgress(progressCount);
        	} else {
        		break;
        	}
        	float rawData[][]=MainWindow.runObject[myRun].GetUsageData(pe,data.begintime,data.endtime,data.phaselist);

        	createSingleProcSource(rawData, pe);

        	//The 0 column is left for the average one
        	progressCount++;
        	dataSource[progressCount] = sDataSrc;
        	colorMap[progressCount] = sColorMap;
        	nameMap[progressCount] = sNameMap;

        }
        progressBar.close();
    }

    private void createSingleProcSource(float[][] rawData, int procNum){
        //fisrt compute number of significant sections
        int numSigSections = 0;
        for(int i=0; i<rawData[0].length; i++){
            if(rawData[0][i]>thresh)
                numSigSections++;
            if(rawData[1][i]>thresh)
                numSigSections++;
        }
        float[] dSrc = new float[numSigSections];
        int[] cMap = new int[numSigSections];
        String[] nMap = new String[numSigSections];

        sDataSrc = dSrc;
        sColorMap = cMap;
        sNameMap = nMap;

        DecimalFormat format_ = new DecimalFormat();
        format_.setMaximumFractionDigits(5);
        format_.setMinimumFractionDigits(5);

        int sigCnt=-1;
        int epIndex;
        float usage;
        int numUserEntries = MainWindow.runObject[myRun].getNumUserEntries();

        for(epIndex=0; epIndex<rawData[0].length; epIndex++){
            usage = rawData[0][epIndex];
            if(usage<=thresh) continue;
            sigCnt++;
            dSrc[sigCnt] = usage;
            cMap[sigCnt] = epIndex;
            if(epIndex==numUserEntries){
                nMap[sigCnt] = "PACKING";
            } else if(epIndex==numUserEntries+1) {
                nMap[sigCnt] = "UNPACKING";
            } else if(epIndex==numUserEntries+2) {
                nMap[sigCnt] = "IDLE";
            } else {
                nMap[sigCnt] = MainWindow.runObject[myRun].getEntryFullNameByIndex(epIndex);
            }

            //!!!!we need to give a table to show the exact usage of every non-tiny entry!!!!
            //This is especially important for CPAIMD!!! Here we ignore
            if ((procNum >= 0) && MainWindow.PRINT_USAGE) 
            {
                System.out.println(procNum + " " + epIndex + " " +
                        format_.format(usage) +
                        " " + nMap[sigCnt]);
            }
        }

	if (MainWindow.runObject[myRun].getVersion() > 4.9) {
            //Computing the entry point message sendTime
            String prefix = "Message Send Time: ";
            for(epIndex=0; epIndex<rawData[1].length; epIndex++){
                usage = rawData[1][epIndex];
                if(usage<=thresh) continue;
                sigCnt++;
                dSrc[sigCnt] = usage;
                cMap[sigCnt] = epIndex;
                if(epIndex==numUserEntries){
                    nMap[sigCnt] = prefix+"PACKING";
                } else if(epIndex==numUserEntries+1) {
                    nMap[sigCnt] = prefix+"UNPACKING";
                } else if(epIndex==numUserEntries+2) {
                    nMap[sigCnt] = prefix+"IDLE";
                } else {
                    nMap[sigCnt] = prefix+MainWindow.runObject[myRun].getEntryFullNameByIndex(epIndex);
                }

                //!!!!we need to give a table to show the exact usage of every non-tiny entry!!!!
                //This is especially important for CPAIMD!!! Here we ignore
                if ((procNum >= 0) && MainWindow.PRINT_USAGE) {
    		 System.out.println(procNum + " " + epIndex + " " +
    				       format_.format(usage) +
    				       " " + nMap[sigCnt]);
                }
            }

        }
    }

}
