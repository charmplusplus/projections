package projections.misc;

/**
 *
 *  Written by Chee Wai Lee
 *  3/27/2002
 *
 *  CommandLineException is used to report an invalid commandline input.
 *
 */

public class CommandLineException extends java.lang.Exception {
    public CommandLineException(String info) {
	super(info);
    }
}
