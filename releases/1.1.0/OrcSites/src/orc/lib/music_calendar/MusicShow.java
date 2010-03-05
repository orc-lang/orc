//
// MusicShow.java -- Java class MusicShow
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.music_calendar;

import java.util.Date;

/**
 * @author tfinster
 */
public class MusicShow {

	private String bandName;
	private String title;
	private String location;
	private String city;
	private String state;
	private Date date;

	public String getBandName() {
		return bandName;
	}

	public void setBandName(final String bandName) {
		this.bandName = bandName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getCity() {
		return city;
	}

	public void setCity(final String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(final String state) {
		this.state = state;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(final Date date) {
		this.date = date;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(final String location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return String.format("Band [%s], Title [%s], Location [%s], City [%s], State [%s], Date [%s]", bandName, title, location, city, state, date);
	}

	public boolean isValid() {
		return bandName != null && title != null && location != null && city != null && state != null && date != null;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == null || !(other instanceof MusicShow)) {
			return false;
		}

		final MusicShow otherShow = (MusicShow) other;
		return this.title.equals(otherShow.title) && this.date.equals(otherShow.date);
	}

	@Override
	public int hashCode() {
		return this.title.hashCode() + this.date.hashCode();
	}
}
