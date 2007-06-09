package projections.gui.graph;

import projections.gui.*;

import java.awt.*;
import javax.swing.*;

public class LegendPanel 
    extends JPanel
{
    private LegendCanvas canvas;
    private JScrollPane sp;

    private JPanel buttonPanel;
    private JButton clearAll;
    private JButton selectAll;
    
    public LegendPanel()
    {
	setSize(getPreferredSize());

	setBackground(Color.lightGray);

	sp = new JScrollPane();
	canvas = new LegendCanvas();

	sp.add(canvas);
	sp.setBackground(Color.lightGray);
	
	buttonPanel = new JPanel();
	clearAll = new JButton("Unselect All");
	clearAll.setActionCommand("unselect");
	clearAll.addActionListener(canvas);
	selectAll = new JButton("Select All");
	selectAll.setActionCommand("select");
	selectAll.addActionListener(canvas);
	buttonPanel.add(clearAll);
	buttonPanel.add(selectAll);

	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();

	setLayout(gbl);
	
	gbc.fill = GridBagConstraints.BOTH;
	gbc.insets = new Insets(5, 5, 5, 5);
	Util.gblAdd(this, new Label("Legend: Checked=Selected", Label.CENTER),
		                        gbc, 0,0, 1,1, 1,0);
	Util.gblAdd(this, sp,           gbc, 0,1, 1,1, 1,10);
	Util.gblAdd(this, buttonPanel,  gbc, 0,2, 1,1, 1,0);
    }

    public void setData(String Nlabels[], Color NcolorMap[],
			boolean NshowFilter[], int NsortMap[]) {
	canvas.setData(Nlabels, NcolorMap, NshowFilter, NsortMap);
    }

    public void setColors(Color NcolorMap[]) {
	canvas.setColors(NcolorMap);
    }

    public void setFilter(boolean Nfilter[]) {
	canvas.setFilter(Nfilter);
    }

    public void setSort(int Nmap[]) {
	canvas.setSort(Nmap);
    }

    public int[] getLegendSort() {
	return canvas.getSort();
    }

    public boolean[] getSelection() {
	return canvas.getSelection();
    }

    public Dimension getMinimumSize() {
	return new Dimension(100, 200);
    }
    
    public Dimension getPreferredSize() {
	return new Dimension(200, 400);
    }
}
