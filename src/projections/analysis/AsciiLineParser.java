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
	
	final private String line;
	final private int lineLength;
	
	protected AsciiLineParser(String _line){
		line = _line;
		lineLength = line.length();
		pos = 0;
	}
	
	/// Read an character from the string. if at end of string, produce '\n'
	final private char nextChar() throws IOException {
		if(pos > lineLength){
			throw new IOException();
		} else if(pos == lineLength){
			pos++;
			return '\n';
 		} else {
			char c = line.charAt(pos);
			pos++;
			return c;
		}
	}

	final protected String nextString(int strlen) {
    		String ret = line.substring(pos, pos+strlen);
		pos += strlen;
		return ret;
    	}

    // Checks whether there is another field available in the line, without
    // returning that field or modifying the line.
    final protected boolean hasNextField() {
        while (pos < lineLength && Character.isWhitespace(line.charAt(pos))) {
            pos++;
        }

        return (pos < lineLength && !Character.isWhitespace(line.charAt(pos)));
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
		while (Character.isWhitespace(c=nextChar())) {}
		if (c == '-') {
			multiplier = -1;
			c=nextChar();
		}
		long ret=toDigit(c);
		while (!Character.isWhitespace(c=nextChar()))
			ret=10*ret+toDigit(c);
		return ret*multiplier;
	}
	//Reads in a double.
        protected final double nextDouble() throws IOException {
		if(line == null){
			throw new IOException();
		}

		int multiplier = 1;
		char c;
		int counter = 0;
 		boolean decimal = false;
		while (Character.isWhitespace(c=nextChar())) {}
		if (c == '-') {
			multiplier = -1;
			c=nextChar();
		}
		double ret=toDigit(c);
		while (!Character.isWhitespace(c=nextChar())) {
                	if(c == '.') {
				decimal = true;
				continue;
			}
			if(decimal)
				counter=counter+1;
			ret=10*ret+toDigit(c);
		}
		return ret*multiplier / Math.pow(10,counter);
	}

	final private int toDigit(char c) {
		return (c)-('0');
	}

}

