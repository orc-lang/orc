package orc.lib.net;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.dynamic.DynamicClientFactory;

/**
 * SOAP (WSDL) webservice site.
 * 
 * <p>FIXME: this does not work with RPC/encoded SOAP services
 * such as Google Search. We should combine this with Webservice
 * to support both RPC/encoded and Document/literal services.
 * 
 * <p>FIXME: Since we aren't using this yet I haven't committed all
 * of the JARs which it requires.
 * 
 * @author quark
 */
public class CxfWebservice extends ThreadedSite {
	@Override
	public Value evaluate(Args args) throws TokenException {
		DynamicClientFactory dcf = DynamicClientFactory.newInstance();
		final Client client = dcf.createClient(args.stringArg(0),
				CxfWebservice.class.getClassLoader());
		/** Service proxy site */
		return new EvalSite() {
			@Override
			public Value evaluate(Args args) throws TokenException {
				final String methodName = args.fieldName();
				/** Method proxy site */
				return new ThreadedSite() {
					@Override
					public Object evaluate(Args args) throws TokenException {
						try {
							// invoke the service
							Object[] out = client.invoke(methodName, args.asArray());
							// interpret results
							if (out.length == 0) {
								// no result is a signal
								return Value.signal();
							} else if (out.length == 1) {
								// one result is returned as a constant
								return out[0];
							} else {
								// multiple results are a tuple
								return new TupleValue(out);
							}
						} catch (Exception e) {
							throw new JavaException(e);
						}
					}
				};
			}
		};
	}
}
