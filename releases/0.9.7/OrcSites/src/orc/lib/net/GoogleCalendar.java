package orc.lib.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import kilim.Pausable;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import orc.oauth.OAuthProvider;
import orc.runtime.Kilim;

import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.calendar.CalendarEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.data.calendar.CalendarFeed;
import com.google.gdata.util.ServiceException;

/**
 * @author quark
 */
public class GoogleCalendar {
	private static final URL eventsURL;
	private static final URL calendarsURL;
    static {
    	try {
			eventsURL = new URL("http://www.google.com/calendar/feeds/default/private/full");
			calendarsURL = new URL("http://www.google.com/calendar/feeds/default/owncalendars/full");
		} catch (MalformedURLException e) {
			throw new AssertionError(e);
		}
    }
    
	CalendarService service;
    
    private GoogleCalendar(CalendarService service) {
    	this.service = service;
    }
    
    /**
     * Return a new authenticated Google calendar.
     * This cannot be a constructor because constructors are not pausable.
     */
    public static GoogleCalendar authenticate(OAuthProvider provider, String consumer)
    throws Pausable, Exception {
		OAuthAccessor accessor = provider.authenticate(consumer,
				OAuth.newList("scope", "http://www.google.com/calendar/feeds/"));
		CalendarService service = new CalendarService("orc-csres-utexas-edu");
		service.setAuthSubToken(accessor.accessToken,
				provider.getPrivateKey(consumer));
		return new GoogleCalendar(service);
    }
    
    private static <E> E runThreaded(Callable<E> thunk) throws Pausable, IOException, ServiceException {
    	try {
			return Kilim.runThreaded(thunk);
		} catch (IOException e) {
			throw e;
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			throw (RuntimeException)e;
		}
    }
    
    public CalendarEventFeed getEvents(final DateTime start, final DateTime end)
    throws Pausable, IOException, ServiceException {
		return runThreaded(new Callable<CalendarEventFeed>() {
			public CalendarEventFeed call() throws IOException, ServiceException {
		    	CalendarQuery query = new CalendarQuery(eventsURL);
		    	query.setMinimumStartTime(new com.google.gdata.data.DateTime(start.getMillis()));
		    	query.setMaximumStartTime(new com.google.gdata.data.DateTime(end.getMillis()-1));
		    	return service.query(query, CalendarEventFeed.class);
			}
		});
    }
    
    public DateTimeZone getDefaultTimeZone() throws Pausable, IOException, ServiceException {
    	return runThreaded(new Callable<DateTimeZone>() {
			public DateTimeZone call() throws IOException, ServiceException {
		    	CalendarFeed resultFeed = service.getFeed(calendarsURL, CalendarFeed.class);
		    	CalendarEntry calendar = resultFeed.getEntries().get(0);
		    	return DateTimeZone.forID(calendar.getTimeZone().getValue());
			}
    	});
    }
}