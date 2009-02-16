package orc.lib.data;

import java.util.HashMap;
import java.util.Map;

import orc.error.runtime.TokenException;
import orc.lib.state.Ref;
import orc.lib.state.Ref.RefInstance;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

/**
 * @author quark
 */
public class Dictionary extends EvalSite {
	private static class DictionaryInstance extends EvalSite {
		private HashMap<String, RefInstance> map = new HashMap<String, RefInstance>();
		@Override
		public Object evaluate(Args args) throws TokenException {
			String field = args.fieldName();
			RefInstance out = map.get(field);
			if (out == null) {
				out = new RefInstance();
				map.put(field, out);
			}
			return out;
		}
		
		public String toString() {
			return map.toString();
		}
	}

	@Override
	public Object evaluate(Args args) throws TokenException {
		return new DictionaryInstance();
	}
}
