//
// GoogleCalendar.java -- Java class GoogleCalendar
// Project OrcSites
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.calendar.CalendarEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.data.calendar.CalendarFeed;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.oauth.OAuthAccessor;

import orc.oauth.OAuthProvider;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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
        } catch (final MalformedURLException e) {
            throw new AssertionError(e);
        }
    }

    CalendarService service;

    public GoogleCalendar(final OAuthProvider provider, final OAuthAccessor accessor, final String consumer) throws Exception {
        service = new CalendarService("orc-csres-utexas-edu");
        service.setAuthSubToken(accessor.accessToken, provider.getPrivateKey(consumer));
    }

    public CalendarEventFeed getEvents(final DateTime start, final DateTime end) throws IOException, ServiceException {
        final CalendarQuery query = new CalendarQuery(eventsURL);
        query.setMinimumStartTime(new com.google.gdata.data.DateTime(start.getMillis()));
        query.setMaximumStartTime(new com.google.gdata.data.DateTime(end.getMillis() - 1));
        return service.query(query, CalendarEventFeed.class);
    }

    public DateTimeZone getDefaultTimeZone() throws IOException, ServiceException {
        final CalendarFeed resultFeed = service.getFeed(calendarsURL, CalendarFeed.class);
        final CalendarEntry calendar = resultFeed.getEntries().get(0);
        return DateTimeZone.forID(calendar.getTimeZone().getValue());
    }
}
