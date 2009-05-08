package projections.streaming;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import projections.ccs.CcsProgress;
import projections.ccs.CcsThread;

public class StreamingDataHandler {

	CcsThread ccs;


	public class progressHandler implements CcsProgress {
		public progressHandler(){

		}

		public void setText(String s) {
			System.out.println("someone called setText(" + s + ")");
		}
	}


	private class dataRequest extends CcsThread.request{
		// store calling panel here if needed
		public dataRequest() {
			super("CkPerfSummaryCcsClientCB",0);
		}

		public void handleReply(byte[] data){
			System.out.println("Received " + data.length + " byte data array\n");
			ccs.addRequest(this);
		}

	}


	/** Constructor */	
	StreamingDataHandler(){

		System.out.println("StreamingDataHandler constructor");

		String server = "localhost";
		int port = 1234;

		progressHandler h = new progressHandler();
		ccs = new CcsThread(h,server,port);

		/** Create first request */
		ccs.addRequest(new dataRequest());

	}



}
