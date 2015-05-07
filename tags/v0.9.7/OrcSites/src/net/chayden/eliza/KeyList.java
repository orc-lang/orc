package net.chayden.eliza;

import java.util.HashMap;
import java.util.Map;

/**
 *  Eliza key list.
 *  This stores all the keys.
 */
public class KeyList {
	private Map<String, Key> keys = new HashMap<String, Key>();

    /**
     *  Add a new key.
     */
    public void add(String key, int rank, DecompList decomp) {
    	keys.put(key, new Key(key, rank, decomp));
    }

    /**
     *  Print all the keys.
     */
    public void print(int indent) {
    	for (Key key : keys.values()) {
    		key.print(indent);
    	}
    }

    /**
     *  Search the key list for a given key.
     *  Return the Key if found, else null.
     */
    Key getKey(String s) {
    	return keys.get(s);
    }

    /**
     *  Break the string s into words.
     *  For each word, if isKey is true, then push the key
     *  into the stack.
     */
    public void buildKeyStack(KeyStack stack, String s) {
        stack.reset();
        s = EString.trim(s);
        String lines[] = new String[2];
        Key k;
        while (EString.match(s, "* *", lines)) {
            k = getKey(lines[0]);
            if (k != null) stack.pushKey(k);
            s = lines[1];
        }
        k = getKey(s);
        if (k != null) stack.pushKey(k);
        //stack.print();
    }
}
