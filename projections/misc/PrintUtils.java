package projections.misc;

import java.awt.*;
import java.awt.print.*;

public class PrintUtils 
    implements Printable 
{
    private Component itemToBePrinted = null;

    public PrintUtils(Component component) {
	itemToBePrinted = component;
    }

    public int print(Graphics g, PageFormat pf, int pageIndex) {     
	// pageIndex 0 to 4 corresponds to page numbers 1 to 5.
	if (pageIndex >= 1) {
	    return Printable.NO_SUCH_PAGE;   
	}
	printComponent(g, pf);
	return Printable.PAGE_EXISTS;   
    } 

    private void printComponent(Graphics g, PageFormat pf) {
        Dimension componentSize = itemToBePrinted.getSize();

        System.out.println("Component dimensions = " + componentSize.width +
                           "x" + componentSize.height);

        // attempting to scale component proportionately to the whole 
	// printing region
        double pageHeight = pf.getImageableHeight();
        double pageWidth = pf.getImageableWidth();

        System.out.println("Paper dimensions = " + pageWidth + "x" 
			   + pageHeight);

	double pScale = Math.min(pageWidth/componentSize.width,
				 pageHeight/componentSize.height);

	((Graphics2D)g).translate(pf.getImageableX(),
				  pf.getImageableY());
        ((Graphics2D)g).scale(pScale, pScale);
    
	itemToBePrinted.printAll(g);
    }
}
