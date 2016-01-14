//
// JSONSite.java -- Java class JSONSite
// Project OrcSites
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.util.LinkedList;
import java.util.List;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Wrap a JSONObject to create an Orc site. Properties may be accessed with
 * dotted syntax. Array values are turned into Orc lists, object values are
 * turned into sites, and all other values are turned into the Orc atomic types
 * you would expect.
 *
 * @author quark
 */
class JSONSite extends EvalSite {
    private final JSONObject root;

    private JSONSite(final JSONObject root) {
        this.root = root;
    }

    public static Object wrapJSON(final Object o) {
        if (o instanceof JSONObject) {
            return new JSONSite((JSONObject) o);
        } else if (o instanceof JSONArray) {
            final List<Object> out = new LinkedList<Object>();
            final JSONArray a = (JSONArray) o;
            for (int i = 0; i < a.length(); ++i) {
                Object e;
                try {
                    e = a.get(i);
                } catch (final JSONException je) {
                    // the index does not exist
                    e = null;
                }
                out.add(wrapJSON(e));
            }
            return makeList(out);
        } else {
            return o;
        }
    }

    @Override
    public Object evaluate(final Args args) throws TokenException {
        try {
            if (root == null) {
                throw new NullPointerException("Tried to get a property from" + " a null JSON object.");
            }
            return wrapJSON(root.get(args.fieldName()));
        } catch (final JSONException e) {
            throw new JavaException(e);
        }
    }
}
