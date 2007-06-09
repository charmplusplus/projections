package projections.gui.count;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

// Modified from code from http://www.chka.de/swing/table/faq.html
public class ColorHeader extends DefaultTableCellRenderer
{
  private static final long serialVersionUID = 1L;
  
  private Color c_ = UIManager.getColor("TableHeader.background");
  
  public ColorHeader(Color c, String toolText) {
    if (c != null) { c_ = c; }
    if (toolText != null) { setToolTipText(toolText); }
    setHorizontalAlignment(SwingConstants.CENTER);
    setOpaque(true);
    // This call is needed because DefaultTableCellRenderer calls setBorder()
    // in its constructor, which is executed after updateUI()
    setBorder(UIManager.getBorder("TableHeader.cellBorder"));
  }
  
  public void updateUI() {
    super.updateUI();
    setBorder(UIManager.getBorder("TableHeader.cellBorder"));
  }
  
  public Component getTableCellRendererComponent(
    JTable table, Object value, boolean selected, boolean focused, 
    int row, int column)
  {
    JTableHeader header;
    
    if (table != null && (header = table.getTableHeader()) != null) {
      setEnabled(header.isEnabled());         
      setComponentOrientation(header.getComponentOrientation());
      setForeground(header.getForeground());
      // setBackground(header.getBackground());
      setBackground(c_);
      setFont(header.getFont());
    }
    else {
      // Use sensible values instead of random leftover values 
      // from the last call
      setEnabled(true);
      setComponentOrientation(ComponentOrientation.UNKNOWN);
      
      setForeground(UIManager.getColor("TableHeader.foreground"));
      // setBackground(UIManager.getColor("TableHeader.background"));
      setBackground(c_);
      setFont(UIManager.getFont("TableHeader.font"));
    }
    setValue(value);
    return this;
  }
}

