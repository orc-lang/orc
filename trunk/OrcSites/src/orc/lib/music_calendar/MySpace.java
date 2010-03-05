//
// MySpace.java -- Java class MySpace
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.music_calendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.PartialSite;

import org.htmlparser.Parser;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.StringFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

/**
 * @author tfinster
 */
public class MySpace extends DotSite {

	@Override
	protected void addMembers() {
		addMember("scrapeMusicShows", new scrapeMusicMember());
	}

	private class scrapeMusicMember extends PartialSite {

		private final Map<String, Boolean> seenURLs = new HashMap<String, Boolean>();

		@Override
		public Object evaluate(final Args args) throws TokenException {

			try {
				final String myspaceURL = args.stringArg(0);

				/* stay silent if we've processed this URL before */
				if (seenURLs.containsKey(myspaceURL)) {
					return null;
				}
				seenURLs.put(myspaceURL, true);

				final Parser profileParser = new Parser(myspaceURL);

				final NodeList list = profileParser.extractAllNodesThatMatch(new LinkRegexFilter("bandprofile.listAllShows"));

				if (list.size() != 1) {
					return null;
				}

				final LinkTag link = (LinkTag) list.elementAt(0);
				final String showUrl = link.extractLink();

				return extractMusicShows(showUrl);
			} catch (final ParserException e) {
				throw new JavaException(e);
			}
		}
	}

	private List<MusicShow> extractMusicShows(final String showUrl) throws ParserException {
		final List<MusicShow> musicShows = new ArrayList<MusicShow>();

		final Parser showParser = new Parser(showUrl);

		// extract band name
		final String bandName = extractBandName(showUrl);

		// extract shows
		final NodeList formList = showParser.extractAllNodesThatMatch(new TagNameFilter("form"));

		for (int i = 0; i < formList.size(); i++) {

			final FormTag form = (FormTag) formList.elementAt(i);

			if (form.getAttribute("action") != null && form.getAttribute("action").contains("mycalendar")) {
				final NodeList inputList = form.getFormInputs();

				final MusicShow show = new MusicShow();
				show.setBandName(bandName);

				for (int j = 0; j < inputList.size(); j++) {
					final InputTag input = (InputTag) inputList.elementAt(j);
					if (input.getAttribute("name") != null) {
						if (input.getAttribute("name").equals("calEvtLocation")) {
							show.setLocation(input.getAttribute("value"));
						} else if (input.getAttribute("name").equals("calEvtTitle")) {
							show.setTitle(input.getAttribute("value"));
						} else if (input.getAttribute("name").equals("calEvtCity")) {
							show.setCity(input.getAttribute("value"));
						} else if (input.getAttribute("name").equals("calEvtState")) {
							show.setState(input.getAttribute("value"));
						} else if (input.getAttribute("name").equals("calEvtDateTime")) {
							try {
								final DateFormat fmt = new SimpleDateFormat("MM-dd-yyyy HH:mm");
								final String dateStr = input.getAttribute("value");
								show.setDate(fmt.parse(dateStr));
							} catch (final ParseException e) {
							}
						}
					}
				}
				musicShows.add(show);
			}
		}

		return musicShows;
	}

	private String extractBandName(final String showUrl) throws ParserException {
		final Parser showParser = new Parser(showUrl);
		final NodeList bandNameList = showParser.extractAllNodesThatMatch(new StringFilter("All Shows for"));

		if (bandNameList.size() != 1) {
			throw new ParserException("Unable to extract band name");
		}

		final TextNode bandNameNode = (TextNode) bandNameList.elementAt(0);
		final String[] parts = bandNameNode.getText().split("All Shows for ");

		if (parts.length != 2) {
			throw new ParserException("Unable to extract band name");
		}

		return parts[1];
	}
}
