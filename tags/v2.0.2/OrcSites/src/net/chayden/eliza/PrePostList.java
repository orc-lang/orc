package net.chayden.eliza;

import java.util.HashMap;
import java.util.Map;

/**
 *  Eliza prePost list.
 *  This list of pre-post entries is used to perform word transformations
 *  prior to or after other processing.
 */
public final class PrePostList {
	private final Map<String, String> map = new HashMap<String, String>();

    /**
     *  Add another entry to the list.
     */
    public void add(String src, String dest) {
    	map.put(src, dest);
    }

    /**
     *  Prnt the pre-post list.
     */
    public void print(int indent) {
    	for (Map.Entry<String, String> entry : map.entrySet()) {
            for (int i = 0; i < indent; i++) System.out.print(" ");
            System.out.println("pre-post: " + entry.getKey() + "  " + entry.getValue());
        }
    }

    /**
     *  Translate a string.
     *  If str matches a src string on the list,
     *  return he corresponding dest.
     *  If no match, return the input.
     */
    String xlate(String str) {
    	String out = map.get(str);
    	if (out == null) return str;
    	else return out;
    }

    /**
     *  Translate a string s.
     *  (1) Trim spaces off.
     *  (2) Break s into words.
     *  (3) For each word, substitute matching src word with dest.
     */
    public String translate(String s) {
        String lines[] = new String[2];
        String work = EString.trim(s);
        s = "";
        while (EString.match(work, "* *", lines)) {
            s += xlate(lines[0]) + " ";
            work = EString.trim(lines[1]);
        }
        s += xlate(work);
        return s;
    }
}
