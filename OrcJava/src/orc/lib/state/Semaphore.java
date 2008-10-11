package orc.lib.state;

import java.util.LinkedList;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;

/**
 * @author quark
 */
public class Semaphore extends EvalSite {

	@Override
	public Object evaluate(Args args) throws TokenException {
		return new SemaphoreInstance(args.intArg(0));
	}
	
	
	protected class SemaphoreInstance extends DotSite {

		private LinkedList<Token> waiters = new LinkedList<Token>();
		private int n;

		SemaphoreInstance(int n) {
			this.n = n;
		}
		
		@Override
		protected void addMethods() {
			addMethod("acquire", new Site() {
				public void callSite(Args args, Token waiter) {
					if (0 == n) {
						waiters.addLast(waiter);
					} else {
						--n;
						waiter.resume();
					}
				}
			});	
			addMethod("release", new Site() {
				@Override
				public void callSite(Args args, Token sender) throws TokenException {
					Object item = args.getArg(0);
					if (waiters.isEmpty()) {
						++n;
					} else {
						Token waiter = waiters.removeFirst();
						waiter.resume(item);
					}
					sender.resume();
				}
			});
		}
	}
}
