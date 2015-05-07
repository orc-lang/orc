package orc.lib.music_calendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.PartialSite;

import org.htmlparser.Parser;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.StringFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

/**
 * @author tfinster
 */
public class MySpace extends DotSite {
    
    @Override
    protected void addMembers() {
        addMember("scrapeMusicShows", new scrapeMusicMember());  
    }

    private class scrapeMusicMember extends PartialSite {
        
        private Map<String, Boolean> seenURLs = new HashMap<String, Boolean>();
        
        @Override
        public Object evaluate(Args args) throws TokenException {

            try {
                String myspaceURL = args.stringArg(0);
                
                /* stay silent if we've processed this URL before */
                if (seenURLs.containsKey(myspaceURL)) {
                    return null;
                }
                seenURLs.put(myspaceURL, true);
                
                Parser profileParser = new Parser(myspaceURL);
                
                NodeList list = profileParser.extractAllNodesThatMatch(new LinkRegexFilter("bandprofile.listAllShows"));
                
                if (list.size() != 1) {
                    return null;
                }
                
                LinkTag link = (LinkTag)list.elementAt(0);
                String showUrl = link.extractLink();
                
                return extractMusicShows(showUrl);
            } catch (ParserException e) {
            	throw new JavaException(e);
            }
        }
    }
    
    private List<MusicShow> extractMusicShows(String showUrl) throws ParserException {
        List<MusicShow> musicShows = new ArrayList<MusicShow>();

        Parser showParser = new Parser(showUrl);
        
        // extract band name
        String bandName = extractBandName(showUrl);
        
        // extract shows
        NodeList formList = showParser.extractAllNodesThatMatch(new TagNameFilter("form"));
        
        for (int i = 0; i < formList.size(); i++) {
            
            FormTag form = (FormTag)formList.elementAt(i);
            
            if (form.getAttribute("action") != null && form.getAttribute("action").contains("mycalendar")) {
                NodeList inputList = form.getFormInputs();
                
                MusicShow show = new MusicShow();
                show.setBandName(bandName);
                
                for (int j = 0; j < inputList.size(); j++) {
                    InputTag input = (InputTag)inputList.elementAt(j);
                    if (input.getAttribute("name") != null) {
                        if (input.getAttribute("name").equals("calEvtLocation")) {
                            show.setLocation(input.getAttribute("value"));
                        } else if (input.getAttribute("name").equals("calEvtTitle")) {
                            show.setTitle(input.getAttribute("value"));
                        } else if (input.getAttribute("name").equals("calEvtCity")) {
                            show.setCity(input.getAttribute("value"));
                        } else if (input.getAttribute("name").equals("calEvtState")) {
                            show.setState(input.getAttribute("value"));
                        } else if (input.getAttribute("name").equals("calEvtDateTime")) {
                            try {
                                DateFormat fmt = new SimpleDateFormat("MM-dd-yyyy HH:mm");
                                String dateStr = input.getAttribute("value");
                                show.setDate(fmt.parse(dateStr));
                            } catch (ParseException e) { }
                        }
                    }
                }
                musicShows.add(show);
            }
        }
        
        return musicShows;
    }

    private String extractBandName(String showUrl) throws ParserException {
        Parser showParser = new Parser(showUrl);
        NodeList bandNameList = showParser.extractAllNodesThatMatch(new StringFilter("All Shows for"));
        
        if (bandNameList.size() != 1) {
            throw new ParserException("Unable to extract band name");
        }
        
        TextNode bandNameNode = (TextNode)bandNameList.elementAt(0);
        String[] parts = bandNameNode.getText().split("All Shows for ");
        
        if (parts.length != 2) {
            throw new ParserException("Unable to extract band name");
        }

        return parts[1];
    }
}