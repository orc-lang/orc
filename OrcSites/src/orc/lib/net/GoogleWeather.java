package orc.lib.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kilim.Pausable;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.KilimSite;

/**
 * API for http://geocoder.us. This service returns a latitude/longitude tuple
 * given a city/state, zip code, or full address in the United States.
 * 
 * @author quark
 */
public class GoogleWeather extends KilimSite {
	private static String baseURL = "http://www.google.com/search?";
	private static Pattern p = Pattern.compile(
			"src=\"/images/weather/([a-z_]+).gif\"[^>]+><br><nobr>([0-9]+)°F \\| ([0-9]+)°F</nobr>",
			Pattern.DOTALL);
	
	public static class WeatherCondition {
		public final String sky;
		public final int low;
		public final int high;
		
		public WeatherCondition(final String sky, final int low, final int high) {
			this.sky = sky;
			this.low = low;
			this.high = high;
		}
		
		public String toString() {
			return "(" + sky + "," + low + "," + high +")";
		}
	}
	
	@Override
	public Object evaluate(Args args) throws TokenException, Pausable {
		try {
			return parse(HTTPUtils.getURL(new URL(baseURL + "q=weather+" + URLEncoder.encode(args.stringArg(0), "UTF-8"))));
		} catch (MalformedURLException e) {
			throw new AssertionError(e);
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		} catch (IOException e) {
			throw new JavaException(e);
		}
	}
	
	public WeatherCondition[] parse(String html) {
		Matcher m = p.matcher(html);
		LinkedList<WeatherCondition> out = new LinkedList<WeatherCondition>();
		while (m.find()) {
			out.add(new WeatherCondition(m.group(1),
					Integer.parseInt(m.group(3)),
					Integer.parseInt(m.group(2))));
		}
		return out.toArray(new WeatherCondition[0]);
	}
}
