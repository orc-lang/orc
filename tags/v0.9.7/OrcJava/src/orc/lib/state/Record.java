package orc.lib.state;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

/**
 * @author quark
 */
public class Record extends EvalSite {
	private static class RecordInstance extends EvalSite {
		private HashMap<String, Object> map = new HashMap<String, Object>();
		@Override
		public Object evaluate(Args args) throws TokenException {
			String field = args.fieldName();
			return map.get(field);
		}
		
		public String toString() {
			return map.toString();
		}
		
		private void put(String key, Object value) {
			map.put(key, value);
		}
	}

	@Override
	public Object evaluate(Args args) throws TokenException {
		RecordInstance out = new RecordInstance();
		Iterator<Object> argsi = args.iterator();
		while (argsi.hasNext()) {
			Object keyo = argsi.next();
			String key;
			try {
				key = (String)keyo;
			} catch (ClassCastException e) {
				throw new ArgumentTypeMismatchException(e);
			}
			if (!argsi.hasNext()) {
				throw new ArityMismatchException("Record key missing a value");
			}
			out.put(key, argsi.next());
		}
		return out;
	}
}
