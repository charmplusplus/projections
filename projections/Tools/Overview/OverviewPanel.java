package projections.Tools.Overview;

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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;

import projections.Tools.Timeline.ImageFilter;
import projections.analysis.LogReader;
import projections.analysis.ThreadManager;
import projections.gui.ColorMap;
import projections.gui.JPanelToImage;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.ScalePanel;
import projections.gui.U;

public class OverviewPanel extends ScalePanel.Child
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	int[][] entryData;   // [pe][interval]

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

	private int nPe;//Number of processors
	private OrderedIntList selectedPEs;
	private long startTime,endTime;
	int startInterval;
	int endInterval;
	int numEPs;

	boolean saveImage;
	
	private ColorMap colorMap;

	private int mode;

	boolean newEPData = true;

	OverviewWindow parentWindow = null;

	public OverviewPanel(OverviewWindow parentWindow) {
		this.parentWindow = parentWindow;
	}

	//Return a string describing the given panel location
	public String getPointInfo(double time,double procs)
	{
		int p=(int)procs;
		if (p<0 || p>=nPe || procs<0)
			return "";
		long t=(long)time;
		if (t<0 || t>=totalTime())
			return "";
		if (selectedPEs != null ) {
			int count=0;
			int pe=0;
			selectedPEs.reset();
			while (count <= p) {
				pe = selectedPEs.nextElement();
				count++;
			}
			//			int numEP = MainWindow.runObject[myRun].getNumUserEntries();
			int interval = (int)(t/intervalSize);

			long  timedisplay = t+startTime;
			if (mode == OverviewWindow.MODE_UTILIZATION) {
				return "Processor " + pe + 
				": Usage = " + utilData[p][interval]+"%" +
				" IDLE = " + idleData[p][interval]+"%" +
				" at "+U.humanReadableString(timedisplay)+" ("+timedisplay+" us). ";
			} else if(mode == OverviewWindow.MODE_EP) {
				//				if (interval >= entryData[p].length) {
				//					return "some bug has occurred"; // strange bug.
				//				}
				//				if (entryData[p][interval] > 0) {
				//					return "Processor "+pe+": Usage = "+
				//					utilData[p][interval]+"%"+
				//					" at "+U.humanReadableString(timedisplay)+" ("+timedisplay+" us)." +
				//					" EP = " + 
				//					MainWindow.runObject[myRun].getEntryNameByIndex(entryData[p][interval]);
				//				} else {
				//					return "Processor "+pe+": Usage = "+
				//					utilData[p][interval]+"%"+
				//					" at "+U.humanReadableString(timedisplay)+" ("+timedisplay+" us). ";
				return "No implementation of getPointInfo()";
			}
		}
		return "";
	}

	// Draw yourself into (0,0,w,h) in the given graphics,
	// scaling your output coordinates via the given axes.
	public void paint(RepaintRequest req) {
//		double proc2pix=req.y(1)-req.y(0);//Pixels per processor
		double time2pix=req.x(1)-req.x(0);//Pixels per microsecond
		double pix2time=1.0/time2pix;

		//Figure out what portion of time and processors are visible
		double tl=req.xInv(0),th=req.xInv(req.w-1);//Time edges
		double pl=req.yInv(0),ph=req.yInv(req.h-1);//Procesor edges
		//Clip time to present data
		if (tl<0) tl=0; if (tl>=totalTime()) tl=totalTime()-0.001;
		if (th<0) th=0; if (th>=totalTime()) th=totalTime()-0.001;
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

		if(selectedPEs != null){
			if (wid>0 && ht>0) 
			{//Write data to offscreen buffer
				int[] offBuf=new int[wid*ht];
//				int proc_min=(int)Math.floor(pl);
//				int proc_max=(int)Math.ceil(ph);
				selectedPEs.reset();
				int proc;
				int p =0;
				while((proc = selectedPEs.nextElement()) != -1){
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

	void applyColorMap(int [][]data, boolean entryBased) {
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
			System.out.println("Final data is of size " + numPE + " by " + numIntervals);
			colors = new int[numPE][numIntervals];
			for (int pe=0;pe<numPE;pe++) {
				for (int interval=0; interval<numIntervals; interval++) {
					if (data[pe][interval] == numEPs){
						// overhead
						colors[pe][interval] = Color.black.getRGB();
					} else if (data[pe][interval] == numEPs+1){
						// idle
						colors[pe][interval] = Color.white.getRGB();
					} else if (data[pe][interval] > 0) {
						// normal EP
						colors[pe][interval] =
							MainWindow.runObject[myRun].getEntryColor(data[pe][interval]).getRGB();
					} else {
						colors[pe][interval] =
							Color.black.getRGB();
					}
				}
			}
		}
				
		// If desired, save the full image to a file
		if(saveImage){
			int numPE = colors.length;
			int numIntervals = colors[numPE-1].length;
			BufferedImage image = new BufferedImage(numIntervals, numPE, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			for(int y=0;y<numPE;y++) {
				for(int x=0;x<numIntervals;x++) {
					Color c = new Color(colors[y][x]);
					g.setColor(c);
					g.fillRect(x, y, 1,1);
				}
			}
			g.dispose();
			JPanelToImage.saveToFileChooserSelection(image, "Save Overview to PNG or JPG",  "Overview.png");
		}		
	}

	private long totalTime() {
		return endTime - startTime;
	}

	/** Setup the time ranges before loading the EP or utilization data */
	public void setRanges(OrderedIntList selectedPEs, long startTime, long endTime)
	{
		this.selectedPEs = selectedPEs;
		this.startTime = startTime;
		this.endTime = endTime;

		int desiredIntervals;

		// necessary for t < 7000
		if (totalTime() < 7000) {
			desiredIntervals = (int )(totalTime() - 1.0);
		} else {
			desiredIntervals = 7000;
		}

		double trialintervalSize = (totalTime()/desiredIntervals);
		if (trialintervalSize < 5 ){
			desiredIntervals = desiredIntervals/(5);
			trialintervalSize = (totalTime()/desiredIntervals);
		}

		intervalSize = (int )trialintervalSize;
		startInterval = (int)(startTime/intervalSize);
		endInterval = (int)(endTime/intervalSize);

		nPe=selectedPEs.size();
		numEPs = MainWindow.runObject[myRun].getNumUserEntries();

	}

	/** Load utilization using Analysis.LoadGraphData()  
	 * @param b */
	public void loadUtilizationData(boolean b){
		mode = OverviewWindow.MODE_UTILIZATION;
		selectedPEs.reset();
		this.saveImage = saveImage;

		// Load the graph data that we need:
		MainWindow.runObject[myRun].LoadGraphData(intervalSize,
				startInterval, endInterval, false, selectedPEs);
		// Get the arrays that were just loaded
		utilData = MainWindow.runObject[myRun].getSystemUsageData(LogReader.SYS_CPU);
		idleData = MainWindow.runObject[myRun].getSystemUsageData(LogReader.SYS_IDLE);


		// **CW** Silly hack because MainWindow.runObject[myRun].getSystemUsageData returns
		// null when LogReader.SYS_IDLE is not available. Create an
		// empty array - do a proper fix if this becomes a memory issue.
		if (idleData == null) {
			idleData = new int[utilData.length][utilData[0].length];
		}
		mergedData =  new int[utilData.length][utilData[0].length];

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
		selectedPEs.reset();
		repaint();
	}

	/** Load utilization using Analysis.LoadGraphData() and a second manual scan through the logs. 
	 * @param saveImage 
	 */
	public void loadEPData(boolean saveImage) {
		if (!MainWindow.runObject[myRun].hasLogData()) {
			System.err.println("No log files are available.");
			return;
		}

		this.saveImage = saveImage;
		
		mode = OverviewWindow.MODE_EP;

		// Create a list of worker threads
		LinkedList<Thread> readyReaders = new LinkedList<Thread>();

		// Create an array that will store the resulting data
		int special = 2;
		int numPEs = selectedPEs.size();

		int numIntervals = endInterval - startInterval;
		entryData = new int[selectedPEs.size()][numIntervals];

		int pIdx=0;		
		while (selectedPEs.hasMoreElements()) {
			int nextPe = selectedPEs.nextElement();
			readyReaders.add( new ThreadedFileReader(nextPe, pIdx, intervalSize, myRun, 
					startInterval, endInterval, false, entryData[pIdx]) );
			pIdx++;
		}

		
		// Pass this list of threads to a class that manages/runs the threads nicely
		ThreadManager threadManager = new ThreadManager("Loading Overview in Parallel", readyReaders, this, true);
		threadManager.runThreads();

		applyColorMap(entryData, true);
		repaint();
	}
}
