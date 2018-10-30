package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.internal.file.SourceDirectorySetFactory;

public class FFMustangPlugin extends CamaroPlugin {

	@Inject
	public FFMustangPlugin(final SourceDirectorySetFactory sourceDirectorySetFactory) {
		super(sourceDirectorySetFactory);
	}

	@Override
	public String getConfiguration() {
		return "mustang";
	}

}
