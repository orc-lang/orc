package orc.lib.music_calendar;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.swing.JOptionPane;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;

import kilim.Mailbox;
import kilim.Task;
import kilim.pausable;

import orc.error.JavaException;
import orc.error.TokenException;
import orc.oauth.SimpleOAuth;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.java.ObjectProxy;
import orc.runtime.values.Value;

import com.centerkey.utils.BareBonesBrowserLaunch;
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
		private SimpleOAuth oauth;
		private String consumer;
		private CalendarService service;
		private boolean authenticated = false;
		/**
		 * @param properties location of the resource defining OAuth properties
		 * @param consumer name of the Google consumer in the properties file
		 * @throws IOException if properties cannot be loaded
		 */
		public GoogleCalendarInstance(String properties, String consumer) throws IOException {
			oauth = new SimpleOAuth(properties);
			this.consumer = consumer;
            this.service = new CalendarService("ut-OrcMusicCalendar-dev");
		}
		
		/**
		 * Authenticate with Google using OAuth.
		 * @throws Exception
		 */
		public @pausable void authenticate() throws OAuthException, IOException {
			// This whole method is boilerplate to do
			// the authentication in a separate thread, using
			// mailboxes to send either a value or exception back.
			// I suspect this is a common idiom with Kilim.
			final Mailbox<String> value = new Mailbox<String>();
			final Mailbox<IOException> ioexception = new Mailbox<IOException>();
			final Mailbox<OAuthException> oauthexception = new Mailbox<OAuthException>();
			new Thread() {
				public void run() {
					try {
						value.putb(getAuthSubToken());
					} catch (IOException e) {
						ioexception.putb(e);
					} catch (OAuthException e) {
						oauthexception.putb(e);
					}
				}
			}.start();
			// Eclipse complains if I don't create the varargs array explicitly
			switch (Mailbox.select(new Mailbox[]{ioexception, oauthexception, value})) {
			case 0: throw ioexception.get();
			case 1: throw oauthexception.get();
			case 2:
    			String authSubToken = value.get();
    			if (authSubToken == null) {
    				Task.exit("Could not get authSubToken");
    				return;
    			} else {
                    service.setAuthSubToken(authSubToken, oauth.getPrivateKey(consumer));
        			synchronized (this) {
                        authenticated = true;
        			}
    			}
			}
		}
		
		/**
		 * Get an authsub token from google. BLOCKING.
		 */
		private String getAuthSubToken() throws IOException, OAuthException {
			OAuthAccessor accessor = oauth.newAccessor(consumer);
			oauth.setRequestToken(accessor,
					OAuth.newList("scope", "http://www.google.com/calendar/feeds/"));
			// prompt the user for authorization
			BareBonesBrowserLaunch.openURL(
					 oauth.getAuthorizationURL(accessor).toExternalForm());
			 int ok = JOptionPane.showConfirmDialog(null,
					 "Your browser should open and ask you to" +
					 "confirm authorization.\n\nPlease click OK once " +
					 "you have confirmed authorization.");
			 if (ok != 0) return null;
			 // confirm authorization
			oauth.setAccessToken(accessor);
			return accessor.accessToken;
		}
		
		/** Add a music show record. */
		public @pausable void addMusicShow(final MusicShow show)
		throws OAuthException, ServiceException, IOException {
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
                
			final Mailbox<Object> value = new Mailbox<Object>();
			final Mailbox<IOException> ioexception = new Mailbox<IOException>();
			final Mailbox<ServiceException> serviceexception = new Mailbox<ServiceException>();
            new Thread() {
            	public void run() {
            		try {
                        addEventToCalendar(show.getTitle(), show.getTitle(), location, startDate, endDate);
                        value.putb(new Object());
            		} catch (IOException e) {
            			ioexception.putb(e);
            		} catch (ServiceException e) {
            			serviceexception.putb(e);
            		}
            	}
            }.start();
			switch (Mailbox.select(new Mailbox[]{ioexception, serviceexception, value})) {
			case 0: throw ioexception.get();
			case 1: throw serviceexception.get();
			case 2: value.get();
			}
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
					// force resource path to be relative to the root
					"/" + args.stringArg(0),
					args.stringArg(1)));
		} catch (IOException e) {
			throw new JavaException(e);
		}
	}
}