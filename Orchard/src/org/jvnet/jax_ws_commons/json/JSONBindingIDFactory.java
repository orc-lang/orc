package org.jvnet.jax_ws_commons.json;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.BindingIDFactory;

import javax.xml.ws.WebServiceException;

/**
 * @author Jitendra Kotamraju
 */
public class JSONBindingIDFactory extends BindingIDFactory {

    public @Nullable BindingID parse(@NotNull String lexical) throws WebServiceException {
    	if (lexical.equals(JSONBindingID.JSON_BINDING)) {
    		return new JSONBindingID();
    	} else {
    		return null;
    	}
    }
    
}
