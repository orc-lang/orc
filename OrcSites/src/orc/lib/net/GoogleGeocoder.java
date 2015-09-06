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

import orc.error.runtime.JavaException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.values.OrcTuple;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

/**
 * API for Google Geocoding service. This service returns a latitude/longitude tuple
 * given a city/state, zip code, or full address in the United States.
 * 
 * @author quark
 */
public class GoogleGeocoder extends EvalSite {
	private static String baseURL = "http://maps.google.com/maps/geo?";

	private static class GeocoderInstance extends EvalSite {
		private final String apiKey;

		public GeocoderInstance(final String apiKey) throws IOException {
			this.apiKey = apiKey;
		}

		@Override
		public Object evaluate(final Args args) throws TokenException {
			try {
				final URL url = new URL(baseURL + "key=" + apiKey + "&q=" + URLEncoder.encode(args.stringArg(0), "UTF-8") + "&sensor=false" + "&output=csv" + "&oe=utf8");
				System.out.println(url);
				return parseCSV(HTTPUtils.getURL(url));
			} catch (final MalformedURLException e) {
				throw new AssertionError(e);
			} catch (final UnsupportedEncodingException e) {
				throw new AssertionError(e);
			} catch (final IOException e) {
				throw new JavaException(e);
			}
		}
	}

	private static OrcTuple parseCSV(final String csv) throws TokenException {
		final String[] parts = csv.split(",");
		final String statusCode = parts[0];
		if (statusCode.equals("200")) {
			try {
				if (parts.length != 4) {
					throw new GoogleGeocoderException("Unexpected input: " + csv);
				}
				return makePair(Double.parseDouble(parts[2].trim()), Double.parseDouble(parts[3].trim()));
			} catch (final NumberFormatException e) {
				throw new GoogleGeocoderException("Unexpected input: " + csv);
			}
		} else if (statusCode.equals("602")) {
			// address unknown
			return null;
		} else if (statusCode.equals("603")) {
			// address unavailable
			return null;
		} else {
			throw new GoogleGeocoderException("Geocoding error code: " + statusCode);
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

	public static class GoogleGeocoderException extends SiteException {
		private static final long serialVersionUID = 4572428654641968903L;

		public GoogleGeocoderException(final String msg) {
			super(msg);
		}
	}
}
