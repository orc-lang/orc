package orc.lib.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.joda.time.DateTime;

import kilim.Pausable;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import orc.oauth.OAuthProvider;

import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.util.ServiceException;

/**
 * @author quark
 */
public class GoogleCalendar {
	private static final URL feedURL;
    static {
    	try {
    		feedURL = new URL("http://www.google.com/calendar/feeds/default/private/full");
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
		CalendarService service = new CalendarService("ut-google-calendar-dev");
		service.setAuthSubToken(accessor.accessToken,
				provider.getPrivateKey(consumer));
		return new GoogleCalendar(service);
    }
    
    public CalendarEventFeed getTomorrowEvents()
    throws IOException, ServiceException {
    	CalendarQuery query = new CalendarQuery(feedURL);
    	DateTime tomorrow = new DateTime().withTime(0, 0, 0, 0).plusDays(1);
    	query.setMinimumStartTime(new com.google.gdata.data.DateTime(tomorrow.getMillis()));
    	query.setMaximumStartTime(new com.google.gdata.data.DateTime(tomorrow.plusDays(1).getMillis()-1));
    	return service.query(query, CalendarEventFeed.class);
    }
}