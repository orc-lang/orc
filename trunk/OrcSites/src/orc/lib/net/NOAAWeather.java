package orc.lib.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import kilim.ExitMsg;
import kilim.Mailbox;
import kilim.Pausable;
import kilim.Task;

import orc.lib.state.Interval;
import orc.runtime.Kilim;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * API for the NOAA Weather Forecasts service as described at http://www.weather.gov/forecasts/xml/.
 * 
 * @author quark
 */
public class NOAAWeather {
	private static String dailyURL = "http://www.weather.gov/forecasts/xml/sample_products/browser_interface/ndfdBrowserClientByDay.php?";
		
	/**
	 * parameters element. In addition to getting lists of values for each kind of parameter,
	 * also supports aggregates over the forecasted period (e.g. maximum and minimum temperature).
	 * 
	 * <p>TODO: support more parameters
	 */
	public static final class Forecast {
		private final Location location;
		private Temperature[] maxTemps = new Temperature[0];
		private Temperature[] minTemps = new Temperature[0];
		private final RainChance[] rains;
		private final Sky[] skies;
		
		private Forecast(Location location, HashMap<String,TimeLayout> layouts, Element parameterNode) {
			this.location = location;
			
			// minTemps and maxTemps
			NodeList tempNodes = parameterNode.getElementsByTagName("temperature");
			for (int i = 0; i < tempNodes.getLength(); ++i) {
				Element e = (Element)tempNodes.item(i);
				TimeLayout layout = layouts.get(e.getAttribute("time-layout"));
				ArrayList<Temperature> tempsList = new ArrayList<Temperature>();
				NodeList values = e.getElementsByTagName("value");
				for (int j = 0; j < values.getLength(); ++j) {
					Element v = (Element)values.item(j);
					if (!"true".equals(v.getAttribute("xsi:nil"))) {
						tempsList.add(new Temperature(layout.getTime(j), Integer.parseInt(v.getTextContent())));
					}
				}
				if (e.getAttribute("type").equals("minimum")) {
					minTemps = tempsList.toArray(new Temperature[0]);
				} else {
					maxTemps = tempsList.toArray(new Temperature[0]);
				}
			}
			
			// chance of rain
			NodeList rainNodes = parameterNode.getElementsByTagName("probability-of-precipitation");
			if (rainNodes.getLength() == 0) {
				rains = new RainChance[0];
			} else {
				Element e = (Element)rainNodes.item(0);
				TimeLayout layout = layouts.get(e.getAttribute("time-layout"));
				ArrayList<RainChance> rainsList = new ArrayList<RainChance>();
				NodeList values = e.getElementsByTagName("value");
				for (int j = 0; j < values.getLength(); ++j) {
					Element v = (Element)values.item(j);
					if (!"true".equals(v.getAttribute("xsi:nil"))) {
						rainsList.add(new RainChance(layout.getTime(j), Integer.parseInt(v.getTextContent())));
					}
				}
				rains = rainsList.toArray(new RainChance[0]);
			}
			
			// skies
			NodeList weatherNodes = parameterNode.getElementsByTagName("weather");
			if (weatherNodes.getLength() == 0) {
				skies = new Sky[0];
			} else {
				Element e = (Element)weatherNodes.item(0);
				TimeLayout layout = layouts.get(e.getAttribute("time-layout"));
				skies = new Sky[layout.size()];
				NodeList values = e.getElementsByTagName("weather-conditions");
				for (int j = 0; j < values.getLength(); ++j) {
					Element v = (Element)values.item(j);
					skies[j] = new Sky(layout.getTime(j), v.getAttribute("weather-summary"));
				}
			}
		}
		
		private Forecast(final Location location, final Temperature[] maxTemps, final Temperature[] minTemps, final RainChance[] rains, final Sky[] skies) {
			super();
			this.location = location;
			this.maxTemps = maxTemps;
			this.minTemps = minTemps;
			this.rains = rains;
			this.skies = skies;
		}

		public Location getLocation() {
			return location;
		}
		
		public Temperature[] getMaxTemperatures() {
			return maxTemps;
		}
		
		/** Get the maximum temperature over the entire forecast period, or null if none is available. */
		public Temperature getMaxTemperature() {
			Temperature max = null;
			for (int i = 0; i < maxTemps.length; ++i) {
				if (max == null) max = maxTemps[i];
				else if (max.compareTo(maxTemps[i]) < 0) max = maxTemps[i];
			}
			return max;
		}
		
		/** Get the minimum temperature over the entire forecast period, or null if none is available. */
		public Temperature getMinTemperature() {
			Temperature min = null;
			for (int i = 0; i < minTemps.length; ++i) {
				if (min == null) min = minTemps[i];
				else if (min.compareTo(minTemps[i]) < 0) min = minTemps[i];
			}
			return min;
		}
		
		public Temperature[] getMinTemperatures() {
			return minTemps;
		}
		
		public Sky[] getSkies() {
			return skies;
		}
		
		/** Get the maximum rain chance the forecast period, or null if none is available. */
		public RainChance getRainChance() {
			RainChance max = null;
			for (int i = 0; i < rains.length; ++i) {
				if (max == null) max = rains[i];
				else if (max.compareTo(rains[i]) < 0) max = rains[i];
			}
			return max;
		}
		
		public RainChance[] rainChances() {
			return rains;
		}
		
		/** Return a new forecast restricted to the given period, which should be a subset of the current forecast's period. */
		public Forecast during(Interval<DateTime> time) {
			return new Forecast(location,
					NOAAWeather.during(time, maxTemps),
					NOAAWeather.during(time, minTemps),
					NOAAWeather.during(time, rains),
					NOAAWeather.during(time, skies));
		}
		
		public String toString() {
			return "Skies: " + Arrays.toString(getSkies()) +
				"; High: " + getMaxTemperature() +
				"; Low: " + getMinTemperature() +
				"; Chance of rain: " + getRainChance();
		}
	}
	
	/** location element */
	public static final class Location {
		private final String key;
		private final double latitude;
		private final double longitude;
		private Location(Element e) {
			Element keyNode = (Element)e.getElementsByTagName("location-key").item(0);
			Element point = (Element)e.getElementsByTagName("point").item(0);
			this.key = keyNode.getTextContent();
			this.latitude = Double.parseDouble(point.getAttribute("latitude"));
			this.longitude = Double.parseDouble(point.getAttribute("longitude"));
		}
		public synchronized String getKey() {
			return key;
		}
		public synchronized double getLatitude() {
			return latitude;
		}
		public synchronized double getLongitude() {
			return longitude;
		}
	}
	
	/** Base class for all forecast parameters. */
	private static abstract class Parameter {
		private final Interval<DateTime> time;
		protected Parameter(Interval<DateTime> time) {
			this.time = time;
		}
		public Interval<DateTime> getTime() {
			return time;
		}
	}
	
	/** temperature element */
	public static final class Temperature extends Parameter implements Comparable<Temperature> {
		private final int degreesF;
		private Temperature(Interval<DateTime> time, int degreesF) {
			super(time);
			this.degreesF = degreesF;
		}
		public int getDegreesF() {
			return degreesF;
		}
		public int compareTo(Temperature o) {
			return degreesF < o.degreesF ? -1 : degreesF > o.degreesF ? 1 : 0;
		}
		public String toString() {
			return degreesF + "F";
		}
	}
	
	/** probability-of-precipitation element */
	public static final class RainChance extends Parameter implements Comparable<RainChance> {
		private final int percent;
		private RainChance(Interval<DateTime> time, int percent) {
			super(time);
			this.percent = percent;
		}
		public int getPercent() {
			return percent;
		}
		public int compareTo(RainChance o) {
			return percent < o.percent ? -1 : percent > o.percent ? 1 : 0;
		}
		public String toString() {
			return percent + "%";
		}
	}
	
	/** weather-conditions element */
	public static final class Sky extends Parameter {
		private final String summary;
		private Sky(Interval<DateTime> time, String summary) {
			super(time);
			this.summary = summary;
		}
		
		public String getSummary() {
			return summary;
		}
		public String toString() {
			return summary;
		}
	}
	
	/**
	 * Represent a time-layout XML element with a list of time ranges.
	 * @author quark
	 */
	private static final class TimeLayout {
		private final String key;
		private final Interval<DateTime>[] times; 
		@SuppressWarnings("unchecked")
		private TimeLayout(Element e) {
			key = e.getElementsByTagName("layout-key").item(0).getTextContent();
			NodeList starts = e.getElementsByTagName("start-valid-time");
			NodeList ends = e.getElementsByTagName("end-valid-time");
			times = new Interval[starts.getLength()];
			for (int i = 0; i < starts.getLength(); ++i) {
				times[i] = new Interval<DateTime>(
					new DateTime(starts.item(i).getTextContent()),
					new DateTime(ends.item(i).getTextContent()));
			}
		}
		
		public String getKey() {
			return key;
		}
		
		public Interval<DateTime> getTime(int index) {
			return times[index];
		}
		
		public int size() {
			return times.length;
		}
	}
	
	/** Restrict an array of parameters to those overlapping the given range. */
	@SuppressWarnings("unchecked")
	private static <E extends Parameter> E[] during(Interval<DateTime> time, E[] parameters) {
		ArrayList<E> out = new ArrayList<E>();
		for (int i = 0; i < parameters.length; ++i) {
			if (time.intersects(parameters[i].time)) out.add(parameters[i]);
		}
		return (E[])out.toArray();
	}
	
	/**
	 * Parse an XML document to extract time layouts indexed by key.
	 */
	private static HashMap<String, TimeLayout> getTimeLayouts(Document doc) {
		// create time layouts
		NodeList layoutNodes = doc.getElementsByTagName("time-layout");
		HashMap<String, TimeLayout> layouts = new HashMap<String, TimeLayout>();
		for (int i = 0; i < layoutNodes.getLength(); ++i) {
			TimeLayout layout = new TimeLayout((Element)layoutNodes.item(i));
			layouts.put(layout.getKey(), layout);
		}
		return layouts;
	}
	
	/**
	 * Parse an XML document to extract locations indexed by key.
	 */
	private static HashMap<String, Location> getLocations(Document doc) {
		// create time layouts
		NodeList locationNodes = doc.getElementsByTagName("location");
		HashMap<String, Location> locations = new HashMap<String, Location>();
		for (int i = 0; i < locationNodes.getLength(); ++i) {
			Location location = new Location((Element)locationNodes.item(i));
			locations.put(location.getKey(), location);
		}
		return locations;
	}
	
	/**
	 * Parse an XML document into an array of forecasts.
	 */
	private static Forecast[] getForecasts(Document doc) {
		HashMap<String,Location> locations = getLocations(doc);
		HashMap<String,TimeLayout> layouts = getTimeLayouts(doc);
		
		NodeList parameterNodes = doc.getElementsByTagName("parameters");
		Forecast[] out = new Forecast[parameterNodes.getLength()];
		for (int i = 0; i < parameterNodes.getLength(); ++i) {
			Element parameterNode = (Element)parameterNodes.item(i);
			out[i] = new Forecast(
				locations.get(parameterNode.getAttribute("applicable-location")),
				layouts,
				parameterNode);
		}
		
		return out;
	}
	
	/**
	 * Call the NDFDgenByDay() service, with a 24-hour period;
	 * exit if no forecast is available.
	 */
	public static Forecast getDailyForecast(double lat, double lon, LocalDate startDate, int numDays) throws IOException, SAXException, Pausable {
		final URL url;
		try {
			url = new URL(dailyURL +
					"&lat=" + lat +
					"&lon=" + lon +
					"&format=24+hourly" +
					"&startDate=" + startDate +
					"&numDays=" + numDays);
		} catch (MalformedURLException e) {
			// impossible
			throw new AssertionError(e);
		}
		Document doc = XMLUtils.getURL(url);
		Forecast[] fcs = getForecasts(doc);
		if (fcs.length == 0) {
			Kilim.exit();
			return null;
		}
		else return fcs[0];
	}
	
	public static void main(String[] args) {
		Kilim.startEngine(1, 1);
		final Mailbox<ExitMsg> m = new Mailbox<ExitMsg>();
		Task task = new Task() {
			@Override
			public void execute() throws Pausable, Exception {
				Forecast forecast = getDailyForecast(47.606, -122.331, new LocalDate(), 1);
				System.out.println(forecast);
			}
		};
		task.informOnExit(m);
		task.start();
		m.getb();
		Kilim.stopEngine();
	}
}
