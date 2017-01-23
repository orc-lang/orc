//
// CompilerService.java -- Java class CompilerService
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.soap;

import java.net.URI;
import java.net.URISyntaxException;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import orc.orchard.AbstractCompilerService;
import orc.orchard.errors.InvalidProgramException;

/**
 * Web Service implementation bean for the Orc executor service. HACK: We must
 * explicitly declare every published web method in this class, we can't simply
 * inherit them, for the following reasons:
 * <ul>
 * <li>JAX-WS ignores (does not publish) inherited methods. You can work around
 * this by using an endpointInterface which includes all the methods you want to
 * publish, but...
 * <li>JAX-WS JSON bindings don't work with endpointInterface at all.
 * </ul>
 *
 * @author quark
 */
@WebService
//@BindingType(JSONBindingID.JSON_BINDING)
public class CompilerService extends AbstractCompilerService {
    /**
     * Construct a service to run in an existing servlet context.
     */
    public CompilerService() {
        super();
    }

    CompilerService(final URI baseURI) {
        this();
        logger.fine("Orchard compiler Web service: Publishing endpoint at '" + baseURI + "'");
        Endpoint.publish(baseURI.toString(), this);
        logger.config("Orchard compiler Web service: Published endpoint at  '" + baseURI + "'");
    }

    @SuppressWarnings("unused")
    public static void main(final String[] args) {
        URI baseURI;
        baseURI = URI.create("http://localhost:8280/orchard/compiler");
        if (args.length > 0) {
            try {
                baseURI = new URI(args[0]);
            } catch (final URISyntaxException e) {
                System.err.println("Invalid URI '" + args[0] + "'");
                return;
            }
        }
        new CompilerService(baseURI);
    }

    /** Do-nothing override */
    @Override
    public String compile(@WebParam(name = "devKey") final String devKey, @WebParam(name = "program") final String program) throws InvalidProgramException {
        return super.compile(devKey, program);
    }
}
