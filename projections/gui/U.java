/*
(U)ser interface (U)tility routines
Orion Sky Lawlor, olawlor@acm.org, 12/29/2000
*/
package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class U
{ 
	private U() {}
	//Makes i an "even" integer-- a near multiple of 10, or 5, or 25
	public static long makeEven(long i) {
		return (i/1000)*1000;
	}

	//Print nDec digits of the decimal expansion of
	// the fraction part of d (which must be positive).
	private final static String printDecimals(double d,int nDec)
	{
		StringBuffer ret=new StringBuffer();
		d-=(int)d;//Remove non-fraction part
		d+=0.000000001;
		for (int i=0;i<nDec;i++)
		{
			d*=10.0;
			int digit=(int)d;
			ret.append((char)(digit+'0'));
			d-=digit;
		}
		return ret.toString();
	}
	/*
	Return a human-readable version of this time, 
	given in microseconds.
	*/
	public static String t(long us)
	{
		if (us<0) return us+"us";
		if (us==0) return "0";
		if (us<1000) return "0."+printDecimals(us*0.001,3)+"ms";
		if (us<1000*10) return (int)(us/1000)+"."+printDecimals(us*0.001,3)+"ms";
		if (us<1000*100) return (int)(us/1000)+"."+printDecimals(us*0.001,2)+"ms";
		if (us<1000*1000) return (int)(us/1000)+"."+printDecimals(us*0.001,1)+"ms";
		if (us<1000*1000*10) return (int)(us/1000000)+"."+printDecimals(us*0.000001,3)+"s";
		if (us<1000*1000*100) return (int)(us/1000000)+"."+printDecimals(us*0.000001,2)+"s";
		return (int)(us/1000000)+"."+printDecimals(us*0.000001,1)+"s";
	}
	
	/*
	Parse a human-readable time, given by default in microseconds but
	with possible suffixes "ms" (milliseconds) or "s" (seconds).
	*/
	public static long fromT(String t)
	{
		double mantissa,conv=1.0;//Conversion factor to microseconds
		int tr=0;
		if (t.endsWith("s")) {tr=1;conv=1000000;} //seconds
		if (t.endsWith("ms")) {tr=2;conv=1000;} //milliseconds
		if (t.endsWith("us")) {tr=2;conv=1;} //microseconds
		if (t.endsWith("ns")) {tr=2;conv=0.001;} //nanoseconds
		if (t.endsWith("ps")) {tr=2;conv=0.000001;} //picoseconds
		if (tr!=0) t=t.substring(0,t.length()-tr);
		try {
			mantissa=Double.valueOf(t.trim()).doubleValue();
		} catch (NumberFormatException e) {
		//	System.out.println("U.fromT: Can't parse time '"+t+"'");
			return 0;
		}
		return (long)(mantissa*conv);
	}
}
