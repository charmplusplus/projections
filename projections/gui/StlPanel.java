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

public class StlPanel extends ScalePanel.Child
{
	private int[][] data;//The utilization data, percent by processor and interval
	private int intervalSize;//Length of an interval, in microseconds
	private int nPe;//Number of processors
	private long totalTime;//Length of run, in microseconds
	private ColorModel colorMap;
	//Return a string describing the given panel location
	public String getPointInfo(double time,double procs)
	{
		int p=(int)procs;
		if (p<0 || p>=nPe || procs<0) return "";
		long t=(long)time;
		if (t<0 || t>=totalTime) return "";
		int utiliz=data[p][(int)(t/intervalSize)];
		return "Processor "+p+": Usage = "+utiliz+"%"+
			" at "+U.t(t)+" ("+t+" us). ";
	}
	//Draw yourself into (0,0,w,h) in the given graphics,
	// scaling your output coordinates via the given axes.
	public void paint(RepaintRequest req) 
	{
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
		
		//Determine mapping from x pixels to time bins
		double x2t_off=req.xInv(startx)/intervalSize;
		double x2t_slope=pix2time/intervalSize;
		
		if (wid>0 && ht>0) 
		{//Write data to offscreen buffer
			byte[] offBuf=new byte[wid*ht];
			int proc_min=(int)Math.floor(pl), proc_max=(int)Math.ceil(ph);
			for (int p=proc_min;p<proc_max;p++) {
				int y_min=(int)Math.floor(req.y(p));
				int y_max=(int)Math.floor(req.y(p+1));
				if (y_min<starty) y_min=starty;
				if (y_max>endy) y_max=endy;
				renderRow(data[p],x2t_off,x2t_slope,offBuf,wid,
					y_min-starty,y_max-starty,0,endx-startx);
			}
			
			Image offImg = createImage(new MemoryImageSource(wid,ht,
					colorMap,offBuf,0,wid));
			req.g.drawImage(offImg, startx, starty, null);
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
		byte[] dest,int w,
		int yLo,int yHi,int xLo,int xHi)
	{
		int iOff=(int)(65536*off);//To fixed-point
		int iSlope=(int)(65536*slope);//To fixed-point
		int iCur=iOff+xLo*iSlope;
		for (int x=xLo;x<xHi;x++) {
			//Simple approach: just take point sample of source
			//OLD: byte val=(byte)src[(int)(off+x*slope)];
			byte val=(byte)src[iCur>>16];
			iCur+=iSlope;
			int loc=yLo*w+x;
			for (int y=yLo;y<yHi;y++,loc+=w)
				dest[loc]=val; //OLD: dest[y*w+x]=val;
		}
	}
	public void setColorMap(ColorModel cm) {
		colorMap=cm;
		repaint();
	}
	public void setData(int desiredIntervals) {
		totalTime=Analysis.getTotalTime();
		intervalSize=(int)(totalTime/desiredIntervals);
		Analysis.LoadGraphData(desiredIntervals,intervalSize,false);
		data=Analysis.getSystemUsageData(1);
		nPe=data.length;
		repaint();
	}
}