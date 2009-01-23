package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class ColorSelectWindow extends Dialog
   implements AdjustmentListener, ActionListener
{

private Label lRed, lGreen, lBlue;
   private Scrollbar sbRed, sbGreen, sbBlue;
   private DisplayCanvas displayCanvas;
   private Button bOK, bCancel;                           
							  
   private TextField tfRed, tfGreen, tfBlue;
   private Color currentColor;      
   String title;
   private Panel p1, p2;
   private Frame myParent;

   private class DisplayCanvas extends Canvas
   {
	  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Color foreground;
	  int w, h;
	  private FontMetrics fm;
	  private int titleHeight;
	  private int titleWidth;
	  
	  public DisplayCanvas(Color c)
	  {
		 foreground = c;
		 setBackground(Color.black);
	  }
	  public Dimension getMinimumSize() {return new Dimension(150,50);}
	  public Dimension getPreferredSize() {return new Dimension(200,75);}
	  
	  public void setColor(Color c)
	  {
		 foreground = c;
		 repaint();
	  }
	  
	  public void setString(String s)
	  {
		 title = s;
		 titleWidth = fm.stringWidth(title);
		 Graphics g = getGraphics();
		 g.clearRect(0, 0, w, h);
		 repaint();   
	  }   
		 
	  public void update(Graphics g)
	  {
		 paint(g);
	  }   
	  
	  public void paint(Graphics g)
	  {
		 if(fm == null)
		 {
			fm = g.getFontMetrics(g.getFont());
			titleHeight = fm.getHeight();
			titleWidth  = fm.stringWidth(title);
		 }   
			   
		 w = getSize().width;
		 h = getSize().height;

		 g.setColor(foreground);
		 g.drawString(title, (w-titleWidth)/2, titleHeight);
		 
		 g.drawLine(0, h, w/2, titleHeight+10);
		 g.fillRect(w/2+20, h/2, w/4, h/2);
		 
		 g.setColor(Color.gray);
		 g.drawLine(0, 0, w, 0);
		 g.drawLine(0, 1, w, 1);
		 g.drawLine(0, 0, 0, h);
		 g.drawLine(1, 0, 1, h);
		 
		 g.setColor(Color.white);
		 g.drawLine(0, h, w, h);
		 g.drawLine(0, h-1, w, h-1);
		 g.drawLine(w, h, w, 0);
		 g.drawLine(w-1, h, w-1, 0);
	  }
   }
   public ColorSelectWindow(Frame parent, Color currentColor, String s)
   {
       super(parent);
       this.setModal(true);
	  this.currentColor = currentColor;
	  myParent=parent;
	  title = s;
	 
	  addWindowListener(new WindowAdapter()
	  {                    
		 public void windowClosing(WindowEvent e)
		 {
			dispose();
		 }
	  });
	  
	  setTitle("Select Color");
	  setBackground(Color.gray);
	  
	  lRed   = new Label("Red");
	  lGreen = new Label("Green");
	  lBlue  = new Label("Blue");
	  
	  sbRed   = new Scrollbar(Scrollbar.HORIZONTAL, currentColor.getRed(),   10, 0, 265);
	  sbGreen = new Scrollbar(Scrollbar.HORIZONTAL, currentColor.getGreen(), 10, 0, 265);
	  sbBlue  = new Scrollbar(Scrollbar.HORIZONTAL, currentColor.getBlue(),  10, 0, 265);
	  sbRed.setBlockIncrement(16);
	  sbRed.addAdjustmentListener(this);
	  sbGreen.setBlockIncrement(16);
	  sbGreen.addAdjustmentListener(this);
	  sbBlue.setBlockIncrement(16);
	  sbBlue.addAdjustmentListener(this);
	  
	  tfRed   = new TextField("" + currentColor.getRed(),   5);
	  tfGreen = new TextField("" + currentColor.getGreen(), 5);
	  tfBlue  = new TextField("" + currentColor.getBlue(),  5);
	  tfRed.setEditable(false);
	  tfGreen.setEditable(false);
	  tfBlue.setEditable(false);
	  
	  bOK     = new Button("OK");
	  bCancel = new Button("Cancel");
	  bOK.addActionListener(this);
	  bCancel.addActionListener(this);
	   
	  displayCanvas = new DisplayCanvas(currentColor);
   
	  p1 = new Panel();
	  p1.setBackground(Color.lightGray);
	  
	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  p1.setLayout(gbl);
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  Util.gblAdd(p1, displayCanvas, gbc, 0,0, 3,1, 1,1, 10,10,10,10);
	  Util.gblAdd(p1, lRed,     gbc, 0,1, 1,1, 0,0, 0,2,0,2);
	  Util.gblAdd(p1, lGreen,   gbc, 0,2, 1,1, 0,0, 0,2,0,2);
	  Util.gblAdd(p1, lBlue,    gbc, 0,3, 1,1, 0,0, 0,2,0,2);
	  Util.gblAdd(p1, sbRed,    gbc, 1,1, 1,1, 1,0);
	  Util.gblAdd(p1, sbGreen,  gbc, 1,2, 1,1, 1,0);
	  Util.gblAdd(p1, sbBlue,   gbc, 1,3, 1,1, 1,0);
	  Util.gblAdd(p1, tfRed,    gbc, 2,1, 1,1, 0,0, 0,2,0,2);
	  Util.gblAdd(p1, tfGreen,  gbc, 2,2, 1,1, 0,0, 0,2,0,2);
	  Util.gblAdd(p1, tfBlue,   gbc, 2,3, 1,1, 0,0, 0,2,0,2); 
	  
	  p2 = new Panel();
	  p2.setBackground(Color.lightGray);
	  
	  p2.setLayout(new FlowLayout());
	  p2.add(bOK);
	  p2.add(bCancel);
	  
	  setLayout(new GridBagLayout());
	  Util.gblAdd(this, p1, gbc, 0,1, 1,1, 1,1, 2,2,2,2);
	  Util.gblAdd(this, p2, gbc, 0,2, 1,1, 1,0, 2,2,2,2);
	  
	  pack();
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  Button b = (Button)evt.getSource();
	  if(b == bOK)
	  {
		 myParent.setBackground(currentColor);
	  }   
	  setVisible(false);
   }   
   public void adjustmentValueChanged(AdjustmentEvent evt)
   {
	  int r = sbRed.getValue();
	  int g = sbGreen.getValue();
	  int b = sbBlue.getValue();
	  
	  tfRed.setText  ("" + r);
	  tfGreen.setText("" + g);
	  tfBlue.setText ("" + b);
	  
	  currentColor = new Color(r, g, b);
	  
	  displayCanvas.setColor(currentColor);
   }   
   public void setColor(Color c)
   {
	  int r = c.getRed();
	  int g = c.getGreen();
	  int b = c.getBlue();
	  
	  sbRed.setValue(r);
	  sbGreen.setValue(g);
	  sbBlue.setValue(b);
	  
	  tfRed.setText  ("" + r);
	  tfGreen.setText("" + g);
	  tfBlue.setText ("" + b);
	  
	  currentColor = c;
	  
	  displayCanvas.setColor(c); 
   }   
   public void setString(String s)
   {
	  displayCanvas.setString(s);
   }   
}
