package orc.orchard;

import java.util.HashMap;
import java.util.Map;

import org.postgresql.util.PGInterval;

/**
 * Provide access to accounts stored in a database.
 * @author quark
 */
public abstract class AbstractAccounts {
	private Map<Integer, Account> accounts = new HashMap<Integer, Account>();
	protected Account guest;
	public AbstractAccounts() {
		// create a special persistent guest account
		guest = new Account() {
			@Override
			protected void onNoMoreJobs() {}	
			public boolean isGuest() { return true; }
		};
		guest.setLifespan(new PGInterval(0, 0, 1, 0, 0, 0));
	}
	
	public abstract Account getAccount(String devKey);
	
	protected synchronized Account getAccount(final Integer account_id, Integer quota, PGInterval lifespan, Integer eventBufferSize) {
		// Get the actual account object. If one already
		// exists, return it.
		Account out;
		if (accounts.containsKey(account_id)) {
			out = accounts.get(account_id);
		} else {
			out = new Account() {
				@Override
				protected void onNoMoreJobs() {
					synchronized (AbstractAccounts.this) {
						// remove the account from the cache
						accounts.remove(account_id);
					}
				}
				public boolean isGuest() { return false; }
			};
			accounts.put(account_id, out);
		}
		out.setQuota(quota);
		out.setLifespan(lifespan);
		out.setEventBufferSize(eventBufferSize);
		return out;
	}
}
