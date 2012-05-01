//
// Dictionary.java -- Java class Dictionary
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.util.HashMap;

import orc.error.runtime.TokenException;
import orc.lib.state.Ref.RefInstance;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

/**
 * @author quark
 */
public class Dictionary extends EvalSite {
	protected static class DictionaryInstance extends EvalSite {
		private final HashMap<String, RefInstance> map = new HashMap<String, RefInstance>();

		@Override
		public Object evaluate(final Args args) throws TokenException {
			synchronized (DictionaryInstance.this) {
				final String field = args.fieldName();
				RefInstance out = map.get(field);
				if (out == null) {
					out = new RefInstance();
					map.put(field, out);
				}
				return out;
			}
		}

		@Override
		public String toString() {
			return map.toString();
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		return new DictionaryInstance();
	}
}
