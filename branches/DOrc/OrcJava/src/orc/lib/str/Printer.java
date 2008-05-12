/**
 * 
 */
package orc.lib.str;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.RemoteToken;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * This allows you to create print/println sites which are evaluated relative to
 * the server they were created on (in a distributed computation) rather than
 * the current server.
 * 
 * @author quark
 */
public class Printer extends EvalSite implements PassedByValueSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Value evaluate(Args args) {
		return new orc.runtime.values.Site(new PrinterInstance());
	}

	private static class PrinterInstance extends DotSite implements PassedByValueSite {
		@Override
		protected void addMethods() {
			addMethod("print", new Print());	
			addMethod("println", new Println());
		}
		private static class Print extends EvalSite {
			@Override
			public Value evaluate(Args args) throws OrcRuntimeTypeException {
				for(int i = 0; i < args.size(); i++) {
					System.out.print(args.stringArg(i));
				}	
				return Value.signal();
			}
		}
		private static class Println extends EvalSite {
			@Override
			public Value evaluate(Args args) throws OrcRuntimeTypeException {
				for(int i = 0; i < args.size(); i++) {
					System.out.println(args.stringArg(i));
				}		
				if (args.size() == 0) System.out.println();
				return Value.signal();
			}
		}
	}
}