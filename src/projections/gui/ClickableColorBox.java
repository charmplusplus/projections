package projections.gui;

import java.awt.Color;

public class ClickableColorBox {
    public int myRun;
    Color c;

    private ColorUpdateNotifier gw;
    private int id;
    private GenericGraphColorer colorer;

    ClickableColorBox(int id_, Color c_, int myRun_, ColorUpdateNotifier gw_) {
        id = id_;
        c = c_;
        myRun = myRun_;
        gw=gw_;
        colorer = null;
    }

    ClickableColorBox(int id_, Color c_, int myRun_, ColorUpdateNotifier gw_, GenericGraphColorer colorer_) {
        id = id_;
        c = c_;
        myRun = myRun_;
        gw=gw_;
        colorer = colorer_;
    }

    public void setColor(Color c){
        this.c = c;
        if(colorer != null)
            colorer.getColorMap()[id] = c;
        else
            MainWindow.runObject[myRun].setEntryColor(id, c);
        gw.colorsHaveChanged();
    }
}
