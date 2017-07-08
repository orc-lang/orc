//
// Dictionary.java -- Java class Dictionary
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.util.HashMap;

import orc.error.runtime.TokenException;
import orc.lib.state.Ref.RefInstance;
import orc.values.Field;
import orc.values.HasMembers;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

// TODO: Remove this entirely? It is based on the idea of objects with variable sets of fields which is kinda broken in the new OrcO stuff.
/**
 * @author quark
 */
public class Dictionary extends EvalSite {
    protected static class DictionaryInstance implements HasMembers {
        private final HashMap<String, RefInstance> map = new HashMap<String, RefInstance>();

        @Override
        public String toString() {
            return map.toString();
        }

        @Override
        synchronized public Object  getMember(Field f) {
            final String field = f.field();
            RefInstance out = map.get(field);
            if (out == null) {
                out = new RefInstance();
                map.put(field, out);
            }
            return out;
        }

        @Override
        synchronized public boolean hasMember(Field f) {
            return true;
        }

        @Override
        public String toOrcSyntax() {
            return "DictionaryInstance";
        }
    }

    @Override
    public Object evaluate(final Args args) throws TokenException {
        return new DictionaryInstance();
    }
}
