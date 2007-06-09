package projections.gui;

/*
This panel actually does the overview data display--
it gets the processor utilization data from Analyzer,
prints it to an image, and displays the image.

The usage data from Analyzer is a 2D array-- the
first index is processor number, mapped to Y; 
the second is the time bin, mapped to X.  The 
value is the integer percent CPU utilization from 0..100.

Orion Sky Lawlor, olawlor@acm.org, 2/12/2001
*/
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

import projections.analysis.*;

public class StlPanel extends ScalePanel.Child
{
    private int[][] entryData;   // [pe][interval]

    // utilData (for utilization based visualization)
    // dimension 0 - indexed by processor index
    // dimension 1 - indexed by interval index
    private int[][] utilData;

    // idleData & mergedData (for supporting - utilization for now - 
    // the other two data formats)
    private int[][] idleData;
    private int[][] mergedData;

    private int[][] colors; //The color per processor per interval
    private int intervalSize;//Length of an interval, in microseconds

    // **CW** These reflect the last known values. Hence, if the viewing
    // mode is changed, the equivalent data can be re-loaded using the
    // same parameters.
    private int nPe;//Number of processors
    private OrderedIntList validPEs;
    private long totalTime;//Length of run, in microseconds
    private long startTime,endTime;

    // Optimization to prevent the reloading of expensive EP-based data.
    private OrderedIntList oldPEList = new OrderedIntList();
    private long prevStartTime = -1;
    private long prevEndTime = -1;

    private ColorMap colorMap;

    // default mode
    private int mode = StlWindow.MODE_UTILIZATION;

    private boolean newEPData = true;

    private StlWindow parentWindow = null;
    
    public StlPanel(StlWindow parentWindow) {
	this.parentWindow = parentWindow;
    }

    //Return a string describing the given panel location
    public String getPointInfo(double time,double procs)
    {
	int p=(int)procs;
	if (p<0 || p>=nPe || procs<0) 
	    return "";
	long t=(long)time;
	if (t<0 || t>=totalTime) 
	    return "";
	if (validPEs != null ) {
	    int count=0;
	    int pe=0;
	    validPEs.reset();
	    while (count <= p) {
		pe = validPEs.nextElement();
		count++;
	    }
	    int numEP = Analysis.getNumUserEntries();
	    int interval = (int)(t/intervalSize);
	    
	    long  timedisplay = t+startTime;
	    if (mode == StlWindow.MODE_UTILIZATION) {
		return "Processor " + pe + 
		    ": Usage = " + utilData[p][interval]+"%" +
		    " IDLE = " + idleData[p][interval]+"%" +
		    " at "+U.t(timedisplay)+" ("+timedisplay+" us). ";
	    } else {
		if (interval >= entryData[p].length) {
		    return ""; // strange bug.
		}
		if (entryData[p][interval] > 0) {
		    return "Processor "+pe+": Usage = "+
			utilData[p][interval]+"%"+
			" at "+U.t(timedisplay)+" ("+timedisplay+" us)." +
			" EP = " + 
			Analysis.getEntryName(entryData[p][interval]);
		} else {
		    return "Processor "+pe+": Usage = "+
			utilData[p][interval]+"%"+
			" at "+U.t(timedisplay)+" ("+timedisplay+" us). ";
		}
	    }
	}
	return "";
    }

    // Draw yourself into (0,0,w,h) in the given graphics,
    // scaling your output coordinates via the given axes.
    public void paint(RepaintRequest req) {
	double proc2pix=req.y(1)-req.y(0);//Pixels per processor
	double time2pix=req.x(1)-req.x(0);//Pixels per microsecond
	double pix2time=1.0/time2pix;
	
	//Figure out what portion of time and processors are visible
	double tl=req.xInv(0),th=req.xInv(req.w-1);//Time edges
	double pl=req.yInv(0),ph=req.yInv(req.h-1);//Procesor edges
	//Clip time to present data
	if (tl<0) tl=0; if (tl>=totalTime) tl=totalTime-0.001;
	if (th<0) th=0; if (th>=totalTime) th=totalTime-0.001;
	//Clip processors to data
	if (pl<0) pl=0; if (pl>=nPe) pl=nPe-0.001;
	if (ph<0) ph=0; if (ph>=nPe) ph=nPe-0.001;
	
	//Figure out where the data block lies on the screen
	int startx=(int)Math.ceil(req.x(tl)), endx=(int)Math.floor(req.x(th));
	int starty=(int)Math.ceil(req.y(pl)), endy=(int)Math.floor(req.y(ph));
	int wid=endx-startx, ht=endy-starty;
	
	//System.out.println("req.w " + req.w + " time range: " + tl + " - " + th);
	//System.out.println("XRange " + startx + " - " + endx);
	//Determine mapping from x pixels to time bins
	double x2t_off=req.xInv(startx)/intervalSize;
	double x2t_slope=pix2time/intervalSize;
		
	if(validPEs != null){
	    if (wid>0 && ht>0) 
		{//Write data to offscreen buffer
		    int[] offBuf=new int[wid*ht];
		    int proc_min=(int)Math.floor(pl);
		    int proc_max=(int)Math.ceil(ph);
		    validPEs.reset();
		    int proc;
		    int p =0;
		    while((proc = validPEs.nextElement()) != -1){
			if(proc != -1){
			    int y_min=(int)Math.floor(req.y(p));
			    int y_max=(int)Math.floor(req.y(p+1));
			    if (y_min<starty) y_min=starty;
			    if (y_max>endy) y_max=endy;
			    if(p < colors.length){
					 renderRow(colors[p],x2t_off,x2t_slope,offBuf,
					      wid,
					      y_min-starty,y_max-starty,
					      startx-startx,endx-startx);
				 }
			    p++;
			}
		    }
		    Image offImg = 
			createImage(new MemoryImageSource(wid,ht,
							  offBuf,0,wid));
		    req.g.drawImage(offImg, startx, starty, null);
		}
	}
	//Erase beyond image
	req.g.setColor(Color.black);
	req.g.fillRect(0,0,startx,endy);//Left
	req.g.fillRect(startx,0,req.w,starty);//Top
	req.g.fillRect(endx,starty,req.w,endy);//Right
	req.g.fillRect(0,endy,req.w,req.h);//Bottom
    }
    
    /** 
     *  Render this row of data into the given offscreen buffer.
     *  We want dest[y*w+x]=src[slope*x+off] for all suitable x and y.
     */
    private void renderRow(int[] src,double off,double slope,
			   int[] dest,int w,
			   int yLo,int yHi,int xLo,int xHi)
    {
	int iOff=(int)(65536*off);//To fixed-point
	int iSlope=(int)(65536*slope);//To fixed-point
	int iCur=iOff+xLo*iSlope;
	int srcIndex=0;
	for (int x=xLo;x<xHi;x++) {
	    //Simple approach: just take point sample of source
	    //OLD: byte val=(byte)src[(int)(off+x*slope)];
	    srcIndex = iCur>>16;
	    if(srcIndex < 0){
		srcIndex = 0;
	    } else {	
		if (srcIndex >= src.length) {
		    iCur+=iSlope;
		} else {
		    int val=src[srcIndex];
		    iCur+=iSlope;
		    int loc=yLo*w+x;
		    for (int y=yLo;y<yHi;y++,loc+=w)
			if(loc < dest.length)
			    dest[loc]=val; //OLD: dest[y*w+x]=val;
		}
	    }	
	}
    }

    public void setColorMap(ColorMap cm) {
	colorMap=cm;
    }

    private void applyColorMap(int [][]data, boolean entryBased) {
	if (!entryBased) {
	    /**
	     * Convert processor usage (0..100) to color values.
	     * 12/9/04 - and idle "usage" (101..201) to a blue-scale.
	     */
	    int nPE=data.length;
	    colors = new int[nPE][];
	    for (int p=0;p<nPE;p++) {
		int n=data[p].length;
		colors[p] = new int[n];
		for (int i=0;i<n;i++) {
		    // old constraints on byte removed. we only need
		    // to make sure the index is between 0 and 255.
		    // (so the array on the colorMap side will not
		    //  be busted).
		    if (data[p][i] > 255 || data[p][i] < 0) {
			// apply the "wrong" green color.
			colors[p][i]=colorMap.apply(255);
			System.err.println("[" + p + "] Warning: Invalid " +
					   "value " + data[p][i] + " being " +
					   "applied to the color map " +
					   "at time interval " + i + "!");
		    } else {
			colors[p][i]=colorMap.apply(data[p][i]);
		    }
		}
	    }
	} else {
	    int numPE = data.length;
	    int numIntervals = data[numPE-1].length;
	    colors = new int[numPE][numIntervals];
	    for (int pe=0;pe<numPE;pe++) {
		for (int interval=0; interval<numIntervals; interval++) {
		    if (data[pe][interval] > 0) {
			colors[pe][interval] =
			    Analysis.getEntryColor(data[pe][interval]).getRGB();
		    } else {
			colors[pe][interval] =
			    Color.black.getRGB();
		    }
		}
	    }
	}
    }
    
    public void setMode(int mode) {
	this.mode = mode;
	int numEPs = Analysis.getNumUserEntries();
	if (mode == StlWindow.MODE_UTILIZATION) {
	    applyColorMap(mergedData, false);
	    repaint();
	} else if (mode == StlWindow.MODE_EP) {
	    if (newEPData) {
		parentWindow.setVisible(false);
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
			    setEPData();
			    newEPData = false;
			    return null;
			}
			public void finished() {
			    parentWindow.setVisible(true);
			    applyColorMap(entryData, true);
			    repaint();
			}
		    };
		worker.start();
	    } else {
		applyColorMap(entryData, true);
		repaint();
	    }
	}
    }

    public void resetMode() {
	this.mode = StlWindow.MODE_UTILIZATION;
	newEPData = true; // for now. setData will ultimately decide this.
    }

    public void setData(OrderedIntList validPEs, long startTime, long endTime)
    {
	this.validPEs = validPEs;
	totalTime = endTime - startTime;
	this.startTime = startTime;
	this.endTime = endTime;

	// necessary for t < 7000
	// int desiredIntervals = 7000;
	int desiredIntervals;
	if (totalTime < 7000) {
	    desiredIntervals = (int )(totalTime - 1.0);
	} else {
	    desiredIntervals = 7000;
	}

	double trialintervalSize = (totalTime/desiredIntervals);
	if (trialintervalSize < 5 ){
	    desiredIntervals = desiredIntervals/(5);
	    trialintervalSize = (totalTime/desiredIntervals);
	}

	intervalSize = (int )trialintervalSize;
	int startInterval = (int)(startTime/intervalSize);
	int endInterval = (int)(endTime/intervalSize);

	nPe=validPEs.size();
	int numEPs = Analysis.getNumUserEntries();

	// Now that we no longer load 4D data, It becomes important that
	// the data is reloaded each time a setData request is made.
	if ((startTime == prevStartTime) &&
	    (endTime == prevEndTime) &&
	    (oldPEList.equals(validPEs))) {
	    // do nothing. Data already loaded.
	    // set to false, so setEPData does not have to care about this.
	    newEPData = false; 
	    return;
	}
	
	// feeling very unnatural now, but
	// I don't have the time to look at it
	this.prevStartTime = startTime; 
	this.prevEndTime = endTime;   
	// **CW** Another optimization. Don't copy the list if it has not
	// changed.
	if (!oldPEList.equals(validPEs)) {
	    oldPEList = validPEs.copyOf();
	}

	validPEs.reset();
	Analysis.LoadGraphData(intervalSize,
			       startInterval, endInterval, false,
			       validPEs);
	utilData = Analysis.getSystemUsageData(LogReader.SYS_CPU);
	idleData = Analysis.getSystemUsageData(LogReader.SYS_IDLE);
	
	// **CW** Silly hack because Analysis.getSystemUsageData returns
	// null when LogReader.SYS_IDLE is not available. Create an
	// empty array - do a proper fix if this becomes a memory issue.
	if (idleData == null) {
	    idleData = new int[utilData.length][utilData[0].length];
	}
	mergedData = 
	    new int[utilData.length][utilData[0].length];
	// merge the two data into utilData
	for (int i=0; i<utilData.length; i++) {
	    for (int j=0; j<utilData[i].length; j++) {
		// I only wish to see idle data if there is no
		// utilization data.
		if (utilData[i][j] == 0) {
		    // 101 is the starting range for idle color
		    // representation. However, we want non-idle
		    // scenarios to remain "black".
		    if (idleData[i][j] > 0) {
			/* Idle data is broken for some reason
			 * Dis-abling until a fix can be acquired
			 mergedData[i][j] = 101 + idleData[i][j];
			*/
			mergedData[i][j] = 0;
		    } else {
			mergedData[i][j] = 0;
		    }
		} else {
		    mergedData[i][j] = utilData[i][j]; 
		}
	    }
	}
	applyColorMap(mergedData, false);
	validPEs.reset();
	repaint();
    }

    public void setEPData() {
        // necessary for t < 7000
        // int desiredIntervals = 7000;
        int desiredIntervals;
        if (totalTime < 7000) {
            desiredIntervals = (int )(totalTime - 1.0);
        } else {
            desiredIntervals = 7000;
        }

        double trialintervalSize = (totalTime/desiredIntervals);
        if (trialintervalSize < 5 ){
            desiredIntervals = desiredIntervals/(5);
            trialintervalSize = (totalTime/desiredIntervals);
        }

        intervalSize = (int )trialintervalSize;
        int startInterval = (int)(startTime/intervalSize);
        int endInterval = (int)(endTime/intervalSize);

        nPe=validPEs.size();
        int numEPs = Analysis.getNumUserEntries();

	/**
	 *  **CW** This is pretty much a stop-gap measure to reduce
	 *  the unnecessary memory foot print of using EP-based
	 *  visualization for Overview. One of these days ...
	 */
	if (Analysis.hasLogData()) {
	    ProgressMonitor progressBar =
		new ProgressMonitor(Analysis.guiRoot, "Building EP Data",
				    "", 0, validPEs.size());
	    progressBar.setNote("Building EP Data");
	    progressBar.setProgress(0);

	    OrderedIntList curPEList = new OrderedIntList();
	    int[][] temp = new int[validPEs.size()][]; // [pe][interval]
	    int[][] max = 
		new int[validPEs.size()][desiredIntervals];  // [pe][interval]
	    entryData =
		new int[validPEs.size()][desiredIntervals];
	    int curPeIdx = 0;
	    int curPE = 0;
	    validPEs.reset();
	    while (validPEs.hasMoreElements()) {
		curPE = validPEs.nextElement();
		curPEList.insert(curPE);
		progressBar.setProgress(curPeIdx);
		progressBar.setNote("[PE: " + curPE + " ( " +
				    curPeIdx + " of " +
				    validPEs.size()+") ] Acummulating Data");
		Analysis.LoadGraphData(intervalSize,
				       startInterval, endInterval, true,
				       curPEList);
		for (int ep=0; ep<numEPs; ep++) {
		    temp[curPeIdx] = 
			Analysis.getUserEntryData(ep, LogReader.TIME)[0];
		    // find max so far for each valid interval
		    for (int i=0; 
			 (i<temp[curPeIdx].length) && (i<desiredIntervals); 
			 i++) {
			if (temp[curPeIdx][i] > max[curPeIdx][i]) {
			    max[curPeIdx][i] = temp[curPeIdx][i];
			    entryData[curPeIdx][i] = ep;
			}
		    }
		}
		curPEList.removeAll();
		curPeIdx++;
	    }
	    progressBar.close();
	}
    }
}
