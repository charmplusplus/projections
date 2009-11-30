package projections.gui;

public class U
{ 
	private U() {}
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
	//Makes i an "even" integer-- a near multiple of 10, or 5, or 25
	public static long makeEven(long i) {
		if (i>1000)
		  return (i/1000)*1000;
                else if (i>10)
		  return (i/10)*10;
                else 
		  return i;
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
      given in microseconds. 02/23/2005 - **CW** added default
      wrapper with 3 dec pl.
    */
    public static String humanReadableString(long us) {
	return humanReadableString(us, 3);
    }

    public static String humanReadableString(long us, int places)
    {
	if (us<0) return us+"us";
	if (us==0) return "0";
	if (us<1000) return "0."+printDecimals(us*0.001,places)+"ms";
	if (us<1000*1000) return (int)(us/1000)+"."+
			      printDecimals(us*0.001,places)+"ms";
	return (int)(us/1000000)+"."+
	    printDecimals(us*0.000001,places)+"s";
    }

    public static int bestNumPlaces(long start, long skip, int num) {
	return 0;
    }

    public static int numUselessZeros(long number) {
	int count = 0;
	while (true) {
	    if (number%10 > 0) {
		break;
	    } else {
		count++;
		number /= 10;
	    }
	}
	return count;
    }

    public static void main(String args[]) {
	System.out.println(numUselessZeros(2345800));
	System.out.println(numUselessZeros(20034));
	System.out.println(numUselessZeros(234580));
    }
}
