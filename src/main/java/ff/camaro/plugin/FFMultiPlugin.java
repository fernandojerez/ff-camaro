package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.internal.file.SourceDirectorySetFactory;

public class FFMultiPlugin extends CamaroPlugin {

	@Inject
	public FFMultiPlugin(final SourceDirectorySetFactory sourceDirectorySetFactory) {
		super(sourceDirectorySetFactory);
	}

	@Override
	public String getConfiguration() {
		return "multi";
	}

}
