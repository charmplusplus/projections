package projections.analysis;

/*
AsciiIntegerReader.java: Charm++ projections.
Orion Sky Lawlor, olawlor@acm.org, 12/28/2000

Similar to StreamTokenizer, but about 10x faster.
Will not work properly with true Unicode files--
only basic ASCII.
*/
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

class AsciiIntegerReader {
	private Reader file;
	private char buffer[];
	private int idx; //Current location in buffer
	private int len; //Current length of buffer
	private char last; //Last character read unneccesarily
	
	public AsciiIntegerReader(Reader fromFile) {
		idx=len=0;
		buffer=new char[1000];
		file=fromFile;
		last='\0';
	}
	public void close() throws IOException {
		idx=len=0;
		buffer=null;
		file.close();
		file=null;
	}
	public void fillBuffer() throws IOException{
		idx=0;
		len=file.read(buffer,0,1000);
		if (len<=0) throw new EOFException();
	}

    public long skip(long n) 
	throws IOException
    {
	int charsSkipped = 0;

	if (idx+n <= len) {
	    charsSkipped += n;
	    idx += n;
	} else {
	    charsSkipped += len-idx;
	    idx += charsSkipped;
	    long temp = file.skip(n-charsSkipped);
	    if (temp > 0) {
		charsSkipped += temp;
		try {
		    fillBuffer();
		} catch (EOFException e) {
		    // do nothing - should not happen since there is
		    // at least one item in the skipped region.
		}
	    }
	}
	return charsSkipped;
    }

    public void seek(long n) throws IOException{
    	skip(n-idx); 	
    }
    
	final public boolean isSpace(char c) {
		return c==' '||c=='\n'||c=='\t';
	}
	final public char nextChar() throws IOException{
		if (idx>=len) fillBuffer();
		return buffer[idx++];
	}
	final public int nextInt() throws IOException {return (int)nextLong();}

	//Read until the end-of-line is encountered
	public void nextLine() throws IOException {
		if (last=='\n') {last='\0';return;}
		while (nextChar()!='\n') {}
	}

    public String readLine() 
	throws IOException
    {
	char temp;
	StringBuffer buffer = new StringBuffer();
	if (last=='\n') {
	    last = '\0';
	    return "";
	}
	do {
	    temp = nextChar();
	    if (temp != '\n') {
		buffer.append(temp);
	    } else {
		return buffer.toString();
	    }
	} while (true);
    }

	//Read a positive long from the current file 
        //With version 7.0, negative numbers have to be
        //  properly handled as well.
	final public long nextLong() throws IOException {
	  int multiplier = 1;
	  char c;
	  while (isSpace(c=nextChar())) {}
	  if (c == '-') {
	    multiplier = -1;
	    last=c=nextChar();
	  }
	  long ret=toDigit(c);
	  while (!isSpace(last=c=nextChar())) 
	    ret=10*ret+toDigit(c);
	  return ret*multiplier;
	}
	//Read a whitespace-separated string
	public String nextString() throws IOException {
		char c;
		while (isSpace(c=nextChar())) {}
		StringBuffer ret=new StringBuffer();
		ret.append(c);
		while (!isSpace(last=c=nextChar())) 
			ret.append(c);
		return ret.toString();
	}
	final public int toDigit(char c) {
		return ((int)c)-((int)'0');
	}
}

