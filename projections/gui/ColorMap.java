package projections.gui;

/*
	Color Map wrapper class-Orion Lawlor, 10/10/98
*/
import java.awt.image.*;

class ColorMap {
	byte red[]=new byte[256],
		green[]=new byte[256],
		blue[]=new byte[256];
	ColorModel cm;
	boolean cm_cached=false;
	public void addBreak(
		int sDex,int sRed,int sGreen,int sBlue,
		int eDex,int eRed,int eGreen,int eBlue)
	{
		int i;
		cm_cached=false;
		eRed-=sRed;
		eGreen-=sGreen;
		eBlue-=sBlue;
		for (i=sDex;i<=eDex;i++)
		{
			float fac=(i-sDex)/(float)(eDex-sDex);
			red[i]=(byte)(sRed+fac*eRed);
			green[i]=(byte)(sGreen+fac*eGreen);
			blue[i]=(byte)(sBlue+fac*eBlue);
		}
	}
	public void addBreaks(int dex[],int r[],int g[],int b[])
	{
		int i;
		for (i=1;i<dex.length;i++)
			addBreak(dex[i-1],r[i-1],g[i-1],b[i-1],
				dex[i],r[i],g[i],b[i]);
	}
	public ColorModel getColorModel()
	{
		if (!cm_cached)
		{
			cm=new IndexColorModel(8,256,red,green,blue,-1);
			cm_cached=true;
		}
		return cm;
	}
	public void initGrey()
	{
		int i;
		cm_cached=false;
		for (i=0;i<256;i++)
			red[i]=green[i]=blue[i]=(byte)(0xff&i);
	}
}