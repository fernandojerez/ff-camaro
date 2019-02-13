package ff.camaro;

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;

public class FFSourceSet extends DefaultSourceSet {

	private final SourceDirectorySet ff;
	private final ExtensionContainer extensions;

	public FFSourceSet(final String displayName, final ObjectFactory factory,
			final ExtensionContainer extensionContainer) {
		super(displayName, factory);
		ff = factory.sourceDirectorySet(displayName, displayName + " FF source");
		ff.getFilter().include("**/*.ff");
		extensions = extensionContainer;
	}

	@Override
	public ExtensionContainer getExtensions() {
		return extensions;
	}

	public SourceDirectorySet getFf() {
		return ff;
	}
}
