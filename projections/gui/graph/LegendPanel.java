package projections.gui.graph;

import projections.gui.*;

import java.awt.*;

public class LegendPanel 
    extends Panel
{
    private LegendCanvas canvas;
    private ScrollPane sp;

    private Panel buttonPanel;
    private Button clearAll;
    private Button selectAll;

    public LegendPanel()
    {
	setSize(getPreferredSize());

	setBackground(Color.lightGray);

	sp = new ScrollPane();
	canvas = new LegendCanvas();

	sp.add(canvas);
	sp.setBackground(Color.lightGray);
	
	buttonPanel = new Panel();
	clearAll = new Button("Unselect All");
	clearAll.setActionCommand("unselect");
	clearAll.addActionListener(canvas);
	selectAll = new Button("Select All");
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
