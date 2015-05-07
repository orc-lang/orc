package orc.lib.state;

import java.util.LinkedList;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PartialSite;
import orc.runtime.sites.Site;

/**
 * Factory for counters. 
 * @author quark
 */
public class Counter extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		final int init = (args.size() == 0) ? 0 : args.intArg(0);
		return new DotSite() {
			private int count = init;
			private LinkedList<Token> waiters = new LinkedList<Token>();
			@Override
			protected void addMembers() {
				addMember("inc", new EvalSite() {
					@Override
					public Object evaluate(Args args) throws TokenException {
						++count;
						return signal();
					}
				});
				addMember("dec", new PartialSite() {
					@Override
					public Object evaluate(Args args) throws TokenException {
						if (count > 0) {
							--count;
							if (count == 0) {
								for (Token waiter : waiters) waiter.resume();
								waiters.clear();
							}
							return signal();
						} else return null;
					}
				});
				addMember("onZero", new Site() {
					@Override
					public void callSite(Args args, Token caller) throws TokenException {
						if (count == 0) caller.resume();
						else waiters.add(caller);
					}
				});
			}
		};
	}
}
