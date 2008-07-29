package orc.jspwiki;

import java.util.Map;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.WikiPlugin;

public class Orc implements WikiPlugin {
	private final static String HAS_ORC = "orc.jspwiki.HAS_ORC";
	public String execute(WikiContext ctx, Map props) throws PluginException {
		String baseURL = (String)props.get("baseURL");
		if (baseURL == null) baseURL = "/orchard/";
		StringBuilder out = new StringBuilder();
		if (ctx.getVariable(HAS_ORC) == null) {
			// add required css and js files
			ctx.setVariable(HAS_ORC, true);
			out.append("<script src=\"" +
					baseURL + "orc.js\" " +
					"type=\"text/javascript\"></script>\n");
			out.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" +
					baseURL + "orc.css\" media=\"screen\"/>");
		}
		String tag = "pre";
		String editable = (String)props.get("editable");
		if (editable != null && !editable.equals("false")) {
			tag = "textarea";
		}
		String height = (String)props.get("height");
		String body = (String)props.get("_body");
		if (body != null) {
			body = TextUtil.replaceEntities(body);
			out.append("<"+tag+" class=\"orc\"");
			if (height != null) {
				out.append(" style=\"height: " + height + "\"");
			}
			out.append(">");
			out.append(body.trim());
			out.append("</"+tag+">");
		}
		return out.toString();
	}
}
