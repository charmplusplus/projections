package projections.gui;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import projections.gui.count.*;

public class MultiRunCallBack extends CallBack{
    MultiRunWindow window;
    ProjectionsFileChooser fc;

    public MultiRunCallBack(MultiRunWindow window,
			    ProjectionsFileChooser fc){
	this.window = window;
	this.fc = fc;
    }

    public void callBack(){
	window.beginAnalysis(fc);
    }
}
