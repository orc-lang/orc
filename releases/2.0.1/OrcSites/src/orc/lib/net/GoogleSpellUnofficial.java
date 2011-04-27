//
// GoogleSpellUnofficial.java -- Java class GoogleSpellUnofficial
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * See examples/spell_web.orc
 * <p>
 * Uses Google Toolbar's unofficial spelling API
 *
 * @author quark
 */
public class GoogleSpellUnofficial extends EvalSite {
	private final static String apiURL = "https://www.google.com/tbproxy/spell?lang=en&hl=en";

	@Override
	public Object evaluate(final Args args) throws TokenException {
		// get the first page of results and the cursor
		try {
			final String search = args.stringArg(0);
			final StringBuffer request = new StringBuffer();
			request.append("<spellrequest textalreadyclipped=\"0\"" + " ignoredups=\"1\" ignoredigits=\"1\" ignoreallcaps=\"0\">" + "<text>");
			request.append(XMLUtils.escapeXML(search));
			request.append("</text></spellrequest>");
			final Document root = XMLUtils.postURL(new URL(apiURL), request.toString());
			final NodeList nodes = root.getElementsByTagName("c");
			if (nodes.getLength() == 0) {
				return nilList();
			}
			final String[] words = nodes.item(0).getTextContent().split("\t");
			//return ListValue.make(words);
			return makeListFromArray(words);
		} catch (final MalformedURLException e) {
			// should be impossible
			throw new AssertionError(e);
		} catch (final IOException e) {
			throw new JavaException(e);
		} catch (final SAXException e) {
			throw new JavaException(e);
		}
	}
}
