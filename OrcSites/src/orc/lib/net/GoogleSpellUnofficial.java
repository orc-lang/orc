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
	public Object evaluate(Args args) throws TokenException, Pausable {
		// get the first page of results and the cursor
		try {
			String search = args.stringArg(0);
			StringBuffer request = new StringBuffer();
			request.append("<spellrequest textalreadyclipped=\"0\""+
					" ignoredups=\"1\" ignoredigits=\"1\" ignoreallcaps=\"0\">" +
					"<text>");
			request.append(XMLUtils.escapeXML(search));
			request.append("</text></spellrequest>");
			Document root = XMLUtils.postURL(new URL(apiURL), request.toString());
			NodeList nodes = root.getElementsByTagName("c");
			if (nodes.getLength() == 0) return NilValue.singleton;
			String[] words = nodes.item(0).getTextContent().split("\t");
			return ListValue.make(words);
		} catch (UnsupportedEncodingException e) {
			// should be impossible
			throw new OrcError(e);
		} catch (MalformedURLException e) {
			// should be impossible
			throw new OrcError(e);
		} catch (IOException e) {
			throw new JavaException(e);
		} catch (SAXException e) {
			throw new JavaException(e);
		}
	}
}
