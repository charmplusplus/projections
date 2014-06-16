package projections.gui;

public class U
{ 
	private U() {}
	/*
	Parse a human-readable time, given by default in microseconds but
	with possible suffixes "ms" (milliseconds) or "s" (seconds).
	*/
	protected static long fromT(String t)
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

//	//Makes i an "even" integer-- a near multiple of 10, or 5, or 25
//	public static long makeEven(long i) {
//		if (i>1000)
//		  return (i/1000)*1000;
//                else if (i>10)
//		  return (i/10)*10;
//                else 
//		  return i;
//	}
	
	
	//Print nDec digits of the decimal expansion of
	// the fraction part of d (which must be positive).
	private final static String printDecimals(double d,int nDec)
	{
		StringBuffer ret=new StringBuffer();
		ret.append(".");
		d-=(int)d;//Remove non-fraction part
		d+=0.000000001; // This seems like a bad idea to Isaac :( Why is this here ????
		for (int i=0;i<nDec;i++)
		{
			d*=10.0;
			int digit=(int)d;
			ret.append((char)(digit+'0'));
			d-=digit;
		}

		return truncateTrailingZeroPeriod(ret.toString());
	}
	

	public final static String truncateTrailingZeroPeriod(String in){
		if(in.lastIndexOf(".") >= 0){
			// Truncate off any trailing 0s
			String s = in;
			while(s.length()>0 && s.lastIndexOf("0") == s.length()-1){
				s = s.substring(0, s.length()-1);
			}	
			// Truncate off a trailing .
			if(s.length()>0 && s.lastIndexOf(".") == s.length()-1){
				s = s.substring(0, s.length()-1);
			}
			return s;	
		} else {
			return in;
		}
	}


    /*
      Return a human-readable version of this time, 
      given in microseconds. 02/23/2005 - **CW** added default
      wrapper with 3 dec pl. 06/16/2014 - Changed 3 dec places
      to number of digits in us to ensure entire log file is read.
    */
    public static String humanReadableString(long us) {
	return humanReadableString(us, String.valueOf(us).length());
    }

    public static String humanReadableString(long us, int places)
    {
	if (us<0) return us+"us";
	if (us==0) return "0";
	if (us<1000) return "0"+printDecimals(us*0.001,places)+"ms";
	if (us<1000*1000) return (int)(us/1000)+printDecimals(us*0.001,places)+"ms";
	return (int)(us/1000000)+printDecimals(us*0.000001,places)+"s";
    }

}
