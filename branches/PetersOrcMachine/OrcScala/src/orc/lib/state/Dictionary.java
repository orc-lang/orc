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

import orc.Handle;
import orc.error.runtime.TokenException;
import orc.lib.state.Ref.RefInstance;
import orc.values.Field;
import orc.values.sites.HasFields;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import scala.Option;
import scala.Some;
import scala.collection.immutable.List;

/**
 * @author quark
 */
public class Dictionary extends EvalSite {
	protected static class DictionaryInstance extends EvalSite implements HasFields {
		private final HashMap<String, RefInstance> map = new HashMap<String, RefInstance>();

		@Override
		public Object evaluate(final Args args) throws TokenException {
			return getField((Field)args.getArg(0));
		}

		@Override
		public String toString() {
			return map.toString();
		}

        @Override
        synchronized public Object getField(Field f) {
          final String field = f.field();
          RefInstance out = map.get(field);
          if (out == null) {
              out = new RefInstance();
              map.put(field, out);
          }
          return out;
        }
        @Override
        synchronized public boolean hasField(Field f) {
          return true;
        }
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		return new DictionaryInstance();
	}
}
