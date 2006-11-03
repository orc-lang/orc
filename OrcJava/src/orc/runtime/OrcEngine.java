/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.util.LinkedList;
import java.util.TreeMap;

import orc.runtime.nodes.Node;
import orc.runtime.sites.Calc;
import orc.runtime.sites.Let;
import orc.runtime.sites.Mail;
import orc.runtime.sites.Rtimer;
import orc.runtime.values.BaseValue;
import orc.runtime.values.Constant;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Record;

/**
 * The Orc Engine provides the main look for executing active tokens.
 * @author wcook
 */
public class OrcEngine {

	LinkedList<Token> activeTokens = new LinkedList<Token>();
	LinkedList<Token> queuedReturns = new LinkedList<Token>();
	int calls;
	public boolean debugMode = false;

	/**
	 * Run Orc given a root node.
	 * Creates an initial environment and then 
	 * executes the main loop.
	 * @param root  node to run
	 */
	public void run(Node root) {

		GroupCell startGroup = new GroupCell();
		Token start = new Token(root, null/*env*/, null/* caller */, startGroup,
				null/* value */);

		start.bind("let", new Let());

		start.bind("cat", new Calc(Calc.Op.CAT));

		start.bind("add", new Calc(Calc.Op.ADD));
		start.bind("sub", new Calc(Calc.Op.SUB));
		start.bind("mul", new Calc(Calc.Op.MUL));
		start.bind("div", new Calc(Calc.Op.DIV));

		start.bind("lt", new Calc(Calc.Op.LT));
		start.bind("le", new Calc(Calc.Op.LE));
		start.bind("eq", new Calc(Calc.Op.EQ));
		start.bind("ne", new Calc(Calc.Op.NE));
		start.bind("ge", new Calc(Calc.Op.GE));
		start.bind("gt", new Calc(Calc.Op.GT));

		start.bind("and", new Calc(Calc.Op.AND));
		start.bind("or", new Calc(Calc.Op.OR));
		start.bind("not", new Calc(Calc.Op.NOT));

		start.bind("clock", new Calc(Calc.Op.CLOCK));
		start.bind("random", new Calc(Calc.Op.RAND));
		start.bind("if", new Calc(Calc.Op.IF));

		start.bind("item", new Calc(Calc.Op.ITEM));
		start.bind("dot", new Calc(Calc.Op.DOT));
		start.bind("print", new Calc(Calc.Op.PRINT));
		start.bind("println", new Calc(Calc.Op.PRINTLN));

		start.bind("Rtimer", new Rtimer(false/*relative*/));
		start.bind("Atimer", new Rtimer(true/*absolute*/));
		try {
			start.bind("SendMail", new Mail());
		} catch (Error e) {
			System.err.println("Warning: mail not avaiable (" + e + ")");
		}

		start.bind("true", new Constant(Boolean.TRUE));
		start.bind("false", new Constant(Boolean.FALSE));
		
		{
			TreeMap<String,BaseValue> m = new TreeMap<String,BaseValue>();
			m.put("zero",new Constant(0));
			m.put("one",new Constant(1));
			m.put("two",new Constant(2));
			start.bind("theCount", new Record(m));
		}
		
		activeTokens.add(start);

		int round = 1;
		while (moreWork()) {
			if (debugMode)
				debug("** Round " + (round++) + " ***", null);

			while (activeTokens.size() > 0)
				activeTokens.remove().process(this);

			if (queuedReturns.size() > 0)
				activeTokens.add(queuedReturns.remove());
		}
	}
	
	/**
	 * Internal function to check if there is more work to do
	 * @return true if more work
	 */
	private synchronized boolean moreWork() {
		if (activeTokens.size() == 0) {
			if (calls == 0)
				return false;
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		return true;
	}

	/**
	 * Activate a token by adding it to the queue of active tokens
	 * @param t	the token to be added
	 */
	synchronized public void activate(Token t) {
		activeTokens.addLast(t);
		notify();
	}

	/**
	 * Counts how many calls have been made
	 * TODO: this is a hack only needed to identify when Orc
	 * can terminate. Normally an Orc execution would terminate
	 * when the first value is produced, and this count would
	 * not be needed.
	 * @param n
	 */
	public void addCall(int n) {
		calls += n;
	}

	//Added by Pooja Gupta
	synchronized public void removeCall()
	{
		calls -=1;
		notify();
	}
	
	/**
	 * Called when a site returns a value. Add the corresponding
	 * token to queue of returned sites
	 * @param label
	 * @param token
	 * @param value
	 */
	synchronized public void siteReturn(String label, Token token,
			Object value) {
		token.setResult(value);
		queuedReturns.add(token);
		if (debugMode)
			debug("ASYMC: " + label + " returned: " + value, token);
		notify(); // wake up main thread
	}

	public void debug(String string, Token token) {
		// if (token != null)
		// System.out.print("[" + token.hashCode() + "] ");
		System.out.println(string);
	}
}
