package orc.lib.net;

import java.util.LinkedList;
import java.util.List;

import orc.error.runtime.JavaException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.ListValue;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Wrap a JSONObject to create an Orc site. Properties
 * may be accessed with dotted syntax. Array values are
 * turned into Orc lists, object values are turned into
 * sites, and all other values are turned into the Orc
 * atomic types you would expect.
 * 
 * @author quark
 */
class JSONSite extends EvalSite {
	private JSONObject root;
	private JSONSite(JSONObject root) {
		this.root = root;
	}
	public static Object wrapJSON(Object o) {
		if (o instanceof JSONObject) {
			return new JSONSite((JSONObject)o); 
		} else if (o instanceof JSONArray) {
			List<Object> out = new LinkedList<Object>();
			JSONArray a = (JSONArray)o;
			for (int i = 0; i < a.length(); ++i) {
				Object e;
				try {
					e = a.get(i);
				} catch (JSONException _) {
					// the index does not exist
					e = null;
				}
				out.add(wrapJSON(e));
			}
			return ListValue.make(out);
		} else {
			return o;
		}
	}
	@Override
	public Object evaluate(Args args) throws TokenException {
		try {
			if (root == null) {
				throw new SiteException("Tried to get a property from" +
						" a null JSON object.");
			}
			return wrapJSON(root.get(args.fieldName()));
		} catch (JSONException e) {
			throw new JavaException(e);
		}
	}
}
