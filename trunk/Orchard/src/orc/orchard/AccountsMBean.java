package orc.orchard;

import java.util.Set;

public interface AccountsMBean {
	public Set<Integer> getAccountIDs();
	public void finishOldJobs();
	public int getNumActiveAccounts();
}