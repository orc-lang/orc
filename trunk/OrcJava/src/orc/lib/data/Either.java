package orc.lib.data;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.Constructor;
import orc.runtime.values.TupleValue;

public abstract class Either extends Constructor {
	protected static class Tag {
		public Object value;
		public Tag(Object value) { this.value = value; }
	}
}
