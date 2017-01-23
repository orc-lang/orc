//
// NOAAWeather.java -- Java class NOAAWeather
// Project OrcSites
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import orc.lib.state.Interval;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * API for the NOAA Weather Forecasts service as described at
 * http://www.weather.gov/forecasts/xml/.
 *
 * @author quark
 */
public class NOAAWeather {
    private static String dailyURL = "http://www.weather.gov/forecasts/xml/sample_products/browser_interface/ndfdBrowserClientByDay.php?";

    /**
     * parameters element. In addition to getting lists of values for each kind
     * of parameter, also supports aggregates over the forecasted period (e.g.
     * maximum and minimum temperature).
     * <p>
     * TODO: support more parameters
     */
    public static final class Forecast {
        private final Location location;
        private Temperature[] maxTemps = new Temperature[0];
        private Temperature[] minTemps = new Temperature[0];
        private final RainChance[] rains;
        private final Sky[] skies;

        private Forecast(final Location location, final HashMap<String, TimeLayout> layouts, final Element parameterNode) {
            this.location = location;

            // minTemps and maxTemps
            final NodeList tempNodes = parameterNode.getElementsByTagName("temperature");
            for (int i = 0; i < tempNodes.getLength(); ++i) {
                final Element e = (Element) tempNodes.item(i);
                final TimeLayout layout = layouts.get(e.getAttribute("time-layout"));
                final ArrayList<Temperature> tempsList = new ArrayList<Temperature>();
                final NodeList values = e.getElementsByTagName("value");
                for (int j = 0; j < values.getLength(); ++j) {
                    final Element v = (Element) values.item(j);
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
            final NodeList rainNodes = parameterNode.getElementsByTagName("probability-of-precipitation");
            if (rainNodes.getLength() == 0) {
                rains = new RainChance[0];
            } else {
                final Element e = (Element) rainNodes.item(0);
                final TimeLayout layout = layouts.get(e.getAttribute("time-layout"));
                final ArrayList<RainChance> rainsList = new ArrayList<RainChance>();
                final NodeList values = e.getElementsByTagName("value");
                for (int j = 0; j < values.getLength(); ++j) {
                    final Element v = (Element) values.item(j);
                    if (!"true".equals(v.getAttribute("xsi:nil"))) {
                        rainsList.add(new RainChance(layout.getTime(j), Integer.parseInt(v.getTextContent())));
                    }
                }
                rains = rainsList.toArray(new RainChance[0]);
            }

            // skies
            final NodeList weatherNodes = parameterNode.getElementsByTagName("weather");
            if (weatherNodes.getLength() == 0) {
                skies = new Sky[0];
            } else {
                final Element e = (Element) weatherNodes.item(0);
                final TimeLayout layout = layouts.get(e.getAttribute("time-layout"));
                skies = new Sky[layout.size()];
                final NodeList values = e.getElementsByTagName("weather-conditions");
                for (int j = 0; j < values.getLength(); ++j) {
                    final Element v = (Element) values.item(j);
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

        /**
         * Get the maximum temperature over the entire forecast period, or null
         * if none is available.
         */
        public Temperature getMaxTemperature() {
            Temperature max = null;
            for (final Temperature maxTemp : maxTemps) {
                if (max == null) {
                    max = maxTemp;
                } else if (max.compareTo(maxTemp) < 0) {
                    max = maxTemp;
                }
            }
            return max;
        }

        /**
         * Get the minimum temperature over the entire forecast period, or null
         * if none is available.
         */
        public Temperature getMinTemperature() {
            Temperature min = null;
            for (final Temperature minTemp : minTemps) {
                if (min == null) {
                    min = minTemp;
                } else if (min.compareTo(minTemp) < 0) {
                    min = minTemp;
                }
            }
            return min;
        }

        public Temperature[] getMinTemperatures() {
            return minTemps;
        }

        public Sky[] getSkies() {
            return skies;
        }

        /**
         * Get the maximum rain chance the forecast period, or null if none is
         * available.
         */
        public RainChance getRainChance() {
            RainChance max = null;
            for (final RainChance rain : rains) {
                if (max == null) {
                    max = rain;
                } else if (max.compareTo(rain) < 0) {
                    max = rain;
                }
            }
            return max;
        }

        public RainChance[] rainChances() {
            return rains;
        }

        /**
         * Return a new forecast restricted to the given period, which should be
         * a subset of the current forecast's period.
         */
        public Forecast during(final Interval<DateTime> time) {
            return new Forecast(location, NOAAWeather.during(time, maxTemps), NOAAWeather.during(time, minTemps), NOAAWeather.during(time, rains), NOAAWeather.during(time, skies));
        }

        @Override
        public String toString() {
            return "Skies: " + Arrays.toString(getSkies()) + "; High: " + getMaxTemperature() + "; Low: " + getMinTemperature() + "; Chance of rain: " + getRainChance();
        }
    }

    /** location element */
    public static final class Location {
        private final String key;
        private final double latitude;
        private final double longitude;

        private Location(final Element e) {
            final Element keyNode = (Element) e.getElementsByTagName("location-key").item(0);
            final Element point = (Element) e.getElementsByTagName("point").item(0);
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

        protected Parameter(final Interval<DateTime> time) {
            this.time = time;
        }

        public Interval<DateTime> getTime() {
            return time;
        }
    }

    /** temperature element */
    public static final class Temperature extends Parameter implements Comparable<Temperature> {
        private final int degreesF;

        private Temperature(final Interval<DateTime> time, final int degreesF) {
            super(time);
            this.degreesF = degreesF;
        }

        public int getDegreesF() {
            return degreesF;
        }

        @Override
        public int compareTo(final Temperature o) {
            return degreesF < o.degreesF ? -1 : degreesF > o.degreesF ? 1 : 0;
        }

        @Override
        public String toString() {
            return degreesF + "F";
        }
    }

    /** probability-of-precipitation element */
    public static final class RainChance extends Parameter implements Comparable<RainChance> {
        private final int percent;

        private RainChance(final Interval<DateTime> time, final int percent) {
            super(time);
            this.percent = percent;
        }

        public int getPercent() {
            return percent;
        }

        @Override
        public int compareTo(final RainChance o) {
            return percent < o.percent ? -1 : percent > o.percent ? 1 : 0;
        }

        @Override
        public String toString() {
            return percent + "%";
        }
    }

    /** weather-conditions element */
    public static final class Sky extends Parameter {
        private final String summary;

        private Sky(final Interval<DateTime> time, final String summary) {
            super(time);
            this.summary = summary;
        }

        public String getSummary() {
            return summary;
        }

        @Override
        public String toString() {
            return summary;
        }
    }

    /**
     * Represent a time-layout XML element with a list of time ranges.
     * 
     * @author quark
     */
    private static final class TimeLayout {
        private final String key;
        private final Interval<DateTime>[] times;

        @SuppressWarnings("unchecked")
        private TimeLayout(final Element e) {
            key = e.getElementsByTagName("layout-key").item(0).getTextContent();
            final NodeList starts = e.getElementsByTagName("start-valid-time");
            final NodeList ends = e.getElementsByTagName("end-valid-time");
            times = new Interval[starts.getLength()];
            for (int i = 0; i < starts.getLength(); ++i) {
                times[i] = new Interval<DateTime>(new DateTime(starts.item(i).getTextContent()), new DateTime(ends.item(i).getTextContent()));
            }
        }

        public String getKey() {
            return key;
        }

        public Interval<DateTime> getTime(final int index) {
            return times[index];
        }

        public int size() {
            return times.length;
        }
    }

    /** Restrict an array of parameters to those overlapping the given range. */
    @SuppressWarnings("unchecked")
    private static <E extends Parameter> E[] during(final Interval<DateTime> time, final E[] parameters) {
        final ArrayList<E> out = new ArrayList<E>();
        for (final E parameter : parameters) {
            if (time.intersects(parameter.getTime())) {
                out.add(parameter);
            }
        }
        return (E[]) out.toArray();
    }

    /**
     * Parse an XML document to extract time layouts indexed by key.
     */
    private static HashMap<String, TimeLayout> getTimeLayouts(final Document doc) {
        // create time layouts
        final NodeList layoutNodes = doc.getElementsByTagName("time-layout");
        final HashMap<String, TimeLayout> layouts = new HashMap<String, TimeLayout>();
        for (int i = 0; i < layoutNodes.getLength(); ++i) {
            final TimeLayout layout = new TimeLayout((Element) layoutNodes.item(i));
            layouts.put(layout.getKey(), layout);
        }
        return layouts;
    }

    /**
     * Parse an XML document to extract locations indexed by key.
     */
    private static HashMap<String, Location> getLocations(final Document doc) {
        // create time layouts
        final NodeList locationNodes = doc.getElementsByTagName("location");
        final HashMap<String, Location> locations = new HashMap<String, Location>();
        for (int i = 0; i < locationNodes.getLength(); ++i) {
            final Location location = new Location((Element) locationNodes.item(i));
            locations.put(location.getKey(), location);
        }
        return locations;
    }

    /**
     * Parse an XML document into an array of forecasts.
     */
    private static Forecast[] getForecasts(final Document doc) {
        final HashMap<String, Location> locations = getLocations(doc);
        final HashMap<String, TimeLayout> layouts = getTimeLayouts(doc);

        final NodeList parameterNodes = doc.getElementsByTagName("parameters");
        final Forecast[] out = new Forecast[parameterNodes.getLength()];
        for (int i = 0; i < parameterNodes.getLength(); ++i) {
            final Element parameterNode = (Element) parameterNodes.item(i);
            out[i] = new Forecast(locations.get(parameterNode.getAttribute("applicable-location")), layouts, parameterNode);
        }

        return out;
    }

    /**
     * Call the NDFDgenByDay() service, with a 24-hour period; exit if no
     * forecast is available.
     */
    public static Forecast getDailyForecast(final double lat, final double lon, final LocalDate startDate, final int numDays) throws IOException, SAXException {
        final URL url;
        try {
            url = new URL(dailyURL + "&lat=" + lat + "&lon=" + lon + "&format=24+hourly" + "&startDate=" + startDate + "&numDays=" + numDays);
        } catch (final MalformedURLException e) {
            // impossible
            throw new AssertionError(e);
        }
        final Document doc = XMLUtils.getURL(url);
        final Forecast[] fcs = getForecasts(doc);
        if (fcs.length == 0) {
            return null;
        } else {
            return fcs[0];
        }
    }

}
