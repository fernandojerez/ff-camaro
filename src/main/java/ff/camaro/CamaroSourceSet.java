package ff.camaro;

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;

public class CamaroSourceSet extends DefaultSourceSet {

	private final SourceDirectorySet dir;
	private final ExtensionContainer extensions;

	public CamaroSourceSet(final String displayName, final ObjectFactory factory,
			final ExtensionContainer extensionContainer) {
		super(displayName, factory);
		dir = factory.sourceDirectorySet(displayName, displayName + " FF source");
		extensions = extensionContainer;
		includes("**/*.ff");
	}

	public void excludes(final String... filters) {
		dir.getFilter().exclude(filters);
	}

	@Override
	public ExtensionContainer getExtensions() {
		return extensions;
	}

	public SourceDirectorySet getSrcDir() {
		return dir;
	}

	public void includes(final String... filters) {
		dir.getFilter().include(filters);
	}
}
