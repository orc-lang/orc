package orc.lib.state;

import java.util.LinkedList;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.SemaphoreType;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 * @author quark
 */
public class Semaphore extends EvalSite {

	@Override
	public Object evaluate(Args args) throws TokenException {
		return new SemaphoreInstance(args.intArg(0));
	}
	
	public Type type() throws TypeException {
		return new ArrowType(Type.INTEGER, new SemaphoreType());
	}
	
	protected class SemaphoreInstance extends DotSite {

		private LinkedList<Token> waiters = new LinkedList<Token>();
		private LinkedList<Token> snoopers = new LinkedList<Token>();
		private int n;

		SemaphoreInstance(int n) {
			this.n = n;
		}
		
		@Override
		protected void addMembers() {
			addMember("acquire", new Site() {
				public void callSite(Args args, Token waiter) {
					if (0 == n) {
						waiter.setQuiescent();
						waiters.addLast(waiter);
						if (!snoopers.isEmpty()) {
							for (Token snooper : snoopers) {
								snooper.unsetQuiescent();
								snooper.resume();
							}
							snoopers.clear();
						}
					} else {
						--n;
						waiter.resume();
					}
				}
			});	
			addMember("acquirenb", new Site() {
				public void callSite(Args args, Token waiter) {
					if (0 == n) {
						waiter.die();
					} else {
						--n;
						waiter.resume();
					}
				}
			});	
			addMember("release", new Site() {
				@Override
				public void callSite(Args args, Token sender) throws TokenException {
					if (waiters.isEmpty()) {
						++n;
					} else {
						Token waiter = waiters.removeFirst();
						waiter.unsetQuiescent();
						waiter.resume();
					}
					sender.resume();
				}
			});
			addMember("snoop", new Site() {
				@Override
				public void callSite(Args args, Token snooper) throws TokenException {
					if (waiters.isEmpty()) {
						snooper.setQuiescent();
						snoopers.addLast(snooper);
					} else {
						snooper.resume();
					}
				}
			});
			addMember("snoopnb", new Site() {
				@Override
				public void callSite(Args args, Token token) throws TokenException {
					if (waiters.isEmpty()) {
						token.die();
					} else {
						token.resume();
					}
				}
			});
		}
	}
}
