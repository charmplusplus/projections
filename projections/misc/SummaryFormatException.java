package projections.misc;

public class SummaryFormatException extends java.lang.Exception
{
	String why;
	public SummaryFormatException(String Nwhy)
	{
	  why = Nwhy;
	}
	public String toString()
	{
	  return "file format exception:"+why;
	}
}