package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class GraphDisplayPanel extends Container
   implements ActionListener, ItemListener, AdjustmentListener
{
   private GraphData data;
 
   private Panel              mainPanel;
   private GraphTitleCanvas   titleCanvas;
   private GraphYAxisCanvas   yAxisCanvas;
   private GraphXAxisCanvas   xAxisCanvas;
   private GraphWAxisCanvas   wAxisCanvas;
   private GraphDisplayCanvas displayCanvas;
   private Scrollbar          HSB;
 
   private Button             bIncreaseX;
   private Button             bDecreaseX;
   private Button             bResetX;
   private Checkbox           cbLineGraph;
   private Checkbox           cbBarGraph;
   private Label              lScale;
   private FloatTextField     scaleField;
   
   public GraphDisplayPanel()
   {
      addComponentListener(new ComponentAdapter()
      {                    
         public void componentResized(ComponentEvent e)
         {
            if(mainPanel != null)
            {
               setAllBounds();
               UpdateDisplay();
            }   
         }
      });
      
      setBackground(Color.lightGray);
      
      ////// Main Panel
      mainPanel     = new Panel();
      titleCanvas   = new GraphTitleCanvas();
      yAxisCanvas   = new GraphYAxisCanvas();
      wAxisCanvas   = new GraphWAxisCanvas();
      xAxisCanvas   = new GraphXAxisCanvas();
      displayCanvas = new GraphDisplayCanvas();
      
      HSB = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 1);
      
      mainPanel.setLayout(null);
      mainPanel.setBackground(Color.black);
      mainPanel.setForeground(Color.white);
      mainPanel.add(titleCanvas);
      mainPanel.add(yAxisCanvas);
      mainPanel.add(wAxisCanvas);
      mainPanel.add(xAxisCanvas);
      mainPanel.add(displayCanvas);
      mainPanel.add(HSB);
      
      HSB.setBackground(Color.lightGray);
      HSB.addAdjustmentListener(this);
      
      //////   Button Panel
      lScale  = new Label("X-Axis Scale: ", Label.CENTER);      
      
      scaleField = new FloatTextField(1, 5);
      scaleField.addActionListener(this);
   
      bDecreaseX = new Button("<<");
      bIncreaseX = new Button(">>");
      bResetX    = new Button("Reset");
      bIncreaseX.addActionListener(this);
      bDecreaseX.addActionListener(this);
      bResetX.addActionListener(this);
      
      CheckboxGroup cbgGraphType = new CheckboxGroup();
      cbLineGraph = new Checkbox("Line Graph", false,  cbgGraphType);
      cbBarGraph  = new Checkbox("Bar Graph",  true, cbgGraphType);
      cbLineGraph.addItemListener(this);
      cbBarGraph.addItemListener(this);
      
      
      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      
      Panel buttonPanel = new Panel();
      buttonPanel.setLayout(gbl);
      
      Util.gblAdd(buttonPanel, cbLineGraph, gbc, 0,0, 1,1, 1,0);
      Util.gblAdd(buttonPanel, cbBarGraph,  gbc, 1,0, 1,1, 1,0);
      Util.gblAdd(buttonPanel, bDecreaseX,  gbc, 2,0, 1,1, 1,0);
      Util.gblAdd(buttonPanel, lScale,      gbc, 3,0, 1,1, 1,0);
      Util.gblAdd(buttonPanel, scaleField,  gbc, 4,0, 1,1, 1,0);
      Util.gblAdd(buttonPanel, bIncreaseX,  gbc, 5,0, 1,1, 1,0);
      Util.gblAdd(buttonPanel, bResetX,     gbc, 6,0, 1,1, 1,0);
      
      /////// put it together
      setLayout(gbl);
      gbc.fill = GridBagConstraints.BOTH;
      Util.gblAdd(this, mainPanel,   gbc, 0,0, 1,1, 1,1, 10,10,10,10);
      Util.gblAdd(this, buttonPanel, gbc, 0,1, 1,1, 1,0, 10,10,10,10);  
   }
   
   public void setAllBounds()
   {
      if(data == null)
         return;
         
      //// set the sizes
      int mpw, mph, tch, ycw, wcw, xch, sbh, dcw, dch;
      
      mpw = mainPanel.getSize().width;
      mph = mainPanel.getSize().height;
      
      tch = titleCanvas.getPreferredHeight();
      ycw = yAxisCanvas.getPreferredWidth();
      wcw = wAxisCanvas.getPreferredWidth();
      xch = xAxisCanvas.getPreferredHeight();
      
      sbh = 20;
      
      dcw = mpw - ycw - wcw;
      dch = mph - tch - xch - sbh;
      
      data.offset2 = xch;
      
      
      // --> set the bounds
      // must set the bounds for the axes before the display canvas so that
      // the scales are set appropriately.
      
      titleCanvas.setBounds  (ycw,       0,     dcw, tch);
      yAxisCanvas.setBounds  (0,       tch,     ycw, dch+xch);
      wAxisCanvas.setBounds  (mpw-wcw, tch,     wcw, dch+xch);
      xAxisCanvas.setBounds  (ycw,     tch+dch, dcw, xch);
      displayCanvas.setBounds(ycw,     tch,     dcw, dch);
      HSB.setBounds          (ycw,     mph-sbh, dcw, sbh); 
      
      HSB.setMaximum((int)(data.scale * dcw));
      HSB.setVisibleAmount(dcw);
      HSB.setBlockIncrement(dcw);
 
      if(data.scale > 1)
         HSB.setVisible(true);
       else
         HSB.setVisible(false);
   }         
      
   public void setGraphData(GraphData data)
   {
      this.data = data;
      titleCanvas.setData(data);
      yAxisCanvas.setData(data);
      wAxisCanvas.setData(data);
      xAxisCanvas.setData(data);
      displayCanvas.setData(data);
   }   
   
   public void PrintGraph(Graphics pg, PrintJob pjob)
   {
      Dimension tmpd = mainPanel.getSize();
      Dimension d = pjob.getPageDimension();
      
      int marginLeft;
      int marginTop;
      if(d.width < d.height)
      {
         marginLeft = (int)(0.7 * d.width / 8.5);    
         marginTop  = (int)(0.7 * d.height / 11.0);
      }
      else
      {
         marginLeft = (int)(0.7 * d.width / 11.0);
         marginTop  = (int)(0.7 * d.height / 8.5);
      }      
      
      int printWidth  = d.width  - 2*marginLeft;
      int printHeight = d.height - 2*marginTop;
      
      mainPanel.setSize(printWidth, printHeight);
      pg.translate(marginLeft, marginTop);
      
      setAllBounds();
      mainPanel.setBackground(Color.white);
      
      mainPanel.printAll(pg);
      
      mainPanel.setSize(tmpd);
      setAllBounds();
      mainPanel.setBackground(Color.black);
   }   
    
   public void UpdateDisplay()
   {
      titleCanvas.repaint();
      yAxisCanvas.repaint();
      wAxisCanvas.repaint();
      xAxisCanvas.repaint();
      displayCanvas.repaint();   
   }  
    
   public void adjustmentValueChanged(AdjustmentEvent evt)
   {
      xAxisCanvas.repaint();
      displayCanvas.repaint();
   }
   
   public int getHSBValue()
   {
      return HSB.getValue();
   }   
   
   public int getDCW()
   {
      return displayCanvas.getSize().width;
   }   
   
   public void actionPerformed(ActionEvent evt)
   {
      if(data == null)
         return;
      
      float scale = data.scale;
      
      if(evt.getSource() instanceof Button)
      {
         Button b = (Button) evt.getSource();
         
         if(b == bDecreaseX)
         {
            scale = (float)((int)(scale * 4)-1)/4;
            if(scale < 1.0)
               scale = (float)1.0;
         }
         else if(b == bIncreaseX)
         {
            scale = (float)((int)(scale * 4)+1)/4;
         }
         else if(b == bResetX)
         {
            scale = (float)1.0;
         }
         scaleField.setText("" + scale);
      }
      else
      {
         scale = scaleField.getValue();
      } 
      
       if(scale > 1)
         HSB.setVisible(true);
       else
         HSB.setVisible(false);
     
      int dcw = displayCanvas.getSize().width;
      int dw  = (int)(scale * dcw);
      HSB.setMaximum(dw);
     
      data.scale = scale;
      
      setAllBounds();
      xAxisCanvas.repaint();
      displayCanvas.repaint();   
   }
   
   public void itemStateChanged(ItemEvent evt)
   {
      if(data == null)
         return;
         
      Checkbox c = (Checkbox) evt.getSource();
      if(c == cbLineGraph)
         data.graphtype = GraphData.LINE;
      else if(c == cbBarGraph)
         data.graphtype = GraphData.BAR;
      setAllBounds();
      UpdateDisplay();
   }                  
         
   public void paint(Graphics g)
   {
      g.setColor(Color.lightGray);
      g.fillRect(0, 0, getSize().width, getSize().height);
      g.setColor(Color.black);
      g.drawRect(0, 0, getSize().width-1, getSize().height-1);
      
      super.paint(g);
   }
}
      
