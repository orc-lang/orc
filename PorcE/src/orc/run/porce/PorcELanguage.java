package orc.run.porce;

import orc.run.porce.instruments.ProfiledPorcENodeTag;
import orc.run.porce.instruments.ProfiledPorcNodeTag;
import orc.util.ExpressionTag;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.instrumentation.ProvidedTags;

@TruffleLanguage.Registration(name = "Orc", version = "2.99.0.1", mimeType = { PorcELanguage.MIME_TYPE })
@ProvidedTags({ ExpressionTag.class, ProfiledPorcNodeTag.class, ProfiledPorcENodeTag.class })
public class PorcELanguage extends PorcELanguageBase {
	public static final String MIME_TYPE = "application/x-orc";

	// The actual implementation is provided by PorcELanguageBase.
	// This class is only needed to trigger the Java annotation processor.
}
