package projections.gui.graph;

import projections.gui.*;

import java.awt.*;

public class LegendPanel 
    extends Panel
{
    private LegendCanvas canvas;
    private ScrollPane sp;

    public LegendPanel()
    {
	setSize(getPreferredSize());

	setBackground(Color.lightGray);

	sp = new ScrollPane();
	canvas = new LegendCanvas();

	sp.add(canvas);
	sp.setBackground(Color.lightGray);
	
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();

	setLayout(gbl);
	
	gbc.fill = GridBagConstraints.BOTH;
	gbc.insets = new Insets(5, 5, 5, 5);
	Util.gblAdd(this, new Label("Legend: Checked=Selected", Label.CENTER), gbc, 0,0,1,1,1,0);
	Util.gblAdd(this, sp,  gbc, 0, 1, 1, 1, 1, 10);
    }

    public void setData(String Nlabels[], Color NcolorMap[]) {
	canvas.setData(Nlabels, NcolorMap);
    }

    public void setColors(Color NcolorMap[]) {
	canvas.setColors(NcolorMap);
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
