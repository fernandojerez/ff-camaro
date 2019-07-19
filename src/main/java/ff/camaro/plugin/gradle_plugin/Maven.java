package ff.camaro.plugin.gradle_plugin;

import org.gradle.api.Project;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

import ff.camaro.Configurator;

public class Maven implements GradlePlugin {

	@Override
	public void apply(final Project prj, final Configurator configurator) {
		prj.getPluginManager().apply(MavenPublishPlugin.class);
	}

}
