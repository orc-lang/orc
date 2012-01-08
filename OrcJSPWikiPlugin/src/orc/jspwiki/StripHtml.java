//
// StripHtml.java -- Java class StripHtml
// Project OrcWikiPlugin
//
// $Id$
//
// Created by jthywiss on Jan 7, 2012.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.jspwiki;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to strip XHTML markup from text, resulting in plain text.
 * 
 * @author jthywiss
 */
public class StripHtml {

	protected static final Map<String, String> entityMap;

	static {
		final String entityMapKV[] = {
				"quot", "\"",
				"amp", "\u0026",
				"lt", "\u003C",
				"gt", "\u003E",
				"apos", "\u0027",
				"OElig", "\u0152",
				"oelig", "\u0153",
				"Scaron", "\u0160",
				"scaron", "\u0161",
				"Yuml", "\u0178",
				"circ", "\u02C6",
				"tilde", "\u02DC",
				"ensp", "\u2002",
				"emsp", "\u2003",
				"thinsp", "\u2009",
				"zwnj", "\u200C",
				"zwj", "\u200D",
				"lrm", "\u200E",
				"rlm", "\u200F",
				"ndash", "\u2013",
				"mdash", "\u2014",
				"lsquo", "\u2018",
				"rsquo", "\u2019",
				"sbquo", "\u201A",
				"ldquo", "\u201C",
				"rdquo", "\u201D",
				"bdquo", "\u201E",
				"dagger", "\u2020",
				"Dagger", "\u2021",
				"permil", "\u2030",
				"lsaquo", "\u2039",
				"rsaquo", "\u203A",
				"euro", "\u20AC",
				"nbsp", "\u00A0",
				"iexcl", "\u00A1",
				"cent", "\u00A2",
				"pound", "\u00A3",
				"curren", "\u00A4",
				"yen", "\u00A5",
				"brvbar", "\u00A6",
				"sect", "\u00A7",
				"uml", "\u00A8",
				"copy", "\u00A9",
				"ordf", "\u00AA",
				"laquo", "\u00AB",
				"not", "\u00AC",
				"shy", "\u00AD",
				"reg", "\u00AE",
				"macr", "\u00AF",
				"deg", "\u00B0",
				"plusmn", "\u00B1",
				"sup2", "\u00B2",
				"sup3", "\u00B3",
				"acute", "\u00B4",
				"micro", "\u00B5",
				"para", "\u00B6",
				"middot", "\u00B7",
				"cedil", "\u00B8",
				"sup1", "\u00B9",
				"ordm", "\u00BA",
				"raquo", "\u00BB",
				"frac14", "\u00BC",
				"frac12", "\u00BD",
				"frac34", "\u00BE",
				"iquest", "\u00BF",
				"Agrave", "\u00C0",
				"Aacute", "\u00C1",
				"Acirc", "\u00C2",
				"Atilde", "\u00C3",
				"Auml", "\u00C4",
				"Aring", "\u00C5",
				"AElig", "\u00C6",
				"Ccedil", "\u00C7",
				"Egrave", "\u00C8",
				"Eacute", "\u00C9",
				"Ecirc", "\u00CA",
				"Euml", "\u00CB",
				"Igrave", "\u00CC",
				"Iacute", "\u00CD",
				"Icirc", "\u00CE",
				"Iuml", "\u00CF",
				"ETH", "\u00D0",
				"Ntilde", "\u00D1",
				"Ograve", "\u00D2",
				"Oacute", "\u00D3",
				"Ocirc", "\u00D4",
				"Otilde", "\u00D5",
				"Ouml", "\u00D6",
				"times", "\u00D7",
				"Oslash", "\u00D8",
				"Ugrave", "\u00D9",
				"Uacute", "\u00DA",
				"Ucirc", "\u00DB",
				"Uuml", "\u00DC",
				"Yacute", "\u00DD",
				"THORN", "\u00DE",
				"szlig", "\u00DF",
				"agrave", "\u00E0",
				"aacute", "\u00E1",
				"acirc", "\u00E2",
				"atilde", "\u00E3",
				"auml", "\u00E4",
				"aring", "\u00E5",
				"aelig", "\u00E6",
				"ccedil", "\u00E7",
				"egrave", "\u00E8",
				"eacute", "\u00E9",
				"ecirc", "\u00EA",
				"euml", "\u00EB",
				"igrave", "\u00EC",
				"iacute", "\u00ED",
				"icirc", "\u00EE",
				"iuml", "\u00EF",
				"eth", "\u00F0",
				"ntilde", "\u00F1",
				"ograve", "\u00F2",
				"oacute", "\u00F3",
				"ocirc", "\u00F4",
				"otilde", "\u00F5",
				"ouml", "\u00F6",
				"divide", "\u00F7",
				"oslash", "\u00F8",
				"ugrave", "\u00F9",
				"uacute", "\u00FA",
				"ucirc", "\u00FB",
				"uuml", "\u00FC",
				"yacute", "\u00FD",
				"thorn", "\u00FE",
				"yuml", "\u00FF",
				"fnof", "\u0192",
				"Alpha", "\u0391",
				"Beta", "\u0392",
				"Gamma", "\u0393",
				"Delta", "\u0394",
				"Epsilon", "\u0395",
				"Zeta", "\u0396",
				"Eta", "\u0397",
				"Theta", "\u0398",
				"Iota", "\u0399",
				"Kappa", "\u039A",
				"Lambda", "\u039B",
				"Mu", "\u039C",
				"Nu", "\u039D",
				"Xi", "\u039E",
				"Omicron", "\u039F",
				"Pi", "\u03A0",
				"Rho", "\u03A1",
				"Sigma", "\u03A3",
				"Tau", "\u03A4",
				"Upsilon", "\u03A5",
				"Phi", "\u03A6",
				"Chi", "\u03A7",
				"Psi", "\u03A8",
				"Omega", "\u03A9",
				"alpha", "\u03B1",
				"beta", "\u03B2",
				"gamma", "\u03B3",
				"delta", "\u03B4",
				"epsilon", "\u03B5",
				"zeta", "\u03B6",
				"eta", "\u03B7",
				"theta", "\u03B8",
				"iota", "\u03B9",
				"kappa", "\u03BA",
				"lambda", "\u03BB",
				"mu", "\u03BC",
				"nu", "\u03BD",
				"xi", "\u03BE",
				"omicron", "\u03BF",
				"pi", "\u03C0",
				"rho", "\u03C1",
				"sigmaf", "\u03C2",
				"sigma", "\u03C3",
				"tau", "\u03C4",
				"upsilon", "\u03C5",
				"phi", "\u03C6",
				"chi", "\u03C7",
				"psi", "\u03C8",
				"omega", "\u03C9",
				"thetasym", "\u03D1",
				"upsih", "\u03D2",
				"piv", "\u03D6",
				"bull", "\u2022",
				"hellip", "\u2026",
				"prime", "\u2032",
				"Prime", "\u2033",
				"oline", "\u203E",
				"frasl", "\u2044",
				"weierp", "\u2118",
				"image", "\u2111",
				"real", "\u211C",
				"trade", "\u2122",
				"alefsym", "\u2135",
				"larr", "\u2190",
				"uarr", "\u2191",
				"rarr", "\u2192",
				"darr", "\u2193",
				"harr", "\u2194",
				"crarr", "\u21B5",
				"lArr", "\u21D0",
				"uArr", "\u21D1",
				"rArr", "\u21D2",
				"dArr", "\u21D3",
				"hArr", "\u21D4",
				"forall", "\u2200",
				"part", "\u2202",
				"exist", "\u2203",
				"empty", "\u2205",
				"nabla", "\u2207",
				"isin", "\u2208",
				"notin", "\u2209",
				"ni", "\u220B",
				"prod", "\u220F",
				"sum", "\u2211",
				"minus", "\u2212",
				"lowast", "\u2217",
				"radic", "\u221A",
				"prop", "\u221D",
				"infin", "\u221E",
				"ang", "\u2220",
				"and", "\u2227",
				"or", "\u2228",
				"cap", "\u2229",
				"cup", "\u222A",
				"int", "\u222B",
				"there4", "\u2234",
				"sim", "\u223C",
				"cong", "\u2245",
				"asymp", "\u2248",
				"ne", "\u2260",
				"equiv", "\u2261",
				"le", "\u2264",
				"ge", "\u2265",
				"sub", "\u2282",
				"sup", "\u2283",
				"nsub", "\u2284",
				"sube", "\u2286",
				"supe", "\u2287",
				"oplus", "\u2295",
				"otimes", "\u2297",
				"perp", "\u22A5",
				"sdot", "\u22C5",
				"lceil", "\u2308",
				"rceil", "\u2309",
				"lfloor", "\u230A",
				"rfloor", "\u230B",
				"lang", "\u2329",
				"rang", "\u232A",
				"loz", "\u25CA",
				"spades", "\u2660",
				"clubs", "\u2663",
				"hearts", "\u2665",
				"diams", "\u2666",
		};
		entityMap = new HashMap<String, String>(entityMapKV.length / 2);
		for (int i = 0; i < entityMapKV.length; i += 2) {
			entityMap.put(entityMapKV[i], entityMapKV[i + 1]);
		}
	}

	/**
	 * No instances
	 */
	private StripHtml() {
		assert false;
	}

	/**
	 * Removes XHTML markup from text, returning plain text. This is inteded for
	 * short spans (less than a line) of text. Block element mapping to
	 * linebreaks is not attempted. Whitespace is passed-through as-is.
	 * <p>
	 * Tags (start, end, and empty) are stripped, leaving their content.
	 * Comments and XML processing instructions are stripped. CDATA sections are
	 * passed-through with the CDATA markup removed. Character references
	 * (named, decimal, and hex) are replaced with the referred-to character.
	 * Invalid XML is passed-through unchanged. Other SGML-like directives
	 * (notably DOCTYPE) are not handled (passed-through unchanged).
	 * 
	 * @param xhtml string with XHTML markup.
	 * @return plain text string
	 */
	public static String stripXhtml(final String xhtml) {
		if (xhtml.indexOf('<') == -1 && xhtml.indexOf('&') == -1) {
			/* No HTML in input string */
			return xhtml;
		}
		final StringBuilder plaintext = new StringBuilder(xhtml.length());
		for (int currCharIndex = 0; currCharIndex < xhtml.length();) {
			int cleanCount = 0;
			while (currCharIndex + cleanCount < xhtml.length() && xhtml.charAt(currCharIndex + cleanCount) != '<' && xhtml.charAt(currCharIndex + cleanCount) != '&') {
				++cleanCount;
			}
			plaintext.append(xhtml.substring(currCharIndex, currCharIndex + cleanCount));
			currCharIndex += cleanCount;
			if (currCharIndex >= xhtml.length()) {
				break;
			}
			char ch = safeCharAt(currCharIndex + 1, xhtml);
			switch (xhtml.charAt(currCharIndex)) {
			case '<':
				if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_' || ch == ':') {
					// Start tag
					final int endRef = xhtml.indexOf('>', currCharIndex + 2);
					// FIXME: this is BROKEN, because > is legal unescaped in
					// attribute values.
					if (endRef >= 0) {
						currCharIndex = endRef + 1;
						break;
					}
				} else if (xhtml.startsWith("</", currCharIndex)) {
					// End tag
					final int endRef = xhtml.indexOf('>', currCharIndex + 2);
					if (endRef >= 0) {
						currCharIndex = endRef + 1;
						break;
					}
					// else fallthrough to invalid case
				} else if (xhtml.startsWith("<!--", currCharIndex)) {
					// Comment
					final int endRef = xhtml.indexOf("-->", currCharIndex + 4);
					if (endRef >= 0) {
						currCharIndex = endRef + 3;
						break;
					}
					// else fallthrough to invalid case
				} else if (xhtml.startsWith("<![CDATA[", currCharIndex)) {
					// CDATA section
					final int endRef = xhtml.indexOf("]]>", currCharIndex + 9);
					if (endRef >= 0) {
						plaintext.append(xhtml.substring(currCharIndex + 9, endRef));
						currCharIndex = endRef + 3;
						break;
					}
					// else fallthrough to invalid case
				} else if (xhtml.startsWith("<?", currCharIndex)) {
					// Processing Instruction
					final int endRef = xhtml.indexOf("?>", currCharIndex + 2);
					if (endRef >= 0) {
						currCharIndex = endRef + 2;
						break;
					}
					// else fallthrough to invalid case
				}
				// Invalid:
				plaintext.append(xhtml.charAt(currCharIndex));
				++currCharIndex;
				break;
			case '&':
				final int endRef = xhtml.indexOf(';', currCharIndex + 1);
				if ((ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_' || ch == ':') && endRef > 0) {
					// Entity reference
					final String entityValue = entityMap.get(xhtml.substring(currCharIndex + 1, endRef));
					if (entityValue != null) {
						plaintext.append(entityValue);
						currCharIndex = endRef + 1;
						break;
					}
					// else fallthrough to invalid case
				} else if (ch == '#' && endRef > 0) {
					// Character reference
					ch = safeCharAt(currCharIndex + 2, xhtml);
					if ((ch == 'x' || ch == 'X') && allCharsHex(xhtml, currCharIndex + 3, endRef)) {
						plaintext.append((char) Integer.parseInt(xhtml.substring(currCharIndex + 3, endRef), 16));
						currCharIndex = endRef + 1;
						break;
					} else if (allCharsDecimal(xhtml, currCharIndex + 2, endRef)) {
						plaintext.append((char) Integer.parseInt(xhtml.substring(currCharIndex + 2, endRef)));
						currCharIndex = endRef + 1;
						break;
					}
					// else fallthrough to invalid case
				}
				// Invalid:
				plaintext.append(xhtml.charAt(currCharIndex));
				++currCharIndex;
				break;
			default:
				assert false;
			}
		}
		return plaintext.toString();
	}

	/**
	 * True if all chars in string from startIndex to endIndex are legal decimal
	 * digits.
	 */
	public static boolean allCharsDecimal(final String str, final int startIndex, final int endIndex) {
		for (int i = startIndex; i < endIndex; i++) {
			if (safeCharAt(i, str) < '0' || safeCharAt(i, str) > '9') {
				return false;
			}
		}
		return true;
	}

	/**
	 * True if all chars in string from startIndex to endIndex are legal
	 * hexadecimal digits.
	 */
	public static boolean allCharsHex(final String str, final int startIndex, final int endIndex) {
		for (int i = startIndex; i < endIndex; i++) {
			if ((safeCharAt(i, str) < '0' || safeCharAt(i, str) > '9') && (safeCharAt(i, str) < 'a' || safeCharAt(i, str) > 'f') && (safeCharAt(i, str) < 'A' || safeCharAt(i, str) > 'F')) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Like String.charAt, but won't throw StringIndexOutOfBoundsException when
	 * index is greater than string length. Instead, returns a NUL '\0' char.
	 * 
	 * @param index the index of the <code>char</code> value.
	 * @param str the string to operate on.
	 * @return the <code>char</code> value at the specified index of the string.
	 * @see java.lang.String#charAt(int)
	 */
	public static char safeCharAt(final int index, final String str) {
		if (index >= str.length()) {
			return '\0';
		} else {
			return str.charAt(index);
		}
	}

}
