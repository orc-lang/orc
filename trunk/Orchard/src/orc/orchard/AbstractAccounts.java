package orc.orchard;

import java.util.HashMap;
import java.util.Map;

/**
 * Provide access to accounts stored in a database.
 * @author quark
 */
public abstract class AbstractAccounts {
	private final Map<Integer, Account> accounts = new HashMap<Integer, Account>();
	protected final Account guest;
	public AbstractAccounts() {
		guest = new GuestAccount();
	}
	
	public abstract Account getAccount(String devKey);
	
	protected synchronized Account getAccount(final Integer account_id, Integer quota, Integer lifespan, Integer eventBufferSize) {
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
				public boolean getIsGuest() { return false; }
			};
			accounts.put(account_id, out);
		}
		out.setQuota(quota);
		out.setLifespan(lifespan);
		out.setEventBufferSize(eventBufferSize);
		return out;
	}
}
