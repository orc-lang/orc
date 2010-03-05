package net.chayden.eliza;

import java.awt.*;

/**
 *  Eliza Application.
 */
public class ElizaApp {

    static String scriptPathname = "script";
    static String testPathname = "test";
    static String scriptURL = "http://www.monmouth.com/~chayden/eliza/script";
    //static String scriptURL = "http://www-gbcs.mt.att.com/~cch/eliza/script";

    static final boolean useWindow = false;
    static final boolean local = true;

    public static void main(String args[]) {
        ElizaMain eliza = new ElizaMain();
        String script = scriptPathname;
        String test = testPathname;
        if (! local) script = scriptURL;
        if (args.length > 0) script = args[0];
        if (args.length > 1) test = args[1];
        int res = eliza.readScript(local, script);
        if (res != 0) System.exit(res);
        if (useWindow) {
            Frame f = new Frame("Eliza");
            f.setSize(100, 100);
            f.setVisible(true);
            Panel p = new Panel();
            f.add(p);
            p.setVisible(true);
            res = eliza.runProgram(test, p);
        } else {
            res = eliza.runProgram(test, null);
            System.exit(res);
        }
    }

}
