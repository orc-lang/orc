package orc.orcx;

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
public class SendSite extends Site {
	public SendSite() {
	}
	
	public void callSite(Args args, Token caller) {
		if (args.getValues().size() != 2)
			throw new Error("Send(address, data)");
		if (!(args.valArg(0) instanceof Constant) || !(args.valArg(1) instanceof Constant) 
			|| !(((Constant)args.valArg(0)).getValue() instanceof String)
			|| !(((Constant)args.valArg(1)).getValue() instanceof String))
			throw new Error("Both arguments to 'Send' must be a string");
		
		/* Send content directly to the receiver's HTTP Server */
		String address = (String)(((Constant)args.valArg(0)).getValue());
		String data = (String)(((Constant)args.valArg(1)).getValue());
		HTTPUtils.sendData(address, "/", data);
	}
}