package orc.error.runtime;

import orc.runtime.sites.Site;
import orc.runtime.transaction.Transaction;

public class NontransactionalSiteException extends TokenException {

	public NontransactionalSiteException(Site s, Transaction t) {
		super("Site " + s + " cannot participate in transaction " + t);
	}

}
