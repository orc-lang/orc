package orc.orcx;

import java.util.concurrent.TimeUnit;

import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Constant;


/**
 * 
 * Site object for publishing data received from the HTTP Server used for
 * communication between Orc nodes.
 * 
 * @author kmorton
 *
 */
public class ReceiveSite extends Site {
	Token caller = null;
	public ReceiveSite() {
	}
	
	public void callSite(Args args, Token caller) {
		/* Read or block from the BlockingQueue used to pump data from the HTTP Server */
		this.caller = caller;
		Thread receiver = new Thread(new ReceiveThread());
		receiver.start();
	}
	
	private class ReceiveThread implements Runnable {
		public void run() {
			try {
				String content = OrcHTTPServer.queue.take();
				caller.resume(new Constant(content));
			}
			catch (Exception e) {
				System.err.println("Error: Attempted to read from OrcX send/receive queue, received exception:");
				System.err.println(e);
			}		
		}
		
	}
}
