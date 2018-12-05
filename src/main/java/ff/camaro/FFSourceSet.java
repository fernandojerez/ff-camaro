package ff.camaro;

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.reflect.Instantiator;

public class FFSourceSet extends DefaultSourceSet {

	private final SourceDirectorySet ff;

	public FFSourceSet(final String displayName, final ObjectFactory factory, final Instantiator instantiator) {
		super(displayName, factory, instantiator);
		ff = factory.sourceDirectorySet(displayName, displayName + " FF source");
		ff.getFilter().include("**/*.ff");
	}

	public SourceDirectorySet getFf() {
		return ff;
	}
}
