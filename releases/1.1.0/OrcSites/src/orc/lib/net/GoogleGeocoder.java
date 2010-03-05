//
// GoogleGeocoder.java -- Java class GoogleGeocoder
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

import kilim.Pausable;
import orc.error.OrcError;
import orc.error.runtime.JavaException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.KilimSite;
import orc.runtime.values.TupleValue;

/**
 * API for http://geocoder.us. This service returns a latitude/longitude tuple
 * given a city/state, zip code, or full address in the United States.
 * 
 * @author quark
 */
public class GoogleGeocoder extends EvalSite {
	private static String baseURL = "http://maps.google.com/maps/geo?";

	private static class GeocoderInstance extends KilimSite {
		private final String apiKey;

		public GeocoderInstance(final String apiKey) throws IOException {
			this.apiKey = apiKey;
		}

		@Override
		public Object evaluate(final Args args) throws Pausable, TokenException {
			try {
				final URL url = new URL(baseURL + "key=" + apiKey + "&q=" + URLEncoder.encode(args.stringArg(0), "UTF-8") + "&sensor=false" + "&output=csv" + "&oe=utf8");
				System.out.println(url);
				return parseCSV(HTTPUtils.getURL(url));
			} catch (final MalformedURLException e) {
				throw new OrcError(e);
			} catch (final UnsupportedEncodingException e) {
				throw new OrcError(e);
			} catch (final IOException e) {
				throw new JavaException(e);
			}
		}
	}

	private static TupleValue parseCSV(final String csv) throws Pausable, TokenException {
		final String[] parts = csv.split(",");
		final String statusCode = parts[0];
		if (statusCode.equals("200")) {
			try {
				if (parts.length != 4) {
					throw new SiteException("Unexpected input: " + csv);
				}
				return new TupleValue(Double.parseDouble(parts[2].trim()), Double.parseDouble(parts[3].trim()));
			} catch (final NumberFormatException e) {
				throw new SiteException("Unexpected input: " + csv);
			}
		} else if (statusCode.equals("602")) {
			// address unknown
			Kilim.exit();
			return null;
		} else if (statusCode.equals("603")) {
			// address unavailable
			Kilim.exit();
			return null;
		} else {
			throw new SiteException("Geocoding error code: " + statusCode);
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		try {
			final String file = "/" + args.stringArg(0);
			final Properties p = new Properties();
			final InputStream stream = GoogleGeocoder.class.getResourceAsStream(file);
			if (stream == null) {
				throw new FileNotFoundException(file);
			}
			p.load(stream);
			return new GeocoderInstance(p.getProperty("orc.lib.net.google.key"));
		} catch (final IOException e) {
			throw new JavaException(e);
		}
	}
}
