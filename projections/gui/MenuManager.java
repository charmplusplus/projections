package projections.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/* ***********************************************************
 * MenuManager.java
 * Chee Wai Lee - 10/30/2002
 *
 * This class is intended to be a class from which various windows'
 * menus can inherit from to perform the window-specific menu item
 * updates that the static Util class cannot provide for.
 *
 * Implementing classes should employ listeners to other GUI
 * objects (and themselves) in order to update the appropriate
 * menu items. The default actionlistener does nothing.
 *
 * See MainMenu.java as an example.
 * ***********************************************************/

public abstract class MenuManager 
    implements ActionListener, ItemListener
{
    JMenuBar menubar;
    JFrame parent;

    public MenuManager(JFrame parent) {
	this.parent = parent;
	menubar = new JMenuBar();
	parent.setJMenuBar(menubar);
    }

    // Model of operation:
    // The implementer creates menu items from a bottom-up manner, creating
    // a list of leaf entries before attaching it to higher level lists.
    //
    // These lists should be kept around by the implementing object for
    // accesses to:
    //     i) update the list
    //     ii) disable entries
    //
    // Enabling and disabling menu items are performed using boolean arrays
    // corresponding to the individual lists.
    //
    JMenu makeJMenu(Object parent, Object[] items)
    {  
	JMenu m = null;
	if (parent instanceof JMenu)
	    m = (JMenu)parent;
	else if (parent instanceof String)
	    m = new JMenu((String)parent);
	else
	    return null;
	
	for (int i=0; i<items.length; i++) {  
	    if (items[i] instanceof String) {  
		JMenuItem mi = new JMenuItem((String)items[i]);
		mi.addActionListener(this);
		m.add(mi);
	    } else if (items[i] instanceof JCheckBoxMenuItem) {
		JCheckBoxMenuItem cmi = (JCheckBoxMenuItem)items[i];
		cmi.addItemListener(this);
		m.add(cmi);
	    } else if (items[i] instanceof JRadioButtonMenuItem) {  
		JRadioButtonMenuItem cmi = (JRadioButtonMenuItem)items[i];
		cmi.addActionListener(this);
		m.add(cmi);
	    } else if (items[i] instanceof JMenuItem) {  
		JMenuItem mi = (JMenuItem)items[i];
		mi.addActionListener(this);
		m.add(mi);
	    } else if (items[i] == null) {
		m.addSeparator();
	    }
	}
	return m;
    }

    void setEnabled(JMenu menu, boolean enableItems[]) {
	Component menuEntry;
	// Note: the count includes separators. Separators should never
	//       be enabled.
	if (enableItems.length != menu.getItemCount()) {
	    System.err.println("Projections Error: Menu items inconsistent!");
	    System.exit(-1);
	} else {
	    for (int i=0; i<enableItems.length; i++) {
		// JSeparators are not menu items.
		// crashes Java horribly.
		menuEntry = menu.getItem(i);
		if (menuEntry instanceof JMenuItem) {
		    ((JMenuItem)menuEntry).setEnabled(enableItems[i]);
		}
	    }
	}
    }

    // convenience method for setting everything to one of the boolean values.
    void setAllTo(JMenu menu, boolean setTo) {
	Component menuEntry;
	for (int i=0; i<menu.getItemCount(); i++) {
	    menuEntry = menu.getItem(i);
	    if (menuEntry instanceof JMenuItem) {
		((JMenuItem)menuEntry).setEnabled(setTo);
	    }
	}
    }

    // The inheriting class should implement interface methods with the
    // parent frame that influences the MenuManager's state. The parent
    // frame should also implement methods to handle reactions to changes
    // in the menu state.

    // This abstract method performs the action whenever the MenuManager's
    // state has changed. The state should be reflected in private static
    // final variables defined in the inheritors.
    abstract void stateChanged(int state);

    // The actions should use stateChanged to make appropriate changes to
    // the menu with respect to local events.

    public void actionPerformed(ActionEvent e) {
	// do nothing - Inheriting object should override.
    }

    public void itemStateChanged(ItemEvent e) {
	// do nothing - Inheriting object should override.
    }
}
