package projections.gui;

import java.awt.Paint;

public class GenericGraphDefaultColors implements GenericGraphColorer {
	int myRun = 0;
	
	GenericGraphDefaultColors(){
	}
	
	public Paint[] getColorMap() {
		return MainWindow.runObject[myRun].getEPColorMap();
	}

}
