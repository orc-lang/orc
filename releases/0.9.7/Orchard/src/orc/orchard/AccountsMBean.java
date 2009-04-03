package orc.orchard;

import java.util.Set;

import orc.orchard.jmx.JMXDescription;
import orc.orchard.jmx.JMXParam;


public interface AccountsMBean {
	@JMXDescription("List cached account IDs")
	public Set<Integer> getAccountIDs();
	@JMXDescription("Force old jobs to be cleaned up immediately")
	public void finishOldJobs();
	@JMXDescription("Get the number of cached account IDs")
	public int getNumActiveAccounts();
	@JMXDescription("Create a new account")
	public boolean createAccount(@JMXParam("accountTypeID") int accountTypeID, @JMXParam("username") String username, @JMXParam("password") String password, @JMXParam("email") String email);
	@JMXDescription("Delete an existing account")
	public boolean dropAccount(@JMXParam("username") String username);
	@JMXDescription("Change an account's password")
	public boolean changePassword(@JMXParam("username") String username, @JMXParam("password") String password);
	@JMXDescription("Change an account's developer key")
	public boolean changeDeveloperKey(@JMXParam("username") String username);
}