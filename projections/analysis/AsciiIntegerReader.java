/*
AsciiIntegerReader.java: Charm++ projections.
Orion Sky Lawlor, olawlor@acm.org, 12/28/2000

Similar to StreamTokenizer, but about 10x faster.
Will not work properly with true Unicode files--
only basic ASCII.
*/
package projections.analysis;
import java.lang.*;
import java.io.*;
import java.util.*;

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
	final public char nextChar() throws IOException{
		if (idx>=len) fillBuffer();
		return buffer[idx++];
	}
	final public boolean isSpace(char c) {
		return c==' '||c=='\n'||c=='\t';
	}
	final public int toDigit(char c) {
		return ((int)c)-((int)'0');
	}
	//Read a positive integer from the current file 
	public int nextInt() throws IOException {
		char c;
		while (isSpace(c=nextChar())) {}
		int ret=toDigit(c);
		while (!isSpace(last=c=nextChar())) 
			ret=10*ret+toDigit(c);
    		return ret;
	}
	//Read until the end-of-line is encountered
	public void nextLine() throws IOException {
		if (last=='\n') {last='\0';return;}
		while (nextChar()!='\n') {}
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
}
