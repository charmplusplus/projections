package projections.misc;

import projections.gui.graph.*;

import java.awt.*;
import java.io.*;

/**
 *  Text renderer for multi-run data. More like a general table drawer.
 *  Given an output writer and a data source, draws a pretty-printed
 *  table.
 *
 *  It is the responsibility of the client object to supply the writer
 *  stream and to close it.
 */
public class MultiRunTextRenderer {

    private Writer writer;
    private int numSets;
    private int numCols;
    private int numRows;
    private DataSource source[];
    private String rowLabels[];
    private String colLabels[];

    private boolean withSubTable = false;

    // ppTable provides the column widths of each data column
    // ppTable dim1 idx = col number
    // ppTable dim2 idx = 0 == column total, x == subcolumn index + 1
    private int ppTable[][];
    private int rowLabelColumnWidth;

    // outputTable stores text information (while ppTable is being filled)
    // outputTable dim1 idx = row number
    // outputTable dim2 idx = col number
    // outputTable dim3 idx = sub column index
    private String outputTable[][][];

    public MultiRunTextRenderer() {
    }

    public void generateOutput(Writer Nwriter, DataSource Nsource[],
			       String NrowLabels[], String NcolLabels[]) 
	throws IOException
    {
	writer = Nwriter;
	source = Nsource;
	numSets = Nsource.length;
	numCols = Nsource[numSets-1].getIndexCount();
	numRows = Nsource[numSets-1].getValueCount();

	rowLabels = NrowLabels;
	colLabels = NcolLabels;

	rowLabelColumnWidth = 0;
	ppTable = new int[numCols][numSets+1];
	outputTable = new String[numRows][numCols][numSets];
	
	// initialize outputTable
	for (int row=0; row<numRows; row++) {
	    for (int col=0; col<numCols; col++) {
		for (int subcol=0; subcol<numSets; subcol++) {
		    outputTable[row][col][subcol] = "";
		}
	    }
	}

	// determine row Label width
	for (int row=0; row<numRows; row++) {
	    if (rowLabelColumnWidth < rowLabels[row].length()) {
		rowLabelColumnWidth = rowLabels[row].length();
	    }
	}

	// fill ppTable and store strings into outputTable

	// set initial ppTable values using colLabels
	for (int col=0; col<numCols; col++) {
	    int tempWidth = 0;
	    for (int subcol=0; subcol<numSets; subcol++) {
		ppTable[col][subcol+1] = source[subcol].getTitle().length();
		tempWidth += ppTable[col][subcol+1];
	    }
	    tempWidth += numSets-1; // add the dividers
	    if (tempWidth < colLabels[col].length()) {
		spreadDifference(colLabels[col].length()-tempWidth,
				 ppTable[col]);
		ppTable[col][0] = colLabels[col].length();
	    } else {
		ppTable[col][0] = tempWidth;
	    }
	}

	// measure width of/store actual data
	double colData[] = new double[numRows];
	for (int col=0; col<numCols; col++) {
	    int tempWidth = 0;
	    for (int subcol=0; subcol<numSets; subcol++) {
		source[subcol].getValues(col, colData);
		for (int row=0; row<numRows; row++) {
		    // writing to output table
		    outputTable[row][col][subcol] +=
			String.valueOf(new Double(colData[row]).longValue());

		    // determining updating the largest width
		    int difference
			= outputTable[row][col][subcol].length() -
			ppTable[col][subcol+1];
		    if (difference > 0) {
			ppTable[col][subcol+1] += difference;
		    }		
		}
		tempWidth += ppTable[col][subcol+1];
	    }
	    tempWidth += numSets-1;
	    if (tempWidth < ppTable[col][0]) {
		spreadDifference(ppTable[col][0]-tempWidth,
				 ppTable[0]);
	    } else {
		ppTable[col][0] = tempWidth;
	    }
	}

	// output headers
	renderColumnLabels();

	// write data rows
	for (int row=0; row<numRows; row++) {
	    // output row label
	    writer.write("|" + rowLabels[row] +
			 pad(rowLabelColumnWidth - rowLabels[row].length(), ' ') +
			 "|");
	    for (int col=0; col<numCols; col++) {
		for (int subcol=0; subcol<numSets; subcol++) {
		    writer.write(outputTable[row][col][subcol] +
				 pad(ppTable[col][subcol+1]-outputTable[row][col][subcol].length(), ' ') +
				 "|");
		}
	    }
	    writer.write(System.getProperty("line.separator"));
	    renderLine();
	}
    }

    private void renderColumnLabels() 
	throws IOException
    {
	renderLine();
	// render column labels
	writer.write("|" + pad(rowLabelColumnWidth, ' ') + "|");
	for (int col=0; col<numCols; col++) {
	    writer.write(colLabels[col] + "|");
	}
	writer.write(System.getProperty("line.separator"));
	renderLine();
	// render sub-column labels
	writer.write("|" + pad(rowLabelColumnWidth, ' ') + "|");
	for (int col=0; col<numCols; col++) {
	    for (int subcol=0; subcol<numSets; subcol++) {
		writer.write(source[subcol].getTitle() + 
			     pad(ppTable[col][subcol+1]-source[subcol].getTitle().length(), ' ') +
			     "|");
	    }
	}
	writer.write(System.getProperty("line.separator"));
	renderLine();
    }

    private void renderLine() 
	throws IOException
    {
	// rowLabel column
	writer.write("+" + pad(rowLabelColumnWidth, '-') + "+");
	for (int col=0; col<numCols; col++) {
	    // ignore total column size
	    for (int subcol=0; subcol<numSets; subcol++) {
		writer.write(pad(ppTable[col][subcol+1], '-') + "+");
	    }
	}
	writer.write(System.getProperty("line.separator"));
    }

    private String pad(int length, char inChar) {
	if (length == 0) {
	    return "";
	}
	char padding[] = new char[length];
	for (int i=0; i<length; i++) {
	    padding[i] = inChar;
	}
	return new String(padding);
    }

    private void spreadDifference(int difference, int ppSubCols[]) {
	for (int i=0; i<numSets; i++) {
	    ppSubCols[i+1] += difference/numSets;
	}
	for (int i=0; i<difference%numSets; i++) {
	    ppSubCols[i+1] += 1;
	}
    }
}
