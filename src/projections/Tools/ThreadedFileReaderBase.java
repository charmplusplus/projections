package projections.Tools;

import projections.analysis.StsReader;
import projections.gui.MainWindow;

public class ThreadedFileReaderBase {
    protected int myRun = 0;
    StsReader sts = MainWindow.runObject[myRun].getSts();
    final int totalPes = sts.getProcessorCount();
    final int totalNodes = sts.getSMPNodeCount();
    final int nodesize = sts.getNodeSize();

    protected boolean isSameNode(int pe1, int pe2) {
        int n1 = pe1 / nodesize;
        if (pe1 >= totalNodes * nodesize && pe1 < totalPes) n1 = pe1 - totalNodes * nodesize;
        int n2 = pe2 / nodesize;
        if (pe2 >= totalNodes * nodesize && pe2 < totalPes) n2 = pe2 - totalNodes * nodesize;
        return n1 == n2;
    }
}
