package orc.orchard.java;

import orc.orchard.AbstractExecutorService;
import orc.orchard.GuestOnlyAccounts;

public class ExecutorService extends AbstractExecutorService {
	public ExecutorService() {
		super(new GuestOnlyAccounts());
	}
}