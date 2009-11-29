package projections.analysis;

/*
AsciiIntegerReader.java: Charm++ projections.
Orion Sky Lawlor, olawlor@acm.org, 12/28/2000

Similar to StreamTokenizer, but about 10x faster.
Will not work properly with true Unicode files--
only basic ASCII.
*/
import java.io.IOException;


/** A little class that can pull integers and strings out of a String  */
class AsciiLineParser {
	
	/// The next position in the string
	int pos;
	
	String line;
	
	public AsciiLineParser(String _line){
		line = _line;
		pos = 0;
	}
	
	    
	final public boolean isSpace(char c) {
		return c==' '||c=='\n'||c=='\t';
	}

	
	/// Read an character from the string. if at end of string, produce '\n'
	final private char nextChar() throws IOException {
		if(pos > line.length()){
			throw new IOException();
		} else if(pos == line.length()){
			return '\n';
		} else {
			char c = line.charAt(pos);
			pos++;
			return c;
		}
	}

	public String restOfLine() {
    	return line.substring(pos);
    }

	
	final public int nextInt() throws IOException {return (int)nextLong();}
	
	//Read a positive long from the current file 
	//With version 7.0, negative numbers have to be
	//  properly handled as well.
	final public long nextLong() throws IOException {
	  int multiplier = 1;
	  char c;
	  while (isSpace(c=nextChar())) {}
	  if (c == '-') {
	    multiplier = -1;
	    c=nextChar();
	  }
	  long ret=toDigit(c);
	  while (!isSpace(c=nextChar())) 
	    ret=10*ret+toDigit(c);
	  return ret*multiplier;
	}

	final public int toDigit(char c) {
		return (c)-('0');
	}

}

