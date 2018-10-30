package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.internal.file.SourceDirectorySetFactory;

/**
 * @author fernandojerez
 */
public class FFJavaPlugin extends CamaroPlugin {

	@Inject
	public FFJavaPlugin(final SourceDirectorySetFactory sourceDirectorySetFactory) {
		super(sourceDirectorySetFactory);
	}

	@Override
	public String getConfiguration() {
		return "java";
	}

}
