/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites;

import java.util.Random;

import orc.runtime.DotAccessible;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.Tuple;
import orc.runtime.values.Record;

/**
 * Helper class defining many basic sites.
 * @author wcook
 */
public class Calc extends Site {

	public enum Op { 
		ADD, SUB, MUL, DIV, 
		LT, LE, EQ, NE, GE, GT,
		AND, OR, NOT, 
		IF, 
		RAND,
		ITEM, DOT,
		CAT,
		PRINT, PRINTLN
		} ;
	Op op;
	
	Random random = new Random();
	
	public Calc(Op op) {
		this.op = op;
	}

	/**
	 * Performs the computation for all the basic calculation sites. 
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	void callSite(Object[] args, Token returnToken, OrcEngine engine) {

		int n = 2;
		Object result = null;
		switch (op) {
			case CAT: {
				StringBuffer buf = new StringBuffer();
				for (Object x : args)
					buf.append(x.toString());
				result = buf.toString();
				n = args.length;
				break;
			}

			case ADD: result = Integer.valueOf(intArg(args, 0) + intArg(args, 1)); break;
			case SUB: result = Integer.valueOf(intArg(args, 0) - intArg(args, 1)); break;
			case MUL: result = Integer.valueOf(intArg(args, 0) * intArg(args, 1)); break;
			case DIV: result = Integer.valueOf(intArg(args, 0) / intArg(args, 1)); break;

			case LE: result = Boolean.valueOf(intArg(args, 0) <= intArg(args, 1)); break;
			case LT: result = Boolean.valueOf(intArg(args, 0) < intArg(args, 1)); break;
			case EQ: result = Boolean.valueOf(intArg(args, 0) == intArg(args, 1)); break;
			case NE: result = Boolean.valueOf(intArg(args, 0) != intArg(args, 1)); break;
			case GT: result = Boolean.valueOf(intArg(args, 0) > intArg(args, 1)); break;
			case GE: result = Boolean.valueOf(intArg(args, 0) >= intArg(args, 1)); break;
			
			case RAND: result = Integer.valueOf(random.nextInt(intArg(args, 0))); n = 1; break;

			case AND: result = Boolean.valueOf(boolArg(args, 0) && boolArg(args, 1)); break;
			case OR: result = Boolean.valueOf(boolArg(args, 0) || boolArg(args, 1)); break;
			case NOT: result = Boolean.valueOf(!boolArg(args, 0)); n = 1; break;

			case IF: {
				if (boolArg(args, 0))
					result = true;
				n = 1;
				break;
			}
			
			case ITEM: {
				Object v = args[0];
				int m = intArg(args, 1);
				n = 2;
				if (v instanceof String)
					if (args.length == 3)
					{
						n = 3;
						result = ((String)v).substring(m, intArg(args, 2));
					}
					else 
						result = ((String)v).substring(m, m + 1);
				else if (v instanceof Tuple)
					result = ((Tuple)v).at(m);
				else
					throw new Error("Invalid item access");
				break;
			}
			case DOT: {
				Object r = args[0];
				String f = stringArg(args, 1);
				if (r instanceof DotAccessible)
				{	
					result = ((DotAccessible)r).dotAccessibleVal(f);
				}
				else
					throw new Error("Attempted dot access into non-record value");
				break;
			}
			
			case PRINTLN:
			case PRINT: {
				for (Object x : args)
					System.out.print(x.toString());
				if (op == Op.PRINTLN)
					System.out.println();
				n = args.length;
				result = true;
				break;
			}
		}

		if (args.length != n)
			throw new Error("Expected " + n + " arguments for " + op + args);

		if (result != null) {
			returnToken.setResult(result);
			engine.activate(returnToken);
		}
	}

}
