package orc.runtime.transaction;

import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.runtime.regions.TransRegion;
import orc.runtime.values.GroupCell;

/**
 * 
 * Compiled node to capture a publication about to leave a transaction,
 * so that it is not released until the transaction commits.
 * 
 * @author dkitchin
 *
 */
public class AtomicPub extends Node {

	public AtomicPub() {}

	@Override
	public void process(Token t) {
		t.getTransaction().registerPublication(t.getResult());
		t.die();
	}

}
