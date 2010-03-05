//
// Geocoder.java -- Java class Geocoder
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
import java.net.URL;
import java.net.URLEncoder;

import kilim.Pausable;
import orc.runtime.Kilim;
import orc.runtime.values.TupleValue;

/**
 * API for http://geocoder.us. This service returns a latitude/longitude tuple
 * given a city/state, zip code, or full address in the United States.
 * 
 * FIXME: as of 6/9/2009, geocoder.us responds to all requests with a "Bad Request"
 * message. Therefore I recommend using GoogleGeocoderFactory instead.
 * @author quark
 */
public class Geocoder {
	private static String baseURL = "http://geocoder.us/service/csv/geocode?";

	private static TupleValue parseCSV(final String csv) throws Pausable {
		final String[] parts = csv.split(",");
		if (parts.length < 3) {
			Kilim.exit();
			return null;
		}
		try {
			return new TupleValue(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
		} catch (final NumberFormatException e) {
			Kilim.exit();
			return null;
		}
	}

	public static TupleValue locateAddress(final String address) throws IOException, Pausable {
		return parseCSV(HTTPUtils.getURL(new URL(baseURL + "address=" + URLEncoder.encode(address, "UTF-8"))));
	}

	public static TupleValue locateCity(final String city, final String state) throws IOException, Pausable {
		return parseCSV(HTTPUtils.getURL(new URL(baseURL + "city=" + URLEncoder.encode(city, "UTF-8") + "&state=" + URLEncoder.encode(state, "UTF-8"))));
	}

	public static TupleValue locateZip(final String zip) throws IOException, Pausable {
		return parseCSV(HTTPUtils.getURL(new URL(baseURL + "zip=" + URLEncoder.encode(zip, "UTF-8"))));
	}
}
