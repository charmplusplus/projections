package projections.gui;

/**
 * Attaches to two ScaleSliders to control pan and zoom
 * for a child pane.  Displays outline of child, zoom
 * scale tick marks, and controls child redraw.
 *
 * Orion Sky Lawlor, 2/10/2001
 */

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

public class ScalePanel extends Panel
   implements MouseMotionListener, MouseListener
{
	/* This is the panel type we pan, zoom, and display.
	 * Several panels may share a child.
	 */
	public static class Child extends Component {
		/*Describes a single redraw*/
		public static final class RepaintRequest {
			public Graphics g;//Destination port
			public int w,h;//Size of graphics port above
			private ScalePanel.Axis hor,ver;//Scaling factors
			
			public RepaintRequest(Graphics Ng,int Nw,int Nh,
				ScalePanel.Axis Nhor,ScalePanel.Axis Nver)
			{
				g=Ng; w=Nw;h=Nh;
				hor=Nhor; ver=Nver;
			}
			//Map panel coordinates to screen coordinates
			public double x(double pc) {return hor.p2s(pc);}
			public double y(double pc) {return ver.p2s(pc);}
			//Map screen coordinates to panel coordinates
			public double xInv(double sc) {return hor.s2p(sc);}
			public double yInv(double sc) {return ver.s2p(sc);}
		}
		//Draw yourself into (0,0,w,h) in the given graphics,
		// scaling your output coordinates via the given axes.
		public void paint(RepaintRequest r)
		{//Default: flat black background
			r.g.setColor(Color.black); //Erase old image
			r.g.fillRect(0,0,r.w,r.h);
		}
		//Return a string describing the given panel location
		public String getPointInfo(double x,double y)
		{
			return "At ("+x+","+y+").";
		}
		//Add this parent to our list
		private Vector parents=new Vector();
		public void addParent(ScalePanel p) {
			parents.insertElementAt(p,parents.size());
		}
		//We get redrawn by requesting all our parents to redraw
		public void repaint() {
			int i;
			for (i=0;i<parents.size();i++)
				((ScalePanel)parents.elementAt(i)).repaint();
		}
	};
	/* An axis maps screen coordinates to panel coordinates.
	 * It maintains its direction's zoom slider and the scaling 
	 * factors.
	 */
	public class Axis implements ScaleSlider.ValueListener {
		private String direction;//"Horizontal" or "Vertical", for status reports
		private double screen,screenInv;//Length of screen
		private double p2scale,p2off;//Panel->norm. screen conversion
		private double s2scale,s2off;//norm. screen->panel conversion
		private double zoomCenter;//Norm. screen point-- zoom about this point
		
		//Map panel to screen coordinates
		public double p2s(double p) {return screen*(p2scale*p+p2off);}
		//Map screen to panel coordinates
		public double s2p(double s) {return s2scale*(s*screenInv)+s2off;}
		
		public Axis(ScaleSlider sl,String myDir)
		{
			sl.setValueListener(this);
			direction=myDir;
			screen=screenInv=1.0;
			p2off=s2off=0.0;
			p2scale=s2scale=1.0;
			zoomCenter=0.5;
		}
		public void setScreenSize(int Ns) {
			screen=Ns; screenInv=1.0/screen;
		}
		
		/*Adjust the panel->screen scaling so that 
		 * p panel units map to the screen size.
		 * Ignores the origin.
		 */
		public void setScale(double p) 
		{
			p2scale=1.0/p;
			s2scale=p;
			zoomCenter=0.5;
		}
		
		/*Adjust the panel->screen zoom (p2scale) to nz, 
		 * leaving the normalized screen point fix stationary.  
		 * I.e.,
		 *   s2scale'=1.0/nz;
		 *   fix*s2scale'+s2off' == s2scale*fix+s2off
		 */
		public void setZoom(double nz,double fix)
		{
			p2scale=nz;
			s2off=s2scale*fix+s2off-fix/nz;
			s2scale=1.0/nz;
			p2off=-s2off/s2scale;
		}
		/*Scroll the screen image by delta pixels--
		 *  i.e., the panel point screen point 0 used to map to 
		 *   is now mapped to by screen point delta.
		 *  Scale factors are unchanged.
		 */
		public void scroll(double delta)
		{
			double dn=delta*screenInv;//Normalize shift
			s2off-=dn*s2scale;
			p2off=-s2off/s2scale;
		}
		//Show the current zoom status
		public void zoomStatus(String middle)
		{
			status(direction+" zoom is "+middle+p2scale);
		}
		
		//Adjust the zoom slider
		public void sliderChanged(ScaleSlider src) {
			double newZoom=Math.pow(10.0,src.getValue());
			setZoom(newZoom,zoomCenter);
			zoomStatus("now ");
			repaint();
		}
		
		//Set/get the zoom center
		public double getZoomCenter() {return zoomCenter*screen;}
		public void setZoomCenter(double zc) {
			zoomCenter=zc*screenInv;
			if (zoomCenter<0.0 || zoomCenter>1.0) zoomCenter=0.5;
		}
		
		/*Get tick marks to illustrate the scale of min-max, screen coords.
		 * The ticks should lie at the nice decimals in panel coords.
		 *Return screen location (pixels)/length (fraction)/label (panel coord) triplets
		 */
		public float[] getTicks(double smin,double smax)
		{
			double per=7.0;//Minimum number of ticks per axis
			
			double pmin=s2p(smin), pmax=s2p(smax);
			double scale=1.0;  //original=cur/scale
			double plen=pmax-pmin;
			
			scale=Math.pow(10.0,Math.ceil(-Math.log(plen/per)/Math.log(10.0)));
			//while (plen*scale>10*per) scale*=0.1; //<- Equiv. to above line
			//while (plen*scale<per) scale*=10.0;
			
			int start=(int)Math.ceil(pmin*scale);
			int end=(int)Math.floor(pmax*scale);
			float lengthScale=(float)(1-Math.log(plen*scale/per)/Math.log(10.0));
			int nTicks=(int)(end-start+1);
			float ticks[]=new float[3*nTicks];
			for (int t=0;t<nTicks;t++) {
				int i=start+t;
				double label=(start+t)/scale;
				ticks[3*t+0]=(float)(
					smin+(smax-smin)*(label-pmin)/(pmax-pmin)
				);
				ticks[3*t+2]=(float)label;
				float len;
				if (i%10==0) len=1.0f; //Decimal ticks always big
				else if (i%5==0) { //Multiples of 5 grow faster
					len=1.3f*lengthScale;
					if (len>1.0f) len=1.0f;
				} else len=lengthScale; //Others grow slower
				ticks[3*t+1]=len*len;
			}
			return ticks;
		}
	};
	
	private Axis hor,ver;
	private Child child;
	private int lC,rC,tC,bC; //The current child clip region
	
	//************ Mouse actions ********
	public static interface StatusDisplay {
		public void setStatus(String msg);
	};
	private StatusDisplay statusDisplay;
	//Update an ongoing mouse drag
	private int lastX=-1,lastY=-1;
	private boolean inDrag;
	public ScalePanel(ScaleSlider h,ScaleSlider v,Child c)
	{
		child=c;
		child.addParent(this);
		add(child);
		hor=new Axis(h,"Horizontal");
		ver=new Axis(v,"Vertical");
		//Get our own mouse events
		addMouseMotionListener(this);
		addMouseListener(this);
	}
	//************ Redraw: **********
	private void drawZoomCenter(Graphics g)
	{//Indicate the zoom center by a cross
		g.setColor(Color.gray);
		int crossSz=8;
		int x=(int)hor.getZoomCenter(), y=(int)ver.getZoomCenter();
		g.drawLine(x-crossSz,y, x+crossSz,y);
		g.drawLine(x,y-crossSz, x,y+crossSz);
	}
	public Dimension getMinimumSize() {return new Dimension(150,100);}
	public Dimension getPreferredSize() {return new Dimension(500,300);}
	//Print information about the current point to the status line
	private void hoverPoint(MouseEvent evt) {
		int x=evt.getX()-lC;
		int y=evt.getY()-tC;
		status(child.getPointInfo(hor.s2p(x),ver.s2p(y))+" Drag to pan");
	}
	//Return true if point is in main (non-tickmark) area
	private boolean inBounds(int x,int y) {
		return  lC<x && x<=rC && tC<y && y<=bC;
	}
	public boolean isDoubleBuffered() {return true;}
	public void mouseClicked(MouseEvent evt)
	{}
	public void mouseDragged(MouseEvent evt)
	{
		updatePoint(evt,false);
	}
	public void mouseEntered(MouseEvent evt)
	{}
	public void mouseExited(MouseEvent evt)
	{status("");}
	public void mouseMoved(MouseEvent evt)
	{
		if (!inDrag) {
			if (evt.getX()>rC) ver.zoomStatus("");
			else if (evt.getY()>bC) hor.zoomStatus("");
			else hoverPoint(evt);
		}
	}
	public void mousePressed(MouseEvent evt)
	{
		if (inBounds(evt.getX(),evt.getY())) //Ignore clicks on ruler
		{
			inDrag=true;
			status("Release mouse to end pan");
			updatePoint(evt,true);
		}
	}
	public void mouseReleased(MouseEvent evt)
	{
		status("");
		updatePoint(evt,false);
		inDrag=false;
		lastX=-1;lastY=-1;
	}
	public void paint(Graphics g)
	{
		int w=getSize().width, h=getSize().height;
		
		int tickSize=8;//Max. tick size; inset along bot & right edges
		int upSet=2;//Inset along top & left edges
		
		//This is the clip region
		lC=upSet; rC=w-tickSize; tC=upSet; bC=h-tickSize;
		hor.setScreenSize(rC-lC); ver.setScreenSize(bC-tC);
		
		g.setColor(Color.black); //Erase old image
		g.fillRect(0,0,w,tC-1);//Top
		g.fillRect(0,0,lC-1,h);//Left
		g.fillRect(rC+1,0,w,h);//Right
		g.fillRect(0,bC+1,w,h);//Bottom
		
		g.setColor(Color.gray);
		g.drawRect(lC-1,tC-1,rC-lC+1,bC-tC+1);
		
		float []ticks; //Array of (pixel pos, length fraction, label) pairs
		ticks=hor.getTicks(0,w-tickSize-upSet);
		for (int t=0;t<ticks.length/3;t++) { //Horizontal tick marks
			int x=lC+(int)ticks[t*3+0];
			int len=(int)((tickSize-2)*ticks[t*3+1]);
			g.drawLine(x,h-tickSize,x,h-tickSize+len);
		}
		ticks=ver.getTicks(0,h-tickSize-upSet);
		for (int t=0;t<ticks.length/3;t++) { //Vertical tick marks
			int y=tC+(int)ticks[t*3+0];
			int len=(int)((tickSize-2)*ticks[t*3+1]);
			g.drawLine(w-tickSize,y,w-tickSize+len,y);
		}
		
		//Restrict further drawing to child region
		g.translate(lC,tC);
		g.setClip(0,0,rC-lC,bC-tC);
		
		drawZoomCenter(g);
		child.paint(new Child.RepaintRequest(g,rC-lC,bC-tC,hor,ver));
		drawZoomCenter(g);
	}
	//Set the initial sizes
	public void setScales(double hVal,double vVal)
	{
		hor.setScale(hVal);ver.setScale(vVal);
	}
	public void setStatusDisplay(StatusDisplay s) {statusDisplay=s;}
	private void status(String msg) {
		if (statusDisplay!=null) statusDisplay.setStatus(msg);
	}
	//Convert this scale factor into a type suitable for the sliders
	public double toSlider(double from)
	{
		return Math.log(from)/Math.log(10);
	}
	public void update(Graphics g) {paint(g);}
	private void updatePoint(MouseEvent evt,boolean firstTime) {
		int x=evt.getX(),y=evt.getY();
		if ((lastX!=x) || (lastY!=y)) {
			if (inDrag)
			{
				hor.setZoomCenter(x-lC); ver.setZoomCenter(y-tC);
				if(!firstTime) {
					hor.scroll(x-lastX);
					ver.scroll(y-lastY);
				}
				repaint();
			}
		}
		lastX=x;lastY=y;
	}
}