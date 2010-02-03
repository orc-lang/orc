package org.jvnet.jax_ws_commons.json;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import com.sun.xml.internal.ws.api.BindingID;
import com.sun.xml.internal.ws.api.BindingIDFactory;

import javax.xml.ws.WebServiceException;

/**
 * @author Jitendra Kotamraju
 */
public class JSONBindingIDFactory extends BindingIDFactory {

    public @Nullable BindingID parse(@NotNull String lexical) throws WebServiceException {
        return new JSONBindingID();
    }
    
}
