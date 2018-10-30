package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.internal.file.SourceDirectorySetFactory;

public class FFBasePlugin extends CamaroPlugin {

	@Inject
	public FFBasePlugin(final SourceDirectorySetFactory sourceDirectorySetFactory) {
		super(sourceDirectorySetFactory);
	}

	@Override
	public String getConfiguration() {
		return "base_camaro";
	}

}
