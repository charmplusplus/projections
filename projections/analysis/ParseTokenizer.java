package projections.analysis;
import java.io.*;

/** Joshua Mostkoff Unger, unger1@uiuc.edu
 *  Parallel Programming Laboratory
 *
 *  Simple extension of StreamTokenizer to get certain attributes so code 
 *  isn't duplicated . */
public class ParseTokenizer extends StreamTokenizer {
  /** Constructor. */
  public ParseTokenizer(Reader r) { super(r); }
  /** Throw IOException if expected isn't the next token. */
  public void checkNextString(String expected)
    throws IOException
  {
    String ret=nextString(expected);
    if (!expected.equals(ret)) {
      throw new IOException("Expected "+expected+" got "+ret);
    }
  }
  /** Return next number or throw IOException */
  public double nextNumber(String description)
    throws IOException
  {
    if (StreamTokenizer.TT_NUMBER!=super.nextToken()) {
      throw new IOException("Couldn't read "+description+" got "+toString());
    }
    return super.nval;
  }
  /** Return next number number (assuming scientific notation) or throw 
   *  IOException. */
  public double nextScientific(String description)
    throws IOException
  {
    double mantissa = nextNumber(description+" mantissa");
    String expString = nextString(description+" exponent");
    char expChar = expString.charAt(0);
    if (expChar != 'e' && expChar != 'd' &&
	expChar != 'E' && expChar!='D') 
    {
      throw new IOException("Couldn't find exponent in " + expString);
    }
    int exponent;
    expString = expString.substring(1); //Clip off leading "e"
    try {
      exponent = Integer.parseInt(expString);
    } 
    catch (NumberFormatException e) {
      throw new IOException("Couldn't parse exponent " + expString);
    }
    return mantissa*Math.pow(10.0,exponent);
  }
  /** Return next string or throw IOException if next token isn't string. */
  public String nextString(String description)
    throws IOException
  {
    if (StreamTokenizer.TT_WORD!=super.nextToken()) {
      throw new IOException(
        "Couldn't read string " + description+" got "+toString());
    }
    return super.sval;
  }

    /**
     *  Skips a line of data. Added by Chee Wai Lee 1/23/2004
     */
    public void skipLine()
	throws IOException
    {
	while (StreamTokenizer.TT_EOL != nextToken()) {
	}
    }
}
