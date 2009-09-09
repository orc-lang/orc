package orc.runtime.transaction;

import orc.runtime.Token;

public interface Cohort {

	// Resume t when ready to commit to trans
	// If not ready, tell trans to abort
	public void ready(Token t, Transaction trans);
	
	// Resume t when finished committing to trans
	public void confirm(Token t, Transaction trans);

	// Resume t when finished reverting trans
	public void rollback(Token t, Transaction trans);
}
