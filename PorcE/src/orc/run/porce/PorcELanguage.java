
package orc.run.porce;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import orc.OrcRuntime;

@TruffleLanguage.Registration(name = "Orc", version = "2.99.0.1", mimeType = PorcELanguage.MIME_TYPE)
@ProvidedTags({})
public class PorcELanguage extends TruffleLanguage<PorcEContext> {
    public static final String MIME_TYPE = "application/x-orc";

    private final OrcRuntime runtime = null;

    @Override
    protected PorcEContext createContext(final com.oracle.truffle.api.TruffleLanguage.Env env) {
        return new PorcEContext(runtime);
    }

    @Override
    protected CallTarget parse(final ParsingRequest request) throws Exception {
        throw new Error("Not Implemented");
    }

    @Override
    protected Object findExportedSymbol(final PorcEContext context, final String globalName, final boolean onlyExplicit) {
        return null;
    }

    @Override
    protected Object getLanguageGlobal(final PorcEContext context) {
        return null;
    }

    @Override
    protected boolean isObjectOfLanguage(final Object object) {
        return true;
    }
}
