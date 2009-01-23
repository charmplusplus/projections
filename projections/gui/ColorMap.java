package projections.gui;

/*
	Color Map wrapper class-Orion Lawlor, 10/10/98
*/
import java.awt.image.*;

class ColorMap {
    int red[]=new int[256],
	green[]=new int[256],
	blue[]=new int[256];
    boolean cm_cached=false;
    
    public void addBreak(int sDex,int sRed,int sGreen,int sBlue,
			 int eDex,int eRed,int eGreen,int eBlue)
    {
	int i;
	cm_cached=false;
	eRed-=sRed;
	eGreen-=sGreen;
	eBlue-=sBlue;
	for (i=sDex;i<=eDex;i++) {
	    float fac=(i-sDex)/(float)(eDex-sDex);
	    red[i]=(int)(sRed+fac*eRed);
	    green[i]=(int)(sGreen+fac*eGreen);
	    blue[i]=(int)(sBlue+fac*eBlue);
	}
    }
    
//    public void addBreaks(int dex[],int r[],int g[],int b[])
//    {
//	int i;
//	for (i=1;i<dex.length;i++)
//	    addBreak(dex[i-1],r[i-1],g[i-1],b[i-1],
//		     dex[i],r[i],g[i],b[i]);
//    }
	
    /**
     * Map this color index into an actual color, suitable
     * for use in a regular "int" Java image.
     *
     * **CW** The only reason we cannot use unsigned bytes is
     * because bloody Java will not support them.
     *
     * 12/9/04 - dealt with the silly byte issue. The index need
     *   not be a byte even though the data is! It just has to be
     *   constrained to between 0 to 255.
     * original code:
     * public int apply(byte v) {
     *	return (0xff<<24)|(red[v]<<16)|(green[v]<<8)|(blue[v]<<0);
     * }
     *
     */
    public int apply(int v) {
	return (0xff<<24)|(red[v]<<16)|(green[v]<<8)|(blue[v]<<0);
    }
    
    /*  NOBODY CALLS THIS CODE?
    public ColorModel getColorModel()
    {
	if (!cm_cached)
	    {
		cm=new IndexColorModel(8,256,red,green,blue,-1);
		cm_cached=true;
	    }
	return cm;
    }
    */
//    public void initGrey()
//    {
//	int i;
//	cm_cached=false;
//	for (i=0;i<256;i++)
//	    red[i]=green[i]=blue[i]=(byte)(0xff&i);
//    }

}
