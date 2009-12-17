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
	private int pos;
	
	private String line;
	
	protected AsciiLineParser(String _line){
		line = _line;
		pos = 0;
	}
	
	    
	final private boolean isSpace(char c) {
		return c==' '||c=='\n'||c=='\t';
	}

	
	/// Read an character from the string. if at end of string, produce '\n'
	final private char nextChar() throws IOException {
		if(pos > line.length()){
			throw new IOException();
		} else if(pos == line.length()){
			pos++;
			return '\n';
 		} else {
			char c = line.charAt(pos);
			pos++;
			return c;
		}
	}

	protected String restOfLine() {
    	return line.substring(pos);
    }


	//Read a positive long from the current file 
	//With version 7.0, negative numbers have to be
	//  properly handled as well.
	protected final long nextLong() throws IOException {
		if(line == null){
			throw new IOException();
		}

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

	final private int toDigit(char c) {
		return (c)-('0');
	}

}

