package projections.gui;

/**
 * Represents a continuous range of values, allowing the user to 
 * slowly adjust them.  Similar to java.awt.Scrollbar.
 * Orion Sky Lawlor, olawlor@acm.org, 2/9/2001
 */
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Scrollbar;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class ScaleSlider extends Canvas
   implements MouseMotionListener, MouseListener
{

	private double min,value,max; //Smallest, current, and largest values
	private double tickStart,tickSep;//First and distance between tick marks
	private int orient;
	private ValueListener myListener;
	
	//This is quite like AdjustmentListener
	static interface ValueListener {
		public void sliderChanged(ScaleSlider src);
	}
	
	private int hw=7; //Half width of indicator
	private int totalThickness=16;//Minimum pixels across short side
	
	//Val2 and coor2 are linear equations relating 
	// values to screen coordinates and vice versa.
	private double val2slope,val2offset;
	private double coor2slope,coor2offset;
	private Image offscreen;
	private int off_w=-1,off_h=-1;
	
	public ScaleSlider(int Norient)
	{
		min=0;max=100.0;value=0.0;
		tickStart=0;tickSep=10.0;
		orient=Norient;
		//Ask for our own clicks
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	private final double coor2val(int coor) 
	  { return coor2slope*coor+coor2offset; }
	public Dimension getMinimumSize() 
	  {return new Dimension(totalThickness,totalThickness);}
	public Dimension getPreferredSize() 
	  {return new Dimension(totalThickness,totalThickness);}
	public double getValue() {return value;}
	public boolean isDoubleBuffered() {return true;}
	public void mouseClicked(MouseEvent evt)
	  {}
	public void mouseDragged(MouseEvent evt)
	  {updateValue(evt);}
	public void mouseEntered(MouseEvent evt)
	  {}
	public void mouseExited(MouseEvent evt)
	  {}
	public void mouseMoved(MouseEvent evt)
	  {}
	public void mousePressed(MouseEvent evt)
	  {updateValue(evt);}
	public void mouseReleased(MouseEvent evt)
	  {updateValue(evt);}
	public void paint(Graphics dest_g)
	{
		int w=getSize().width, h=getSize().height;
		if (off_w!=w || off_h!=h ) //Set up a new offscreen buffer
			offscreen=createImage(off_w=w,off_h=h);
		Graphics g=offscreen.getGraphics();
		
		if (orient==Scrollbar.VERTICAL)
			{int tmp=w;w=h;h=tmp;} //Swap so w is big dimension
		setVal2coor(w);
		
		//Erase background (black)
		int back[]={0,0, 0,h,  w,h, w,0};
		poly(g,back,Color.black,true,0,0);
		
		int loS=2; //Start of blue triangle
		int loE=h-4; //End of blue triangle
		
		//Draw blue trapezoid
		int loTri[]={hw,0, hw,loS, w-hw,loE, w-hw,0};
		poly(g,loTri,Color.blue,true,0,0);
		
		//Draw tick marks
		double cur=tickStart;
		int x;
		
		while ((x=val2coor(cur))<=w-hw) {
			int tick[]={x,0, x,h};
			poly(g,tick,Color.black,false,0,0);
			cur+=tickSep;
		}
		
		//Draw indicator pointer
		x=val2coor(value);
		int yh=1,ym=yh+hw,yl=h-3;
		int t[]={x+hw,ym, x+hw,yl, x-hw,yl, x-hw,ym, x,yh};
		poly(g,t,Color.lightGray,true,0,0);//Interior
		int tCr[]={t[8],t[9], t[0],t[1]};
		poly(g,tCr,Color.gray,false,0,0);//Corner
		int tA[]={t[4],t[5], t[6],t[7], t[8],t[9]};
		poly(g,tA,Color.white,false,0,0);//Hilight
		int tB[]={t[0],t[1], t[2],t[3], t[4],t[5]};
		poly(g,tB,Color.darkGray,false,0,0);//Shadow
		
		dest_g.drawImage(offscreen, 0, 0, null);
	}
//**************** drawing utility ********************
	private void poly(Graphics g,int coords[],Color color,boolean withFill,
		int shiftX,int shiftY)
	{
		int xc[]=new int[coords.length/2];
		int yc[]=new int[coords.length/2];
		g.setColor(color);
		int xo=0,yo=1;
		if (orient==Scrollbar.VERTICAL) {xo=1;yo=0;}//Flip for vertical
		for (int i=0;i<xc.length;i++)
		{
			xc[i]=coords[i*2+xo]+shiftX;
			yc[i]=coords[i*2+yo]+shiftY;
		}
		if (withFill)
			g.fillPolygon(xc,yc,xc.length);
		else{
			g.drawPolyline(xc,yc,xc.length);
		}	
	}
	public void setMax(double m) {max=m; repaint();}
//***************** State *********************
	public void setMin(double m) {min=m; repaint();}
	public void setTicks(double start,double delta) 
	{
		tickStart=start; 
		tickSep=delta;
		repaint();
	}
	private void setVal2coor(int screenSize) {
		if((max-min) == 0){
			val2slope=(screenSize-2*hw)/1;
		}else{	
			val2slope=(screenSize-2*hw)/(max-min);
		}	
		val2offset=hw-val2slope*min;
		coor2slope=1.0/val2slope;
		coor2offset=-val2offset/val2slope;
	}
	public void setValue(double v) {value=v; repaint();}
	public void setValueListener(ValueListener dest)
	{
		myListener=dest;
		
	}
	public void update(Graphics g) {paint(g);}
//***************** Interaction *********************
	private void updateValue(MouseEvent evt)
	{
		int coor=evt.getX();
		if (orient==Scrollbar.VERTICAL) coor=evt.getY();
		double newValue=coor2val(coor);
		if (newValue<min) newValue=min;
		if (newValue>max) newValue=max;
		double oldValue=getValue();
		if (newValue!=oldValue) {
			setValue(newValue);
			//Inform listener of the adjustment
			if (myListener!=null)
				myListener.sliderChanged(this);
		}
	}
	private final int val2coor(double val) 
	  { 
		return (int)(val2slope*val+val2offset+0.5); 
	  }
}
