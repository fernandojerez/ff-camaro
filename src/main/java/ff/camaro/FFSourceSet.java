package ff.camaro;

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.internal.tasks.DefaultSourceSet;

public class FFSourceSet extends DefaultSourceSet {

	private final SourceDirectorySet ff;

	public FFSourceSet(final String displayName, final SourceDirectorySetFactory factory) {
		super(displayName, factory);
		ff = factory.create(displayName + " FF source");
		ff.getFilter().include("**/*.ff");
	}

	public SourceDirectorySet getFf() {
		return ff;
	}
}
