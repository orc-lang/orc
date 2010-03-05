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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import kilim.Pausable;
import orc.error.OrcError;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.KilimSite;
import orc.runtime.values.ListValue;
import orc.runtime.values.NilValue;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * See examples/spell.orc
 * <p>
 * http://developer.yahoo.com/search/web/V1/spellingSuggestion.html
 * @author quark
 */
public class GoogleSpellUnofficial extends KilimSite {
	private final static String apiURL = "https://www.google.com/tbproxy/spell?lang=en&hl=en";

	@Override
	public Object evaluate(final Args args) throws TokenException, Pausable {
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
				return NilValue.singleton;
			}
			final String[] words = nodes.item(0).getTextContent().split("\t");
			return ListValue.make(words);
		} catch (final UnsupportedEncodingException e) {
			// should be impossible
			throw new OrcError(e);
		} catch (final MalformedURLException e) {
			// should be impossible
			throw new OrcError(e);
		} catch (final IOException e) {
			throw new JavaException(e);
		} catch (final SAXException e) {
			throw new JavaException(e);
		}
	}
}
