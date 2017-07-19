package orc.run.porce;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.instrumentation.ProvidedTags;

import orc.OrcRuntime;
import orc.run.extensions.SimpleWorkStealingScheduler;

@TruffleLanguage.Registration(name = "Orc", version = "2.99.0.1", mimeType = PorcELanguage.MIME_TYPE)
@ProvidedTags({})
public class PorcELanguage extends TruffleLanguage<PorcEContext> {
    public static final String MIME_TYPE = "application/x-orc";
    
	private OrcRuntime runtime = null;    

	@Override
	protected PorcEContext createContext(com.oracle.truffle.api.TruffleLanguage.Env env) {
		return new PorcEContext(runtime );
	}
	
	@Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
		throw new Error("Not Implemented");
    }

	@Override
	protected Object findExportedSymbol(PorcEContext context, String globalName, boolean onlyExplicit) {
		return null;
	}

	@Override
	protected Object getLanguageGlobal(PorcEContext context) {
		return null;
	}

	@Override
	protected boolean isObjectOfLanguage(Object object) {
		return true;
	}
}