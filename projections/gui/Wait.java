package projections.gui;

/** Joshua Unger, unger1@uiuc.edu, 07.05.2002<p>
 *
 * Wait is a class that allows a sort of "BLOCK" until user does something
 * in your code.<p>
 *
 * For example, say you want to pop up a window and then wait until the window
 * is closed, then take the results of something that the user did in the 
 * window.  Use the following code:<p>
 *
 * <pre>
 * public class mainClass {<br>
 *   public mainClass() {<br>
 *     TextArea t = new TextArea("YourNameHere");<br>
 *     GetTextWindow w = new GetTextWindow(t, wait_);<br>  
 *     // GetTextWindow:<br>
 *     // +----------------+<br>
 *     // | Type your name:|<br>
 *     // | "YourNameHere" |<br>
 *     // |   +--------+   |<br>
 *     // |   |Finished|   |<br>
 *     // |   +--------+   |<br>
 *     // +----------------+<br>
 *     // GetTextWindow cannot be modal!<br>
 *     // Finished is of type WaitButton, initialized by:<br>
 *     //   new WaitButton("Finished", wait_);
 *     // When "Finished" pressed, window calls wait_.setValue(false);<br>
 *     w.setVisible(true);<br>
 *     wait_.setValue(true);<br>
 *     Thread thread = new Thread() {<br>
 *       public void run() { wait_.waitFor(false); }<br>
 *     }<br>
 *     thread.run();<br>
 *     System.out.println("Your name is "+t.getText());<br>
 *   }<br>
 *   public final static main(String[] args) {<br>
 *     mainClass m = new mainClass();<br>
 *   }<br>
 *   private Wait wait_ = new Wait(true);<br>
 * }<br>
 * </pre>
 */
public class Wait {
  /** Constructor. Set initial value of wait. */
  public Wait(boolean val) { value_ = val; }
  /** The thread calling this function will block until another thread calls
   *  setValue(<value>). If the value of the Wait is <value> when called
   *  thread will return immediately. */
  public synchronized void waitFor(boolean val) {
    while (value_ != val) {
      if (value_ != val) { 
	try { wait(); } catch (InterruptedException ie) { } 
      }
      else { return; }
    }
  }
  /** Sets value of wait to <val>. */
  public synchronized void setValue(boolean val) {
    value_ = val;
    notifyAll();
  }
  /** Return current setting of the Wait class. */
  public boolean getValue() { return value_; }

  private boolean value_ = true;  // the value
}
