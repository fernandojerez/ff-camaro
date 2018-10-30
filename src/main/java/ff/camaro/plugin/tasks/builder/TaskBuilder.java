package ff.camaro.plugin.tasks.builder;

import java.util.List;
import java.util.Map;

import org.gradle.api.Project;

import ff.camaro.Configurator;

public abstract class TaskBuilder {

	private Map<String, Object> definition;
	private Configurator config;

	protected <T> T cast(final Object value) {
		return config.cast(value);
	}

	public abstract void define(Project project, String taskName);

	public Configurator getConfiguration() {
		return config;
	}

	public Map<String, Object> getDefinition() {
		return definition;
	}

	protected <T> List<T> getList(final Map<String, Object> config, final String key) {
		return this.config.getList(config, key);
	}

	protected <T> List<T> getList(final String key) {
		return config.getList(definition, key);
	}

	public Map<String, Object> getMap(final String key) {
		return config.getMap(definition, key);
	}

	public String getString(final String key) {
		return config.toString(definition.get(key));
	}

	public void setDefinition(final Map<String, Object> definition, final Configurator config) {
		this.definition = definition;
		this.config = config;
	}
}
