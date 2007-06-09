package projections.gui;

import java.awt.*;
import java.io.*;
import java.awt.event.*;

public class HelpWindow extends Frame
   implements ActionListener
{  
  
	private static final long serialVersionUID = 1L;

	public HelpWindow(MainWindow mainWindow)  
   {      
	  addWindowListener(new WindowAdapter()
	  {                    
		 public void windowClosing(WindowEvent e)
		 {
			setVisible(false);
		 }
	  });
	  
	  MenuBar mbar = new MenuBar();
	  
	  mbar.add(Util.makeMenu("File", new Object[]
	  {
		 "Close"
	  },
	  this));                  
   
	  Menu helpMenu = new Menu("Help");
	  mbar.add(Util.makeMenu(helpMenu, new Object[]
	  {
		 "About"
	  },
	  this)); 
	  
	  mbar.setHelpMenu(helpMenu);
	  setMenuBar(mbar); 
	  
	  TextArea ta = new TextArea("", 100, 100, 1);
	  ta.setEditable(false);
	  
	  try
	  {
		 BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("HelpWindow.txt")));
	  
		 String line;
		 while((line = br.readLine()) != null)
			ta.append(line + "\n");
	  }
	  catch (IOException e) {}
			
	  add(ta, "Center");
	  pack();
	  
	  setTitle("Projections Help");
   }   
   public void actionPerformed(ActionEvent evt)
   {
	  if(evt.getSource() instanceof MenuItem)
	  {
		 MenuItem mi = (MenuItem)evt.getSource();
		 String arg = mi.getLabel();
		 if(arg.equals("Close"))
			dispose();
		 /* don't keep this anymore
		 else if(arg.equals("About"))
			mainWindow.ShowAboutDialog((Frame) this);       
		 */
	  }
   }   
}
