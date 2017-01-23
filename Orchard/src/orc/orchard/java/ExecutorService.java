//
// ExecutorService.java -- Java class ExecutorService
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.java;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import orc.orchard.AbstractExecutorService;
import orc.orchard.errors.InvalidJobException;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.InvalidPromptException;
import orc.orchard.errors.QuotaException;
import orc.orchard.events.BrowseEvent;
import orc.orchard.events.JobEvent;
import orc.orchard.events.PrintlnEvent;
import orc.orchard.events.PromptEvent;
import orc.orchard.events.PublicationEvent;
import orc.orchard.events.TokenErrorEvent;
import orc.orchard.events.Visitor;

public class ExecutorService extends AbstractExecutorService {

    protected ExecutorService() {
        super();
    }

    public static void main(final String[] args) throws FileNotFoundException, IOException, QuotaException, InvalidProgramException, InvalidOilException, InvalidJobStateException, InvalidJobException, InterruptedException {
        String program;
        if (args.length > 0) {
            program = getStreamContent(new FileInputStream(args[0]));
        } else {
            program = getStreamContent(System.in);
        }
        final ExecutorService executor = new ExecutorService();
        logger.setLevel(Level.OFF);
        final String job = executor.compileAndSubmit("", program);
        executor.startJob("", job);
        List<JobEvent> events;
        do {
            events = executor.jobEvents("", job);
            executor.purgeJobEvents("", job);
            for (final JobEvent event : events) {
                event.accept(new Visitor<Void>() {
                    @Override
                    public Void visit(final PrintlnEvent event) {
                        System.out.println(event.line);
                        return null;
                    }

                    @Override
                    public Void visit(final PromptEvent event) {
                        final String response = JOptionPane.showInputDialog(event.message);
                        try {
                            executor.respondToPrompt("", job, event.promptID, response);
                        } catch (final RemoteException e) {
                            System.err.println("ERROR: " + e.getMessage());
                        } catch (final InvalidPromptException e) {
                            System.err.println("ERROR: " + e.getMessage());
                        } catch (final InvalidJobException e) {
                            System.err.println("ERROR: " + e.getMessage());
                        }
                        return null;
                    }

                    @Override
                    public Void visit(final PublicationEvent event) {
                        System.out.println(event.value.toString());
                        return null;
                    }

                    @Override
                    public Void visit(final BrowseEvent event) {
                        System.err.println("REDIRECT: " + event.url);
                        return null;
                    }

                    @Override
                    public Void visit(final TokenErrorEvent event) {
                        System.err.println("ERROR: " + event.message);
                        return null;
                    }
                });
            }
            Thread.yield();
        } while (!events.isEmpty());
    }

    private static String getStreamContent(final InputStream stream) throws IOException {
        // read program from stdin
        final InputStreamReader reader = new InputStreamReader(stream);
        final StringBuilder sb = new StringBuilder();
        int blen;
        final char[] buffer = new char[1024];
        while ((blen = reader.read(buffer)) > 0) {
            sb.append(buffer, 0, blen);
        }
        return sb.toString();
    }
}
