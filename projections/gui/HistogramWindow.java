package projections.gui;
import projections.gui.graph.*;

import java.awt.*;
import java.awt.event.*;

public class HistogramWindow extends Frame
   implements ActionListener
{
   private MainWindow mainWindow;

   public HistogramWindow(MainWindow mainWindow)
   {
	  this.mainWindow = mainWindow;

	  addWindowListener(new WindowAdapter()
	  {
		 public void windowClosing(WindowEvent e)
		 {
			Close();
		 }
	  });

	  setBackground(Color.lightGray);
	  setTitle("Projections Histograms");

	  CreateMenus();
	  CreateLayout();
	  pack();
	  setVisible(true);

	  // ShowDialog();
   }   
   /*
   private void ShowDialog()
   {
	  if(dialog == null)
		 dialog = new LogFileViewerDialog(this);
	  dialog.setVisible(true);
   }

   public void CloseDialog()
   {
	  if(dialog != null)
	  {
		 dialog.dispose();
		 dialog = null;
	  }
   }
   */

   public void actionPerformed(ActionEvent evt)
   {
	  if(evt.getSource() instanceof MenuItem)
	  {
		 MenuItem m = (MenuItem)evt.getSource();
		 if(m.getLabel().equals("Set Ranges"))
		   /*ShowDialog()*/;
		 else if(m.getLabel().equals("Close"))
			Close();
	  }
   }   
   private void Close()
   {
	  setVisible(false);
	  mainWindow.CloseHistogramWindow();
	  dispose();
  }   
  private void CreateLayout()
  {
	  Panel p = new Panel();
	  p.setBackground(Color.lightGray);

	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  gbc.fill = GridBagConstraints.BOTH;

	  setLayout(gbl);

	  Util.gblAdd(this, p, gbc, 0,0, 1,1, 1,1);

	  p.setLayout(gbl);

	  /*FIXME: hardcoded data*/
	  int data[]={0,1,5,14,3,0,0,7,5,1,0};
	  
	  DataSource ds=new DataSource1D("Histogram",data);
	  XAxis xa=new XAxisFixed("Entry Point Execution Time","ms");
	  YAxis ya=new YAxisAuto("Count","",ds);

/*Tiny test: try out these objects*/
	  System.out.println("Data source: '"+ds.getTitle()+"'; "+
	    ds.getIndexCount()+" data points, each with "+
	    ds.getValueCount()+" values");
	  System.out.println("XAxis: '"+xa.getTitle()+"'; "+
	    "index 3's name is '"+xa.getIndexName(3)+"'");
	  System.out.println("YAxis: '"+ya.getTitle()+"'; "+
	    "from "+ya.getMin()+" to "+ya.getMax()+", "+
	    "with values like '"+ya.getValueName(5.0)+"'");
	  
	  /*FIXME: actually add the graph display here--
	  Graph g=new Graph();
	  g.setData(ds,xa,ya);
	  p.add(g);
	  */
	  

   }   
   private void CreateMenus()
   {
	  MenuBar mbar = new MenuBar();

	  mbar.add(Util.makeMenu("File", new Object[]
	  {
		 "Close"
	  },
	  this));

	  Menu helpMenu = new Menu("Help");
	  mbar.add(Util.makeMenu(helpMenu, new Object[]      {

		 "Index",
		 "About"
	  },
	  this));

	  mbar.setHelpMenu(helpMenu);
	  setMenuBar(mbar);
   }   
}
