//
//package projections.gui.Timeline;
//
//import java.awt.Graphics;
//import java.awt.event.*;
//
//import javax.swing.JPanel;
//
//
///** A class that handles the mouse events and painting of a user selection over the axis */
//public class MainOverlayPanel extends JPanel
//{ 
//	
//	private MainOverlayPanel thisPanel;
//
//
//	private static final long serialVersionUID = 1L;
//
//	private Data  data;
//
//
//
//	public MainOverlayPanel(Data data)
//	{
//		this.data = data;
//		thisPanel = this;
//
//		setOpaque(false);
//	}   
//
//
//	public void paintComponent(Graphics g)
//	{
////		super.paintComponent(g);
////		System.out.println("PaintComponent() AxisOverlayPanel size="+getWidth()+"x"+getHeight());
////
////		if(data.selectionValid()){
////			
////			g.setColor(Color.red);
////			g.drawLine(data.leftSelection(),0, data.leftSelection(), getHeight()-1);
////			g.drawLine(data.leftSelection()-1,0, data.leftSelection()-1, getHeight()-1);
////			g.drawLine(data.leftSelection()+1,0, data.leftSelection()+1, getHeight()-1);
////		
////			g.setColor(Color.green);
////			g.drawLine(data.rightSelection(),0, data.rightSelection(), getHeight()-1);
////			g.drawLine(data.rightSelection()-1,0, data.rightSelection()-1, getHeight()-1);
////			g.drawLine(data.rightSelection()+1,0, data.rightSelection()+1, getHeight()-1);
////
////		}
//		
////		g.setColor(Color.blue);
////		g.drawLine(mouseDraggedX,0, mouseDraggedX, getHeight()-1);
////		g.drawLine(mouseDraggedX-1,0, mouseDraggedX-1, getHeight()-1);
////		g.drawLine(mouseDraggedX+1,0, mouseDraggedX+1, getHeight()-1);
//		
//		
//	}
//
//
//
//
//
//}
