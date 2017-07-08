package orc.run.porce;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.instrumentation.ProvidedTags;

import orc.run.extensions.SimpleWorkStealingScheduler;
import scala.NotImplementedError;

@TruffleLanguage.Registration(name = "Orc", version = "2.99.0.1", mimeType = PorcELanguage.MIME_TYPE)
@ProvidedTags({})
public class PorcELanguage extends TruffleLanguage<PorcEContext> {
    public static final String MIME_TYPE = "application/x-orc";
    
    private SimpleWorkStealingScheduler scheduler = null;

	@Override
	protected PorcEContext createContext(com.oracle.truffle.api.TruffleLanguage.Env env) {
		return new PorcEContext(scheduler);
	}
	
	@Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
		throw new NotImplementedError();
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