/*
 Send one request to the CCS server.
 Orion Sky Lawlor, olawlor@acm.org, 10/7/2004
 */
package projections.ccs;

import java.io.*;

import projections.ccs.CcsServer;
//import java.util.*;

public class PokeCCS
{
    public static void main(String args[]) {
    	if (args.length!=3 && args.length!=4) {
		System.out.println("Usage: java client <host> <port> "+
			"<ccs handler to call> [<pe>]");
		System.exit(1);
	}
    	String[] ccsArgs=new String[2];
	ccsArgs[0]=args[0]; ccsArgs[1]=args[1];
	CcsServer ccs=CcsServer.create(ccsArgs,false);
	System.out.println("Connected: The CCS server has "+ccs.getNumPes()+" processors."); 
	
	String handler=args[2];
        int forPE=0;
        if (args.length>3) forPE=Integer.parseInt(args[3]);
	try {
		CcsServer.Request r=ccs.sendRequest(handler,forPE,null);
 		byte[] resp=ccs.recvResponse(r);
		System.out.println("Sent request to "+handler+" and got back "+resp.length+" bytes");
	} catch(IOException e) {
		e.printStackTrace();
	}
    }
};
