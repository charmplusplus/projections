package projections.gui;

import javax.swing.*;
import java.awt.event.*;

/** Joshua Unger, unger1@uiuc.edu, 07.05.2002<p>
 * 
 *  A button that sets the "Wait" class to false when pressed. 
 *  See projections.gui.Wait for usage. */
public class WaitButton extends JButton {
  public WaitButton(String title, Wait w) {
    super(title);
    wait_ = w;
    super.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) { wait_.setValue(false); }
    });
  }
  private Wait wait_ = null;
}
