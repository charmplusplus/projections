package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class LogFileViewerWindow extends Frame
   implements ActionListener
{
   private LogFileViewerDialog dialog;
   private int logfilenum = -1;
   private MainWindow mainWindow;
   private int oldlogfilenum = -1;
   
   private LogFileViewerTextArea textArea;
   private Label lTitle;
   private Button bOpen, bClose;
   private Panel titlePanel;
   
   public LogFileViewerWindow(MainWindow mainWindow)
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
      
      Toolkit tk = Toolkit.getDefaultToolkit();
      Dimension d = tk.getScreenSize();
      int w = d.width;
      int h = d.height;
      pack();
      setSize(3*w/4, 3*h/4);
      setLocation(w/8, h/8);
      setTitle("Projections Log File Viewer");
      
      CreateMenus();
      CreateLayout();
      setVisible(true);
      
      ShowDialog();
   }
   
   private void CreateMenus()
   {
      MenuBar mbar = new MenuBar();
      
      mbar.add(Util.makeMenu("File", new Object[]
      {
         "Open File",
         null,
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
   
   private void CreateLayout()
   {
      Panel p = new Panel();
      p.setBackground(Color.lightGray);
      
      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.BOTH;
      
      setLayout(gbl);
      
      Util.gblAdd(this, p, gbc, 0,0, 1,1, 1,1);
      
      textArea = new LogFileViewerTextArea();
      
      p.setLayout(gbl);
      
      titlePanel = new Panel();
      titlePanel.setBackground(Color.black);
      lTitle = new Label("", Label.CENTER);
      lTitle.setForeground(Color.white);
      lTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
      titlePanel.add(lTitle);
      
      Util.gblAdd(p, titlePanel, gbc, 0,0, 1,1, 1,0, 5,5,0,5);
      
      Util.gblAdd(p, textArea, gbc, 0,1, 1,1, 1,1, 0,5,5,5);  
      
      Panel p2 = new Panel();
      bOpen = new Button("Open File");
      bClose = new Button("Close Window");
      bOpen.addActionListener(this);
      bClose.addActionListener(this);
      
      p2.add(bOpen);
      p2.add(bClose);
      Util.gblAdd(p, p2, gbc, 0,2, 1,1, 1,0, 0,5,5,5);
   }
   
   private void Close()
   {
      setVisible(false);
      mainWindow.CloseLogFileViewerWindow();
      dispose();
   }   
   
   public void setLogFileNum(int p)
   {
      logfilenum = p;
      lTitle.setText("LOG FILE FOR PROCESSOR " + p);
      lTitle.invalidate();
      titlePanel.validate();
   } 
   
   private void ShowDialog()
   {
      oldlogfilenum = logfilenum;
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
      
      setCursor(new Cursor(Cursor.WAIT_CURSOR));
      if(logfilenum != oldlogfilenum)
         textArea.setText(Analysis.getLogFileText(logfilenum));
      setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
   }        
   
   
   public void actionPerformed(ActionEvent evt)
   {
      if(evt.getSource() instanceof MenuItem)
      {
         MenuItem m = (MenuItem)evt.getSource();
         
         if(m.getLabel().equals("Open File"))
            ShowDialog();
         else if(m.getLabel().equals("Close"))
            Close();
      }
      else if(evt.getSource() instanceof Button)
      {
         Button b = (Button)evt.getSource();
         
         if(b == bOpen)
            ShowDialog();
         else if(b == bClose)
            Close();
      }
   }
}


      
