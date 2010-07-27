package orc.lib.music_calendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.PartialSite;

import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.CssSelectorNodeFilter;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.StringFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class MySpace extends DotSite {
    
    @Override
    protected void addMembers() {
        addMember("scrapeMusicShows", new scrapeMusicMethod());  
    }

    private class scrapeMusicMethod extends PartialSite {
        
        private Map<String, Boolean> seenURLs = new HashMap<String, Boolean>();
        
        @Override
        public Object evaluate(Args args) {

            try {
                String myspaceURL = (String)args.getArg(0);
                
        		/* stay silent if we've processed this URL before */
        		if (seenURLs.containsKey(myspaceURL)) {
        			return null;
        		}
        		seenURLs.put(myspaceURL, true);

        		Parser profileParser = new Parser(myspaceURL);

        		NodeList list = profileParser.extractAllNodesThatMatch(new LinkRegexFilter("Events/1"));

        		if (list.size() != 1) {
        			return null;
        		}

        		LinkTag link = (LinkTag)list.elementAt(0);
        		String showUrl = link.extractLink();

        		List<MusicShow> musicShows = extractMusicShows(showUrl);
                return musicShows;
            } catch (TokenException e) {
            } catch (ParserException e) {
            }
            
            return null;
        }
    }
    
	private List<MusicShow> extractMusicShows(String showUrl) throws ParserException {
		List<MusicShow> musicShows = new ArrayList<MusicShow>();

		Parser showParser = new Parser(showUrl);

		// extract band name
		String bandName = extractBandName(showUrl);

		// extract shows
		NodeList showList = showParser.extractAllNodesThatMatch(new CssSelectorNodeFilter("div.event-info"));
		for(int i = 0; i < showList.size(); i++) {
			
			MusicShow show = new MusicShow();
			show.setBandName(bandName);
			
			Tag infoTag = (Tag) showList.elementAt(i);
			
			String title = infoTag.getFirstChild().getFirstChild().getFirstChild().getFirstChild().getText();
			show.setTitle(title);
			
			String location = infoTag.getFirstChild().getLastChild().getLastChild().getFirstChild().getText();
			String[] parts = location.split(", ");
			if (parts.length != 3) {
				continue;
			}
			show.setLocation(parts[0]);
			show.setCity(parts[1]);
			show.setState(parts[2]);
			
			String dateStr = infoTag.getChildren().elementAt(1).getFirstChild().getText();
			
			try {
				Calendar eventDate = Calendar.getInstance();
				Calendar today = Calendar.getInstance();
				
				// handle 'Today' and 'Tomorrow'
				if (dateStr.contains("Today") || dateStr.contains("Tomorrow")) {
					DateFormat fmt = new SimpleDateFormat("HH:mm aa");
					
					Date parsedTime;
					if (dateStr.contains("Today")) {
						parsedTime = fmt.parse(dateStr.split("Today @ ")[1]);
					} else {
						parsedTime = fmt.parse(dateStr.split("Tomorrow @ ")[1]);
					}

					eventDate.setTime(parsedTime);
					eventDate.set(Calendar.MONTH, today.get(Calendar.MONTH));
					eventDate.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
					
				} else {
					DateFormat fmt = new SimpleDateFormat("EEE, MMMM dd @ HH:mm a");
					Date parsedTime = fmt.parse(dateStr);	
					eventDate.setTime(parsedTime);	
				}
				
				eventDate.set(Calendar.YEAR, today.get(Calendar.YEAR));
				
				if (dateStr.contains("AM")) {
					eventDate.set(Calendar.AM_PM, Calendar.AM);
				} else {
					eventDate.set(Calendar.AM_PM, Calendar.PM);
				}
					
				show.setDate(eventDate.getTime());

			} catch (ParseException e) { 
				continue;
			}
			
			musicShows.add(show);
		}

		return musicShows;
	}

	private String extractBandName(String showUrl) throws ParserException {
		Parser showParser = new Parser(showUrl);
		NodeList bandNameList = showParser.extractAllNodesThatMatch(new StringFilter("Upcoming Shows"));

		TextNode bandNameNode = (TextNode)bandNameList.elementAt(0);
		String[] parts = bandNameNode.getText().split(" - Upcoming Shows");

		if (parts.length != 2) {
			throw new ParserException("Unable to extract band name");
		}

		return parts[0];
	}
}