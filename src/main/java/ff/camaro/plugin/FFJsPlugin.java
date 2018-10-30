package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.internal.file.SourceDirectorySetFactory;

public class FFJsPlugin extends CamaroPlugin {

	@Inject
	public FFJsPlugin(final SourceDirectorySetFactory sourceDirectorySetFactory) {
		super(sourceDirectorySetFactory);
	}

	@Override
	public String getConfiguration() {
		return "js";
	}
}
