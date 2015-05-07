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
 * @author quark
 */
public class Geocoder {
	private static String baseURL = "http://geocoder.us/service/csv/geocode?";

	private static TupleValue parseCSV(String csv) throws Pausable {
		String[] parts = csv.split(",");
		if (parts.length < 3) {
			Kilim.exit();
			return null;
		}
		try {
			return new TupleValue(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
		} catch (NumberFormatException e) {
			Kilim.exit();
			return null;
		}
	}
	
	public static TupleValue locateAddress(String address) throws IOException, Pausable {
		return parseCSV(HTTPUtils.getURL(new URL(baseURL + "address="
				+ URLEncoder.encode(address, "UTF-8"))));
	}
	
	public static TupleValue locateCity(String city, String state) throws IOException, Pausable {
		return parseCSV(HTTPUtils.getURL(new URL(
			baseURL + "city=" + URLEncoder.encode(city, "UTF-8")
			+ "&state=" + URLEncoder.encode(state, "UTF-8"))));
	}
	
	public static TupleValue locateZip(String zip) throws IOException, Pausable {
		return parseCSV(HTTPUtils.getURL(new URL(
			baseURL + "zip=" + URLEncoder.encode(zip, "UTF-8"))));
	}
}
