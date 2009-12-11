package projections.analysis;

import java.util.Hashtable;
import java.util.Stack;

/**
 *  CallStackManager is essentially an interface to projections to 
 *  conveniently handle function calls from multiple array elements.
 */
public class CallStackManager extends Hashtable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Stack activeStack;

    // Public interface to CallStackManager
    public void push(Object data, int id1, int id2, int id3) {
	activeStack = (Stack)get(tripleToKey(id1, id2, id3));
	if (activeStack == null) {
	    activeStack = new Stack();
	    put(tripleToKey(id1, id2, id3), activeStack);
	}
	activeStack.push(data);
    }

    public Object pop(int id1, int id2, int id3) {
	// NOTE: there's no need to remove the stack if it is empty, since
	// we know the thread id exists and is likely to be used again.
	activeStack = (Stack)get(tripleToKey(id1, id2, id3));
	if (activeStack == null) {
	    return null;
	}
	if (activeStack.empty()) {
	    return null;
	}
	return activeStack.pop();
    }

    public Object read(int id1, int id2, int id3) {
	activeStack = (Stack)get(tripleToKey(id1, id2, id3));
	if (activeStack == null) {
	    return null;
	}
	if (activeStack.empty()) {
	    return null;
	}
	return activeStack.peek();
    }

    /**
     *  Acquire a copy of the current stack. This does not have to
     *  be a deep copy.
     */
    protected Stack getStack(int id1, int id2, int id3) {
	activeStack = (Stack)get(tripleToKey(id1, id2, id3));
	if (activeStack == null) {
	    return null;
	}
	if (activeStack.empty()) {
	    return null;
	}
	return (Stack)activeStack.clone();
    }

    // Private methods for specialized stack functionality
 
    /**
     *  for converting the thread ids to a hash key
     */
    private Object tripleToKey(int id1, int id2, int id3) {
	String newkey = "[0]" + String.valueOf(id1) + "[1]" + 
	    String.valueOf(id2) + "[2]" + String.valueOf(id3);

	return newkey;
    }

    /**
     *   Simple testing routines.
     */
    public static void main(String args[]) {
	CallStackManager cs = new CallStackManager();
	Stack stack1, stack2;

	cs.push("Hello", 3,4,5);
	cs.push("There", 3,4,5);
	cs.push("You", 3,4,5);
	stack1 = cs.getStack(3,4,5);
	cs.pop(3,4,5);
	stack2 = cs.getStack(3,4,5);
	cs.push("Dummy", 3,4,5);
	System.out.println("Stack1 top = " + (String)stack1.pop());
	System.out.println("Stack2 top = " + (String)stack2.pop());
	System.out.println("Current top = "+(String)cs.getStack(3,4,5).pop());
    }
}
