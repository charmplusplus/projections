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

import projections.misc.ProgressDialog;
import projections.analysis.*;

public class StlPanel extends ScalePanel.Child
{
    // dimension 0 - indexed by EP (including the overall average util)
    // dimension 1 - indexed by processor index
    // dimension 2 - indexed by interval index
    private int[][][] data;//incoming util data per processor per interval
    private int[][] colors; //The color per processor per interval
    private int intervalSize;//Length of an interval, in microseconds
    private int nPe;//Number of processors
    private OrderedIntList validPEs;
    private long totalTime;//Length of run, in microseconds
    private long startTime,endTime;

    // default mode
    private int mode = StlWindow.MODE_UTILIZATION;
    
    //Return a string describing the given panel location
    public String getPointInfo(double time,double procs)
    {
	try {
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
		if (mode == StlWindow.MODE_UTILIZATION) {
		    int utiliz=data[0][pe][(int)(t/intervalSize)];
		    long  timedisplay = t+(long)startTime;
		    return "Processor "+pe+": Usage = "+utiliz+"%"+
			" at "+U.t(timedisplay)+" ("+timedisplay+" us). ";
		} else {
		    // **CW** nothing for now.
		    return "";
		}
	    }
	} catch (Exception e) { 
	}
	return "";		
    }

    //Draw yourself into (0,0,w,h) in the given graphics,
    // scaling your output coordinates via the given axes.
    public void paint(RepaintRequest req) {
	double proc2pix=req.y(1)-req.y(0);//Pixels per processor
	double time2pix=req.x(1)-req.x(0);//Pixels per microsecond
	double pix2proc=1.0/proc2pix;
	double pix2time=1.0/time2pix;
	
	//Figure out what portion of time and processors are visible
	double tl=req.xInv(0),th=req.xInv(req.w-1);//Time edges
	double pl=req.yInv(0),ph=req.yInv(req.h-1);//Procesor edges
	if (tl<0) tl=0; //Clip time to present data
	if (th>=totalTime) th=totalTime-0.001;
	if (pl<0) pl=0; //Clip processors to data
	if (ph>=nPe) ph=nPe-0.001;
		
	//Figure out where the data block lies on the screen
	int startx=(int)Math.ceil(req.x(tl)), endx=(int)Math.floor(req.x(th));
	int starty=(int)Math.ceil(req.y(pl)), endy=(int)Math.floor(req.y(ph));
	int wid=endx-startx, ht=endy-starty;
	
	//System.out.println("req.w " + req.w + "th " + th);
	//System.out.println("XRange " + startx + "-" + endx);
	//Determine mapping from x pixels to time bins
	double x2t_off=req.xInv(startx)/intervalSize;
	double x2t_slope=pix2time/intervalSize;
		
	int testwidth = endx-startx;
	// System.out.println("pix2time "+ pix2time);
	// System.out.println("x2t_off = " + x2t_off +" x2t_slope = " + 
	// x2t_slope + "endx - startx " + testwidth + "   IntervalSize " + 
	// intervalSize);
	
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
			    renderRow(colors[proc],x2t_off,x2t_slope,offBuf,
				      wid,y_min-starty,y_max-starty,startx,
				      endx);
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
    
    /*Render this row of data into the given offscreen buffer.
     * We want dest[y*w+x]=src[slope*x+off] for all suitable x and y.
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
		if(srcIndex >= src.length){
		    // System.out.println("Index greater than data points \n");
		    iCur+=iSlope;
		}else{
		    //System.out.println("Number of elements in each row " + src.length);
		    int val=src[srcIndex];
				//	System.out.println("val = " + val);
		    iCur+=iSlope;
		    int loc=yLo*w+x;
		    for (int y=yLo;y<yHi;y++,loc+=w)
			if(loc < dest.length)
			    dest[loc]=val; //OLD: dest[y*w+x]=val;
		}
	    }	
	}
	//System.out.println("Max srcIndex = " + srcIndex);
    }

    private ColorMap colorMap;
    public void setColorMap(ColorMap cm) {colorMap=cm;}

    /**
     * Convert processor usage (0..100) to color values.
     */
    private void applyColorMap(int [][]data) {
	int nPE=data.length;
	colors = new int[nPE][];
	for (int p=0;p<nPE;p++) {
	    int n=data[p].length;
	    colors[p] = new int[n];
	    for (int i=0;i<n;i++) 
		colors[p][i]=colorMap.apply((byte)data[p][i]);
	}
    }

    /**
     *  colors should not have to be re-created here. Will have to re-work
     *  this to make it more efficient some time soon.
     */
    private void applyColorMap(int [][][]data) {
	int numEP = data.length;
	int numPE = data[numEP-1].length; // ignore average util
	colors = new int[numPE][];
	for (int pe=0;pe<numPE;pe++) {
	    int n=data[numEP-1][pe].length;
	    colors[pe] = new int[n];
	    for (int interval=0; interval<n; interval++) {
		// find max ep
		int maxEP = 0;
		int max = 0;
		for (int ep=0; ep<numEP-1; ep++) {
		    if (data[ep][pe][interval] >= max) {
			max = data[ep][pe][interval];
			maxEP = ep;
		    }
		}
		colors[pe][interval]=Analysis.getEntryColor(maxEP).getRGB();
	    }
	}
    }

    public void setMode(int mode) {
	this.mode = mode;
	int numEPs = Analysis.getNumUserEntries();
	if (mode == StlWindow.MODE_UTILIZATION) {
	    applyColorMap(data[numEPs]);
	} else if (mode == StlWindow.MODE_EP) {
	    applyColorMap(data);
	}
	repaint();
    }

    public void setData(int desiredIntervals) {
	totalTime=Analysis.getTotalTime();
	intervalSize=(int)(totalTime/desiredIntervals);
	/*
	  Analysis.LoadGraphData(intervalSize,
	  0, desiredIntervals-1,false,null);
	*/
	// **CW** now, there's the possibility we want entry-point
	// based data.
	Analysis.LoadGraphData(intervalSize,
			       0, desiredIntervals-1,true,null);
	int numEPs = Analysis.getNumUserEntries();
	data = new int[numEPs+1][][];
	/* **CW** we now compute the utilization instead of taking it
	   from analysis.
	*/
	// data=Analysis.getSystemUsageData(1);
	computeUtilizationData();
	if (mode == StlWindow.MODE_UTILIZATION) {
	    applyColorMap(data[numEPs]);
	} else if (mode == StlWindow.MODE_EP) {
	    applyColorMap(data);
	}
	nPe=data[numEPs].length;
	repaint();
    }
    
    public void setData(OrderedIntList validPEs, long startTime, long endTime)
    {
	this.validPEs = validPEs;
	totalTime = endTime - startTime;
	this.startTime = startTime;
	this.endTime = endTime;
	//necessary for t < 7000
	//int desiredIntervals = 7000;
	int desiredIntervals;
	if(totalTime < 7000)
	    desiredIntervals = (int )(totalTime -1.0);
	else	
	    desiredIntervals = 7000;
	
	double trialintervalSize = (totalTime/desiredIntervals);
	if (trialintervalSize < 5 ){
	    desiredIntervals = desiredIntervals/(5);
	    trialintervalSize = (totalTime/desiredIntervals);
	}
	intervalSize = (int )trialintervalSize;
	//System.out.println("desiredIntervals : " + desiredIntervals + " intervalSize " + intervalSize);
	int totalIntervals = (int )Analysis.getTotalTime()/intervalSize;
	int startInterval = (int )startTime/intervalSize;
	int endInterval = (int )endTime/intervalSize;
	/*
	  Analysis.LoadGraphData(intervalSize,
	  startInterval, endInterval,false,validPEs);
	*/
	// **CW** now, there's the possibility we want entry-point
	// based data.
	Analysis.LoadGraphData(intervalSize,
			       startInterval, endInterval,true,validPEs);
	System.out.println(Analysis.hasUserEntryData(0,LogReader.TIME));
	int numEPs = Analysis.getNumUserEntries();
	data = new int[numEPs+1][][];
	computeUtilizationData();
	if (mode == StlWindow.MODE_UTILIZATION) {
	    applyColorMap(data[numEPs]);
	} else if (mode == StlWindow.MODE_EP) {
	    applyColorMap(data);
	}
	nPe=data[numEPs].length;
	validPEs.reset();
	repaint();
    }

    private void computeUtilizationData() {
	int numEPs = data.length-1;

	for (int ep=0; ep<numEPs; ep++) {
	    // wierd, java does not allow me to simply "append" an array
	    // to another.
	    int temp[][] = Analysis.getUserEntryData(ep, LogReader.TIME);
	    int numPEs = temp.length;
	    data[ep] = new int[numPEs][];
	    System.out.println("length = " + numPEs);
	    for (int pe=0; pe<numPEs; pe++) {
		int numIntervals = temp[pe].length;
		data[ep][pe] = new int[numIntervals];
		for (int interval=0; interval<numIntervals; interval++) {
		    // average utilization entry
		    data[ep][pe][interval] = temp[pe][interval];
		    data[numEPs][pe][interval] += data[ep][pe][interval];
		}
	    }
	}
	// there's no need to compute the % utilization as it will be done
	// later when rendering takes place. Wierd place to do it though.
    }
}
