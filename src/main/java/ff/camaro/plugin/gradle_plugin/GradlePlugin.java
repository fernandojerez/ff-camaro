package ff.camaro.plugin.gradle_plugin;

import org.gradle.api.Project;

import ff.camaro.Configurator;

public interface GradlePlugin {

	public void apply(Project target, Configurator configurator);

}
