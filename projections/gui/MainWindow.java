package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class MainWindow extends Frame
   implements ActionListener, ItemListener
{

   private GraphWindow          graphWindow;
   private TimelineWindow       timelineWindow;
   private AnimationWindow      animationWindow;
   private ProfileWindow        profileWindow;
   private HelpWindow           helpWindow;
   private LogFileViewerWindow  logFileViewerWindow;
   private HistogramWindow      histogramWindow;

   private AboutDialog          aboutDialog;

   private MainTitlePanel       titlePanel;
   private MainButtonPanel      buttonPanel;
   private int filetype;
   private boolean toolsEnabled = false;
   private Image paper;

   private static int w, h;
   private Image bgimage;

   public static void main(String args[])
   {
      MainWindow f = new MainWindow();

      Toolkit tk  = Toolkit.getDefaultToolkit();
      Dimension d = tk.getScreenSize();
      w = d.width/3;
      h = d.height/2;
      f.setSize(w, h);
      f.setLocation((d.width-w)/2, (d.height-h)/2);
      f.setTitle("Projections");
      f.setResizable(false);
      f.setVisible(true);
   }

   public MainWindow()
   {
      addWindowListener(new WindowAdapter()
      {
         public void windowClosing(WindowEvent e)
         {
            System.exit(0);
         }
      });

      setBackground(Color.lightGray);

      CreateMenus();
      CreateLayout();
   }

   private void CreateMenus()
   {
      MenuBar mbar = new MenuBar();

      mbar.add(Util.makeMenu("File", new Object[]
      {
         "Open...",
         null,
         "Quit"
      },
      this));

      mbar.add(Util.makeMenu("View", new Object[]
      {
         new CheckboxMenuItem("WindowShade")
      },
      this));


      mbar.add(Util.makeMenu("Tools", new Object[]
      {
         "Graphs",
         "Timelines",
         "Usage Profile",
         "Animations",
         "View Log Files",
         "Histograms"
      },
      this));


      Menu helpMenu;
      mbar.add(helpMenu = Util.makeMenu("Help", new Object[]
      {
         "Index",
         "About"
      },
      this));

      mbar.setHelpMenu(helpMenu);
      setMenuBar(mbar);
   }

   private void CreateLayout()
   {
      URL imageURL = ((Object)this).getClass().getResource("/projections/images/bgimage");
      bgimage = Toolkit.getDefaultToolkit().getImage(imageURL);
      Util.waitForImage(this, bgimage);
      GridBagLayout      gbl = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.BOTH;

      setLayout(gbl);

      titlePanel  = new MainTitlePanel(this);
      buttonPanel = new MainButtonPanel(this);

      Util.gblAdd(this, titlePanel,  gbc, 0,0, 1,1, 1,1,  0, 0, 0, 0);
      Util.gblAdd(this, buttonPanel, gbc, 0,1, 1,1, 1,0, 10,10,10,10);

      pack();
   }
   public void ShowOpenFileDialog()
   {
      buttonPanel.disableButtons();
      FileDialog d = new FileDialog((Frame)this, "Select .sts file to load", FileDialog.LOAD);

      d.setDirectory(".");
	  d.setFilenameFilter(new FilenameFilter() {
	    public boolean accept(File dir, String name) {
	      //System.out.println("Asked to filter "+name);//Never gets called!
	      return name.endsWith(".sts");
	    }
	  });
      d.setFile("pgm.sts");
      d.setVisible(true);

      String filename = d.getFile();
      if(filename == null)
         return;
      filename = d.getDirectory() + filename;

      try
      {
         setCursor(new Cursor(Cursor.WAIT_CURSOR));
         filename = filename.substring(0, filename.length()-4);
         Analysis.initAnalysis(filename);
         buttonPanel.enableButtons();
         toolsEnabled = true;
         setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
      catch(IOException e)
      {
         setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
         InvalidFileDialog ifd = new InvalidFileDialog(this);
         ifd.validate();
         ifd.setVisible(true);
      }
      catch(StringIndexOutOfBoundsException e)
      {
         setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
         InvalidFileDialog ifd = new InvalidFileDialog(this);
         ifd.validate();
         ifd.setVisible(true);
      }
      File name = new File(filename + ".0.sum");
      if (name.isFile())
          filetype = 0;
      else
          filetype = 1;
   }

   public void ShowGraphWindow()
   {
       if(graphWindow == null)
         graphWindow = new GraphWindow(this, filetype);
   }

   public void CloseGraphWindow()
   {
      graphWindow = null;
   }

   public void ShowTimelineWindow()
   {
      if(timelineWindow == null)
         timelineWindow = new TimelineWindow(this);
   }

   public void CloseTimelineWindow()
   {
      timelineWindow = null;
   }

   public void ShowProfileWindow()
   {
      if(profileWindow == null)
         profileWindow = new ProfileWindow(this, null);
      profileWindow.setVisible(true);
   }

   public void CloseProfileWindow()
   {
      profileWindow = null;
   }

   public void ShowAnimationWindow()
   {
      if(animationWindow == null)
         animationWindow = new AnimationWindow();
      animationWindow.setVisible(true);
   }

   public void ShowLogFileViewerWindow()
   {
      if(logFileViewerWindow == null)
         logFileViewerWindow = new LogFileViewerWindow(this);
      logFileViewerWindow.setVisible(true);
   }

   public void CloseLogFileViewerWindow()
   {
      if(logFileViewerWindow != null)
         logFileViewerWindow = null;
   }

   public void ShowHistogramWindow()
   {
      if(histogramWindow == null)
         histogramWindow = new HistogramWindow(this);
      histogramWindow.setVisible(true);
   }

   public void CloseHistogramWindow()
   {
      if(histogramWindow != null)
         histogramWindow = null;
   }

   public void ShowHelpWindow()
   {
      if(helpWindow == null)
         helpWindow = new HelpWindow(this);
      helpWindow.setVisible(true);
   }

   public void ShowAboutDialog(Frame parent)
   {
      if(aboutDialog == null)
         aboutDialog = new AboutDialog(parent);
      aboutDialog.setVisible(true);
   }

   public boolean GraphExists()
   {
      if(graphWindow != null)
         return true;
      else
         return false;
   }

   public Color getGraphColor(int e)
   {
      if(graphWindow != null)
         return graphWindow.getGraphColor(e);
      else
         return null;
   }

   public void actionPerformed(ActionEvent evt)
   {
      if(evt.getSource() instanceof MenuItem)
      {
         MenuItem mi = (MenuItem)evt.getSource();
         String arg = mi.getLabel();

         if(arg.equals("Open..."))
            ShowOpenFileDialog();
         else if(arg.equals("Quit"))
            System.exit(0);
         else if(arg.equals("Index"))
            ShowHelpWindow();
         else if(arg.equals("About"))
            ShowAboutDialog(this);

         else if(toolsEnabled)
         {
            if(arg.equals("Graphs"))
               ShowGraphWindow();
            else if(arg.equals("Timelines"))
               ShowTimelineWindow();
            else if(arg.equals("Animations"))
               ShowAnimationWindow();
            else if(arg.equals("Usage Profile"))
               ShowProfileWindow();
            else if(arg.equals("View Log Files"))
               ShowLogFileViewerWindow();
            else if(arg.equals("Histograms"))
               ShowHistogramWindow();
         }
      }
   }

   public void itemStateChanged(ItemEvent evt)
   {
      CheckboxMenuItem c = (CheckboxMenuItem)evt.getSource();
      if(c.getState() == false)
      {
         setSize(w, h);
      }
      else
      {
         int inTop = getInsets().top;
         setSize(w, inTop);
      }
      validate();
   }

   public void update(Graphics g)
   {
      paint(g);
   }

   public void paint(Graphics g)
   {
      Util.wallPaper(this, g, bgimage);
      super.paint(g);
   }

}



