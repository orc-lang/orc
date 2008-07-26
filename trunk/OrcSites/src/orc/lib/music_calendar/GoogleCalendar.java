package orc.lib.music_calendar;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import kilim.pausable;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import orc.error.ArgumentTypeMismatchException;
import orc.error.JavaException;
import orc.error.TokenException;
import orc.oauth.OAuthProvider;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.java.ObjectProxy;
import orc.runtime.values.Value;

import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.extensions.When;
import com.google.gdata.data.extensions.Where;
import com.google.gdata.util.ServiceException;

/**
 * @author tfinster, quark
 */
public class GoogleCalendar extends EvalSite {
    private static final URL eventFeedURL;
    static {
    	try {
			eventFeedURL = new URL("http://www.google.com/calendar/feeds/default/private/full");
		} catch (MalformedURLException e) {
			throw new AssertionError(e);
		}
    }
	
    /**
     * This implements the main Google Calendar stuff.
     * @author quark
     */
	public static class GoogleCalendarInstance {
		private String consumer;
		private CalendarService service;
		private boolean authenticated = false;
		private OAuthProvider provider;
		/**
		 * @param properties location of the resource defining OAuth properties
		 * @param consumer name of the Google consumer in the properties file
		 * @throws IOException if properties cannot be loaded
		 */
		public GoogleCalendarInstance(OAuthProvider provider, String consumer) throws IOException {
			this.provider = provider;
			this.consumer = consumer;
            this.service = new CalendarService("ut-OrcMusicCalendar-dev");
		}
		
		/**
		 * Authenticate with Google using OAuth.
		 */
		public @pausable void authenticate() throws Exception {
			OAuthAccessor accessor = provider.authenticate(consumer,
					OAuth.newList("scope", "http://www.google.com/calendar/feeds/"));
			service.setAuthSubToken(accessor.accessToken,
					provider.getPrivateKey(consumer));
			synchronized (this) {
				authenticated = true;
			}
		}
		
		/** Add a music show record. */
		public @pausable void addMusicShow(final MusicShow show) throws Exception {
			synchronized (this) {
				if (!authenticated) {
					throw new OAuthException("Not authenticated.");
				}
			}
            final Calendar startDate = new GregorianCalendar();
            startDate.setTime(show.getDate());
                    
            final Calendar endDate = new GregorianCalendar();
            endDate.setTime(show.getDate());
            endDate.add(Calendar.MINUTE, 90);
                    
            final String location = String.format("%s, %s, %s", show.getLocation(), show.getCity(), show.getState());
            
            Kilim.runThreaded(new Callable<Object>() {
            	public Object call() throws Exception {
            		addEventToCalendar(show.getTitle(), show.getTitle(), location, startDate, endDate);
            		// return a signal to indicate that the method finished
            		return new Object();
            	}
            });
		}
    
		/**
		 * Actually add an event to a calendar. BLOCKING.
		 */
        private void addEventToCalendar(String eventTitle, String eventContent, String location,
        		Calendar startDate, Calendar endDate)
        throws ServiceException, IOException {
            When eventTimes = new When();
            eventTimes.setStartTime(new DateTime(startDate.getTime(), TimeZone.getDefault()));
            eventTimes.setEndTime(new DateTime(endDate.getTime(), TimeZone.getDefault()));
            
            Where where = new Where();
            where.setValueString(location);
            
            CalendarEventEntry myEntry = new CalendarEventEntry();
    
            myEntry.setTitle(new PlainTextConstruct(eventTitle));
            myEntry.setContent(new PlainTextConstruct(eventContent));
            myEntry.addLocation(where);
            myEntry.addTime(eventTimes);
    
            service.insert(eventFeedURL, myEntry);
        }
	}

	@Override
	public Value evaluate(Args args) throws TokenException {
		try {
			return new ObjectProxy(new GoogleCalendarInstance(
					(OAuthProvider)args.getArg(0),
					args.stringArg(1)));
		} catch (IOException e) {
			throw new JavaException(e);
		} catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(e);
		}
	}
}