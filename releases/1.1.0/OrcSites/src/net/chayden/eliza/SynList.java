package net.chayden.eliza;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *  Eliza synonym list.
 *  Collection of all the synonym elements.
 */
public final class SynList {
	/** Index word lists by their contents. */
	private final Map<String, WordList> index = new HashMap<String, WordList>();
	private final List<WordList> wordLists = new LinkedList<WordList>();

    /**
     *  Add another word list the the synonym list.
     */
    public void add(WordList words) {
    	wordLists.add(words);
    	for (String word : words) {
    		index.put(word, words);
    	}
    }

    /**
     *  Prnt the synonym lists.
     */
    public void print(int indent) {
    	for (WordList words : wordLists) {
            for (int j = 0; j < indent; j++) System.out.print(" ");
            System.out.print("synon: ");
            words.print(indent);
        }
    }

    /**
     *  Find a synonym word list given the any word in it.
     */
    public WordList find(String s) {
    	return index.get(s);
    }
    /**
     *  Decomposition match,
     *  If decomp has no synonyms, do a regular match.
     *  Otherwise, try all synonyms.
     */
    boolean matchDecomp(String str, String pat, String lines[]) {
        if (! EString.match(pat, "*@* *", lines)) {
            //  no synonyms in decomp pattern
            return EString.match(str, pat, lines);
        }
        //  Decomp pattern has synonym -- isolate the synonym
        String first = lines[0];
        String synWord = lines[1];
        String theRest = " " + lines[2];
        //  Look up the synonym
        WordList syn = find(synWord);
        if (syn == null) {
            System.out.println("Could not fnd syn list for " + synWord);
            return false;
        }
        for (String word : syn) {
            //  Make a modified pattern
            pat = first + word + theRest;
            if (EString.match(str, pat, lines)) {
                int n = EString.count(first, '*');
                //  Make room for the synonym in the match list.
                for (int j = lines.length-2; j >= n; j--)
                    lines[j+1] = lines[j];
                //  The synonym goes in the match list.
                lines[n] = word;
                return true;
            }
        }
        return false;
    }

}
