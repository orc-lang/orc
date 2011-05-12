package net.chayden.eliza;

import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 *  Eliza main class.
 *  Stores the processed script.
 *  Does the input transformations.
 */
public class ElizaMain {
	private Eliza eliza = new Eliza();
    private final boolean printData = false;

    TextArea textarea;
    TextField textfield;

    public void response(String str) {
        textarea.append(str);
        textarea.append("\n");
    }

    int readScript(boolean local, String script) {
        Reader in;
        try {
            if (local) {
                in = new FileReader(script);
            } else {
                try {
                    URL url = new URL(script);
                    URLConnection connection = url.openConnection();
                    in = new InputStreamReader(connection.getInputStream());
                } catch (MalformedURLException e) {
                    System.out.println("The URL is malformed: " + script);
                    return 1;
                } catch (IOException e) {
                    System.out.println("Could not read script file.");
                    return 1;
                }
            }
            eliza.readScript(in);
        } catch (IOException e) {
            System.out.println("There was a problem reading the script file.");
            System.out.println("Tried " + script);
            return 1;
        }
        if (printData) eliza.print();
        return 0;
    }

    int runProgram(String test, Panel w) {
        BufferedReader in;

        if (w != null) {

            w.setLayout(new BorderLayout(15, 15));

            textarea = new TextArea(10, 40);
            textarea.setEditable(false);

            w.add("Center", textarea);

            textfield = new TextField(15);
            w.add("South", textfield);

            w.setSize(600, 300);
            w.setVisible(true);

            String hello = "Hello.";
            response(">> " + hello);
            try {
				response(eliza.processInput(hello));
			} catch (IOException e) {
				response("ERROR: " + e.getMessage());
			}
            textfield.requestFocus();

        } else {
            try {
                in = new BufferedReader(new FileReader(test));
                String s;
                s = "Hello.";
                while (true) {
                    System.out.println(">> " + s);
                    String reply = eliza.processInput(s);
                    System.out.println(reply);
                    if (eliza.finished()) break;
                    s = in.readLine();
                    if (s == null) break;
                }
            } catch (IOException e) {
                System.out.println("Problem reading test file.");
                return 1;
            }
        }

        return 0;

    }

    public boolean handleEvent(Event event) {
        switch (event.id) {
            case Event.ACTION_EVENT:
                if (event.target == textfield) {
                    String input = (String)event.arg;
                    textfield.setText("");
                    response(">> " + input);
                    try {
                        String reply = eliza.processInput(input);
                        response(reply);
        			} catch (IOException e) {
        				response("ERROR: " + e.getMessage());
        			}

                    return true;
                }
        }
        return false;
    }
}
