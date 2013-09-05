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

import orc.values.OrcTuple;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * API for http://geocoder.us. This service returns a latitude/longitude tuple
 * given a city/state, zip code, or full address in the United States.
 * 
 * @author quark
 */
public class Geocoder {
	private static String baseURL = "http://geocoder.us/service/csv/geocode?";

	private static OrcTuple parseCSV(final String csv) {
		final String[] parts = csv.split(",");
		if (parts.length < 3) {
			return null;
		}
		try {
			return SiteAdaptor.makePair(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
		} catch (final NumberFormatException e) {
			return null;
		}
	}

	public static OrcTuple locateAddress(final String address) throws IOException {
		return parseCSV(HTTPUtils.getURL(new URL(baseURL + "address=" + URLEncoder.encode(address, "UTF-8"))));
	}

	public static OrcTuple locateCity(final String city, final String state) throws IOException {
		return parseCSV(HTTPUtils.getURL(new URL(baseURL + "city=" + URLEncoder.encode(city, "UTF-8") + "&state=" + URLEncoder.encode(state, "UTF-8"))));
	}

	public static OrcTuple locateZip(final String zip) throws IOException {
		return parseCSV(HTTPUtils.getURL(new URL(baseURL + "zip=" + URLEncoder.encode(zip, "UTF-8"))));
	}
}
