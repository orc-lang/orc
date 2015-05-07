//
// Upcoming.java -- Java class Upcoming
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * API for http://upcoming.yahoo.org. This is a factory class
 * which can be used to get instances of the Upcoming class.
 * 
 * @author quark
 */
public final class Upcoming {
	private static String baseURL = "http://upcoming.yahooapis.com/services/rest/";
	private static DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
	private static DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("HH:mm:ss");
	private static DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

	private static final LocalDate parseDate(final String date) {
		if (date == null || date.equals("")) {
			return null;
		}
		return dateFormatter.parseDateTime(date).toLocalDate();
	}

	private static final LocalDateTime parseDateTime(final String dateTime) {
		if (dateTime == null || dateTime.equals("")) {
			return null;
		}
		return dateTimeFormatter.parseDateTime(dateTime).toLocalDateTime();
	}

	private static final LocalTime parseTime(final String time) {
		if (time == null || time.equals("")) {
			return null;
		}
		try {
			return timeFormatter.parseDateTime(time).toLocalTime();
		} catch (final IllegalArgumentException _) {
			// FIXME: I'm seeing nonsense values like "-00001";
			// for now we'll just ignore invalid dates and times
			return null;
		}
	}

	private final String key;

	/**
	 * Create a new Upcoming object using the given properties file.
	 * @param file resource path to properties file which should define orc.lib.net.upcoming.key
	 * @throws IOException if the properties file cannot be read
	 */
	public Upcoming(final String file) throws IOException {
		final Properties p = new Properties();
		final InputStream stream = Upcoming.class.getResourceAsStream("/" + file);
		if (stream == null) {
			throw new FileNotFoundException(file);
		}
		p.load(stream);
		key = p.getProperty("orc.lib.net.upcoming.key");
	}

	public EventSearch eventSearch() {
		return new EventSearch();
	}

	public final class EventSearch {
		public String search_text = null;
		public String location = null;
		public LocalDate min_date = null;
		public LocalDate max_date = null;

		// TODO: add more parameters

		private EventSearch() {
		}

		private String toQuery() {
			try {
				final String out = (search_text != null ? "&search_text=" + URLEncoder.encode(search_text, "UTF-8") : "") + (location != null ? "&location=" + URLEncoder.encode(location, "UTF-8") : "") + (min_date != null ? "&min_date=" + URLEncoder.encode(dateFormatter.print(min_date), "UTF-8") : "") + (max_date != null ? "&max_date=" + URLEncoder.encode(dateFormatter.print(max_date), "UTF-8") : "");
				return out;
			} catch (final UnsupportedEncodingException e) {
				// should be impossible
				throw new AssertionError(e);
			}
		}

		public Event[] run() throws IOException, SAXException {
			final URL url;
			try {
				url = new URL(baseURL + "?api_key=" + key + "&method=event.search" + toQuery());
			} catch (final MalformedURLException e) {
				// should be impossible
				throw new AssertionError(e);
			}
			final Document result = XMLUtils.getURL(url);
			final NodeList events = result.getElementsByTagName("event");
			final Event[] out = new Event[events.getLength()];
			for (int i = 0; i < events.getLength(); ++i) {
				out[i] = new Event((Element) events.item(i));
			}
			return out;
		}
	}

	public final static class Event {
		public final String id;
		public final String name;
		public final String description;
		public final LocalDate start_date;
		public final LocalTime start_time;
		public final LocalDate end_date;
		public final LocalTime end_time;
		public final boolean personal;
		public final boolean selfpromotion;
		public final String metro_id;
		public final String venue_id;
		public final String user_id;
		public final String category_id;
		public final LocalDateTime date_posted;
		public final double latitude;
		public final double longitude;
		public final String geocoding_precision;
		public final boolean geocoding_ambiguous;
		public final String venue_name;
		public final String venue_address;
		public final String venue_city;
		public final String venue_state_id;
		public final String venue_state_code;
		public final String venue_state_name;
		public final String venue_country_id;
		public final String venue_country_code;
		public final String venue_country_name;
		public final String venue_zip;
		public final String ticket_url;
		public final String ticket_price;
		public final boolean ticket_free;
		public final String photo_url;

		public Event(final Element e) {
			id = e.getAttribute("id");
			name = e.getAttribute("name");
			description = e.getAttribute("description");
			start_date = parseDate(e.getAttribute("start_date"));
			start_time = parseTime(e.getAttribute("start_time"));
			end_date = parseDate(e.getAttribute("end_date"));
			end_time = parseTime(e.getAttribute("end_time"));
			personal = "1".equals(e.getAttribute("personal"));
			selfpromotion = "1".equals(e.getAttribute("selfpromotion"));
			metro_id = e.getAttribute("metro_id");
			venue_id = e.getAttribute("venue_id");
			user_id = e.getAttribute("user_id");
			category_id = e.getAttribute("category_id");
			date_posted = parseDateTime(e.getAttribute("date_posted"));
			latitude = Double.parseDouble(e.getAttribute("latitude"));
			longitude = Double.parseDouble(e.getAttribute("longitude"));
			geocoding_precision = e.getAttribute("geocoding_precision");
			geocoding_ambiguous = "1".equals(e.getAttribute("geocoding_ambiguous"));
			venue_name = e.getAttribute("venue_name");
			venue_address = e.getAttribute("venue_address");
			venue_city = e.getAttribute("venue_city");
			venue_state_id = e.getAttribute("venue_state_id");
			venue_state_code = e.getAttribute("venue_state_code");
			venue_state_name = e.getAttribute("venue_state_name");
			venue_country_id = e.getAttribute("venue_country_id");
			venue_country_code = e.getAttribute("venue_country_code");
			venue_country_name = e.getAttribute("venue_country_name");
			venue_zip = e.getAttribute("venue_zip");
			ticket_url = e.getAttribute("ticket_url");
			ticket_price = e.getAttribute("ticket_price");
			ticket_free = "1".equals(e.getAttribute("ticket_free"));
			photo_url = e.getAttribute("photo_url");
		}
	}
}
