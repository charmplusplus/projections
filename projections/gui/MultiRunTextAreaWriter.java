package projections.gui;

import java.io.*;

import java.awt.*;
import java.awt.event.*;

public class MultiRunTextAreaWriter
    extends Writer
{
    TextArea textArea;
    // used to make writing to the textarea more efficient
    StringBuffer tempBuffer;

    boolean newInput = true;

    public MultiRunTextAreaWriter(TextArea NtextArea) {
	textArea = NtextArea;
	tempBuffer = new StringBuffer();
    }

    public void close() {
	flush(); // by contract
	// set newInput flag
	newInput = true;
    }

    public void flush() {
	textArea.append(tempBuffer.toString());
	tempBuffer = null;
	tempBuffer = new StringBuffer();
    }

    public void write(char[] cbuf) {
	checkNewInput();
	write(new String(cbuf));
    }

    public void write(char[] cbuf, int off, int len) {
	checkNewInput();
	write(new String(cbuf, off, len));
    }

    public void write(int c) {
	checkNewInput();
	write(String.valueOf((char)c));
    }

    public void write(String str) {
	checkNewInput();
	tempBuffer.append(str);
    }

    public void write(String str, int off, int len) {
	checkNewInput();
	write(str.substring(off,off+len));
    }

    private void checkNewInput() {
	if (newInput) {
	    textArea.setText("");
	}
	newInput = false;
    }
}
