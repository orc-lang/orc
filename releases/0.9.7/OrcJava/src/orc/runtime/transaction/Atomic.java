package orc.runtime.transaction;

import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.values.GroupCell;

/**
 * 
 * Compiled node to initiate a transaction.
 * 
 * @author dkitchin
 *
 */
public class Atomic extends Node {

	Node body; // must end with a Store node
	Node output;
	
	public Atomic(Node body, Node output) {
		this.body = body;
		this.output = output;
	}

	@Override
	public void process(Token t) {
		GroupCell cell = new GroupCell(t.getGroup(), t.getTracer().pull());
		Transaction trans = new Transaction(t, output, cell);
		TransRegion region = new TransRegion(t.getRegion(), trans);
		cell.setTransaction(trans);
		try {
			t.fork(cell, region).setTransaction(trans).move(body).activate();
		} catch (TokenLimitReachedError e) {
			t.error(e);
		}
	}

}
