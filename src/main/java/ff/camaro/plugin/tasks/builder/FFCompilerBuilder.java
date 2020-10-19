package ff.camaro.plugin.tasks.builder;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import ff.camaro.ConfigLoader;
import ff.camaro.plugin.CamaroPlugin;
import ff.camaro.plugin.tasks.FFCompiler;

public class FFCompilerBuilder extends TaskBuilder<FFCompiler> {

	@Override
	public void configure(final Project project, final String taskName, final FFCompiler compiler) {
		final String language = config.getString("language", null);
		final boolean isMacros = "macros".equals(language);
		final String finalLanguage = isMacros ? "java" : language;
		final boolean testMode = "true".equals(config.getString("test"));
		final String compilationType = config.getString("compilation_type", "LIBRARY");

		final List<String> configurations = config.getList("configuration");
		configurations.add(language);
		if (testMode) {
			configurations.add(language + "_test");
		}
		if (project.getConfigurations().findByName(finalLanguage + "_libs") != null) {
			configurations.add(finalLanguage + "_libs");
		}
		final String interfaces = config.getString("interfaces", "interfaces/" + finalLanguage);

		// add to camaro.build.json metadata file
		CamaroPlugin.metadata(project).getLanguages().add(language);
		final File buildDir = project.getBuildDir();

		compiler.setLanguage(language);
		compiler.setMacroMode(isMacros);
		compiler.setTestMode(testMode);
		compiler.setCompilationType(compilationType);
		compiler.setConfiguration(new HashSet<>(configurations).toArray(new String[0]));
		compiler.setInterfaces(
				new File(buildDir, ConfigLoader.output_path(project, interfaces, SourceSet.MAIN_SOURCE_SET_NAME))
						.toPath());
	}

	@Override
	public Class<FFCompiler> getTaskClass() {
		return FFCompiler.class;
	}

}
