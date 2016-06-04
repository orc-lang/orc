package org.jvnet.jax_ws_commons.json;

import com.sun.istack.internal.NotNull;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.pipe.Codec;

/**
 * @author Jitendra Kotamraju
 */
public class JSONBindingID extends BindingID {

    public static final String JSON_BINDING = "https://jax-ws-commons.dev.java.net/json/";

    public SOAPVersion getSOAPVersion() {
        return SOAPVersion.SOAP_11;
    }

    public @NotNull Codec createEncoder(@NotNull WSBinding binding) {
        return new JSONCodec(binding);
    }

    public String toString() {
        return JSON_BINDING;
    }

    @Override
    public boolean canGenerateWSDL() {
        return true;
    }

}
