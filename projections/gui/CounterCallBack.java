package projections.gui;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import projections.gui.count.*;

public class CounterCallBack extends CallBack{
	CounterFrame f;
	ProjectionsFileChooser fc;
	public CounterCallBack(CounterFrame f,ProjectionsFileChooser fc){
		this.f = f;
		this.fc = fc;
	}
	public void callBack(){
	try{
		ProjectionsFileMgr  fileMgr = fc.getProjectionsFileMgr();
	  	fileMgr.printSts();
     	 	f.setFileMgr(fileMgr);
     	 	f.setSize(800,600);
		//f.setVisible(true);
     	 	f.loadFiles();
     	 	f.sortByColumn(1);

	}catch(Exception e){
		System.out.println("Exception ");
		e.printStackTrace();
	}
	}
}
