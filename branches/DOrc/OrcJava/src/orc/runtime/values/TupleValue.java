/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.List;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;

/**
 * A tuple value container
 * @author wcook, quark
 */
public class TupleValue extends Site {
	public TupleValue() {
		super(new TupleValueSite());
	}
	public TupleValue(Value v, Value w) {
		super(new TupleValueSite(v, w));
	}
	public TupleValue(List<Value> values) {
		super(new TupleValueSite(values));
	}
	public TupleValue(Value[] values) {
		super(new TupleValueSite(values));
	}
	/**
	 * Actual site implementation.
	 * FIXME: this is sort of a hack, ideally this would
	 * be an inner class but then we couldn't pass it to
	 * the Site constructor.
	 * @author quark
	 */
	private static class TupleValueSite extends EvalSite implements PassedByValueSite {
		Value[] values;
		public TupleValueSite() {
			this.values = new Value[0];
		}
		public TupleValueSite(Value v, Value w) {
			this.values = new Value[2];
			this.values[0] = v;
			this.values[1] = w;
		}		
		public TupleValueSite(List<Value> values) {
			this.values = new Value[values.size()];
			this.values = values.toArray(this.values);
		}
		public TupleValueSite(Value[] values) {
			this.values = values;
		}
		// A tuple may be used as a site for array lookup
		public Value evaluate(Args args) throws OrcRuntimeTypeException	{
			// TODO: Generalize this treatment of dot sites.
			try { 
				String s = args.fieldName();
				if (s.equals("fits")) {
					return new Site(new FitSite(values.length));
				}
			} catch (OrcRuntimeTypeException e) {
				// do nothing
			}
			return values[args.intArg(0)];
		} 
	}

	static class FitSite extends EvalSite {
		int size;
		public FitSite(int size) {
			this.size = size;
		}
		public Value evaluate(Args args) throws OrcRuntimeTypeException {
			return new Constant(args.intArg(0) == this.size);
		}
	}
	
	public Value at(int i) {
		return ((TupleValueSite)this.site).values[i];
	}
	
	public int size() {
		return ((TupleValueSite)this.site).values.length;
	}

	public String toString() {
		return format('(', ((TupleValueSite)this.site).values, ", ", ')');
	}
	
	public static String format(char left, Object[] items, String sep, char right) {
		StringBuffer buf = new StringBuffer();
		buf.append(left);
		for (int i = 0; i < items.length; ++i) {
			if (i > 0) buf.append(sep);
			buf.append(items[i].toString());
		}
		buf.append(right);
		return buf.toString();
	}
}