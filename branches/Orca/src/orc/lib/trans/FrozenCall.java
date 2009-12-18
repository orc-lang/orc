package orc.lib.trans;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.transaction.Transaction;

class FrozenCall {
		
		Args args;
		Token token;
		Transaction transaction;
		Site site;
	
		public FrozenCall(Args args, Token token, Transaction transaction,
				Site site) {
			this.args = args;
			this.token = token;
			this.transaction = transaction;
			this.site = site;
		}
		
		public void unfreeze() {
			try {
				site.callSite(args, token, transaction);
			} catch (TokenException e) {
				token.error(e);
			}
		}
	}