package orc.lib.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedSite;

public class TrueRandom extends ThreadedSite {
	private static String baseURL = "http://www.random.org/integers/?num=1&col=1&base=10&format=plain&rnd=new";
	@Override
	public Object evaluate(Args args) throws TokenException {
		try {
			String number = HTTPUtils.getURL(
					new URL(baseURL
						+ "&min=" + args.longArg(0)
						+ "&max=" + (args.longArg(1)-1)));
			return new Long(number.trim());
		} catch (MalformedURLException e) {
			throw new JavaException(e);
		} catch (IOException e) {
			throw new JavaException(e);
		}
	}
}
