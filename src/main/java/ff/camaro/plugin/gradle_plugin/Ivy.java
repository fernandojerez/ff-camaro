package ff.camaro.plugin.gradle_plugin;

import org.gradle.api.Project;
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin;

import ff.camaro.Configurator;

public class Ivy extends GradlePlugin {

	@Override
	public void apply(final Project target, final Configurator configurator) {
		target.getPluginManager().apply(IvyPublishPlugin.class);
	}

}
