package orc.lib.music_calendar;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import orc.error.JavaException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Value;

import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.extensions.When;
import com.google.gdata.data.extensions.Where;
import com.google.gdata.util.ServiceException;

/**
 * @author tfinster
 */
public class GoogleCalendar extends DotSite {
    
    private static final String METAFEED_URL_BASE = "http://www.google.com/calendar/feeds/";
    private static final String EVENT_FEED_URL_SUFFIX = "/private/full";
    
    @Override
    protected void addMethods() {
        addMethod("addMusicShows", new addMusicShowsMethod());  
    }

    private class addMusicShowsMethod extends EvalSite {
        
        @Override
        public Value evaluate(Args args) throws TokenException {

            try {
            	String userName = args.stringArg(0);
            	String userPassword = args.stringArg(1);
                List<MusicShow> shows = (List<MusicShow>)args.getArg(2);
                
                CalendarService service = new CalendarService("OrchestratedMusicCalendar");
                service.setUserCredentials(userName, userPassword);
                URL eventFeedUrl = new URL(METAFEED_URL_BASE + userName + EVENT_FEED_URL_SUFFIX);

                for (MusicShow show : shows) {
                    Calendar startDate = new GregorianCalendar();
                    startDate.setTime(show.getDate());
                    
                    Calendar endDate = new GregorianCalendar();
                    endDate.setTime(show.getDate());
                    endDate.add(Calendar.MINUTE, 90);
                    
                    String location = String.format("%s, %s, %s", show.getLocation(), show.getCity(), show.getState());
                    
                    addEventToCalendar(service, show.getTitle(), show.getTitle(), location, startDate, endDate, eventFeedUrl);
                }
            } catch (ServiceException e) {
            	throw new JavaException(e);
            } catch (IOException e) {
            	throw new JavaException(e);
            } catch (ClassCastException e) {
            	throw new JavaException(e);
            }
            
            return Value.signal();
        }
    }
    
    private void addEventToCalendar(CalendarService service, String eventTitle, String eventContent, String location, Calendar startDate, Calendar endDate, URL eventFeedUrl) throws ServiceException, IOException {
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

        service.insert(eventFeedUrl, myEntry);
    }
}