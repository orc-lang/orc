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
    public void setBandName(String bandName) {
        this.bandName = bandName;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String toString() {
        return String.format("Band [%s], Title [%s], Location [%s], City [%s], State [%s], Date [%s]", bandName, title, location, city, state, date);
    }
    
    public boolean isValid() {
        return bandName != null && title != null && location != null && city != null && state != null && date != null;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof MusicShow)) {
            return false;
        }
        
        MusicShow otherShow = (MusicShow)other;
        return this.title.equals(otherShow.title) && this.date.equals(otherShow.date);
    }
    
    @Override
    public int hashCode() {
        return this.title.hashCode() + this.date.hashCode();
    }
}