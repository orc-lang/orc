package net.chayden.eliza;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *  Eliza word list.
 */
public final class WordList implements Iterable<String> {
	private final Set<String> words = new HashSet<String>(); 
	public void add(String word) {
		words.add(word);
	}
    /**
     *  Print a word list on one line.
     */
    public void print(int indent) {
    	for (String word : words) {
            System.out.print(word + "  ");
        }
        System.out.println();
    }

    /**
     *  Find a string in a word list.
     *  Return true if the word is in the list, false otherwise.
     */
    boolean find(String s) {
    	return words.contains(s);
    }
	@Override
	public Iterator<String> iterator() {
		return words.iterator();
	}
}
