// UNUSED FILE

//package projections.gui.count;
//
//import java.io.*;
//import java.util.*;
//import java.awt.*;
//import java.awt.event.*;
//import javax.swing.*;
//import javax.swing.table.*;
//import javax.swing.event.*;
//import projections.gui.*;
//
//public class FrameCallBack extends CallBack{
//	public CounterFrame f;
//	public FrameCallBack(CounterFrame f){
//		this.f = f;
//	}
//	public void callBack(){
//		f.setVisible(true);
//		f.tabbedPane_.removeAll();
//    		for (int i=0; i<f.cTable_.getNumSims(); i++) {
//      			String name = f.cTable_.getRunName(i);
//      			f.tabbedPane_.addTab(name, null, f.cTable_.getCounterPanel(i),
//			f.cTable_.getToolTip(i));
//    		}
//    		f.jTable_.tableChanged(new TableModelEvent(
//      		f.cTable_, 0, f.cTable_.getRowCount()-1, TableModelEvent.ALL_COLUMNS));
//
//	}
//}