package ff.camaro.plugin.gradle_plugin;

import org.gradle.api.Project;

import ff.camaro.Configurator;
import groovy.util.Eval;

public abstract class GradlePlugin {

	public abstract void apply(Project target, Configurator configurator);

	public Object evaluate(final String code) {
		return Eval.me(code);
	}

	public void ext(final Project prj, final String name, final Object value) {
		prj.getExtensions().getExtraProperties().set(name, value);
	}

	public void ext_closure(final Project prj, final String name, final String code) {
		prj.getExtensions().getExtraProperties().set(name, evaluate(code));
	}
}
