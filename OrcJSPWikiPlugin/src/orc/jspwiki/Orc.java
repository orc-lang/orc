//
// Orc.java -- Java class Orc, a plugin for JSPWiki
// Project OrcWikiPlugin
//
// $Id$
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.jspwiki;

import java.util.Map;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.WikiPlugin;

/**
 * This is a plugin for JSPWiki which enables syntax like this:
 * <pre>
 * [{orc
 * 
 * -- your program here
 * }]
 * </pre>
 * 
 * <p>This plugin supports the properties:
 * <ul>
 * <li>runnable: if "false", program is not runnable.
 * <li>editable: if "true", program is editable.
 * <li>height: a CSS height (e.g. "100px"). If this is not given, the box will be just big enough to contain the existing program text.
 * <li>spoiler: if present, this text will appear instead of the program. The user can click it to reveal the program. E.g. spoiler="(click for solution)".
 * <li>baseURL: URL path to Orchard installation (defaults to "/orchard/")
 * </ul>
 * 
 * <p>To install, "ant jar", place the resulting jar in the WEB-INF/lib directory of jspwiki, and restart your servlet container.
 * 
 * @author quark
 *
 */
public class Orc implements WikiPlugin {
	private final static String HAS_ORC = "orc.jspwiki.HAS_ORC";
	@Override
	public String execute(final WikiContext ctx, final Map props) throws PluginException {
		String baseURL = (String)props.get("baseURL");
		if (baseURL == null) baseURL = "/orchard/";
		StringBuilder out = new StringBuilder();
		// hack to ensure the browser closes any previous paragraph tags
		out.append("<div>");
		if (ctx.getVariable(HAS_ORC) == null) {
			// add required css and js files
			ctx.setVariable(HAS_ORC, true);
			out.append("<script src=\"" +
					baseURL + "orc.js\" " +
					"type=\"text/javascript\"></script>\n");
			out.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" +
					baseURL + "orc.css\" media=\"screen\"/>");
			out.append("<script type=\"text/javascript\">" +
					"function orcSpoiler(a) {" +
					"var $a = jQuery(a);" +
					"var $orc = $a.next();" +
					"$orc.css('display', 'none');" +
					"$orc.css('visibility', 'visible');" +
					"$orc.css('position', 'static');" +
					"$orc.slideDown();" +
					"$a.hide();" +
					"}" +
					"</script>");
		}
		String tag = "pre";
		String editable = (String)props.get("editable");
		if (editable != null && !editable.equals("false")) {
			tag = "textarea";
		}
		String height = (String)props.get("height");
		String runnable = (String)props.get("runnable");
		String spoiler = (String)props.get("spoiler");
		String body = (String)props.get("_body");
		if (body != null) {
			if (spoiler != null) {
				out.append("<a href=\"#\" onclick=\"orcSpoiler(this)\">" + spoiler + "</a>");
				out.append("<div style=\"visibility: hidden; position: absolute; top: 0; left: 0;\">");
			}
			body = TextUtil.replaceEntities(body);
			out.append("<"+tag+" class=\"orc");
			if (runnable != null && runnable.equals("false")) {
				out.append("-snippet");
			}
			out.append("\"");
			if (height != null) {
				out.append(" style=\"height: " + height + "\"");
			}
			out.append(">");
			out.append(body.trim());
			out.append("</"+tag+">");
			if (spoiler != null) {
				out.append("</div>");
			}
		}
		out.append("</div>");
		return out.toString();
	}
}
