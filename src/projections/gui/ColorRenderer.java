package projections.gui;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/// A simple color renderer
public class ColorRenderer extends JLabel
implements TableCellRenderer {	
	public ColorRenderer() {
		setOpaque(true);
	}
	public Component getTableCellRendererComponent(
			JTable table, Object color,
			boolean isSelected, boolean hasFocus,
			int row, int column) {
		if(color instanceof Color){
			setBackground((Color) color);
		}else if(color instanceof ClickableColorBox){
			setBackground(((ClickableColorBox) color).c);
		}
		return this;
	}
}
