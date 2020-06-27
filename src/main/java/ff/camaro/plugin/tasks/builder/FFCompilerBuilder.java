package ff.camaro.plugin.tasks.builder;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import ff.camaro.ConfigLoader;
import ff.camaro.plugin.CamaroPlugin;
import ff.camaro.plugin.tasks.BaseTask;
import ff.camaro.plugin.tasks.FFCompiler;

public class FFCompilerBuilder extends TaskBuilder {

	@Override
	public void define(final Project project, final String taskName) {
		final String language = getString("language");
		CamaroPlugin.metadata(project).getLanguages().add(language);
		project.getTasks().create(taskName, FFCompiler.class, new Action<FFCompiler>() {

			@Override
			public void execute(final FFCompiler compiler) {
				final File buildDir = project.getBuildDir();
				BaseTask.base_setup(compiler, getDefinition(), getConfiguration());
				compiler.setAnalizedOutput(new File(buildDir, getString("analizedOutput")));
				if (!("java".equals(language) || "macros".equals(language))) {
					compiler.setDefinitionOutput(new File(buildDir, getString("outputDir")));
				}
				compiler.setLanguage(language);
				compiler.setCompilationType(getString("compilation_type"));
				compiler.setFolder(getString("folder"));
				compiler.setMacroOutput(new File(buildDir, getString("macroOutput")));
				compiler.setOutput(new File(buildDir, getString("outputDir")));
				compiler.setSourceSet(getString("sourceSet"));
				compiler.setConfiguration(getList("configuration").toArray(new String[0]));
				final String interfaces = getString("interfaces");
				if (interfaces != null) {
					compiler.setInterfaces(new File(buildDir,
							ConfigLoader.output_path(project, interfaces, SourceSet.MAIN_SOURCE_SET_NAME)));
				}
				if (!"macros".equals(getString("source"))) {
					compiler.setMacros(new File(buildDir,
							ConfigLoader.output_path(project, "macros", SourceSet.MAIN_SOURCE_SET_NAME)));
					compiler.setJavaOutput(new File(buildDir,
							ConfigLoader.output_path(project, "ff_java", SourceSet.MAIN_SOURCE_SET_NAME)));
					compiler.setMacro(false);
				} else {
					compiler.setMacros(new File(buildDir,
							ConfigLoader.output_path(project, "ff_java", SourceSet.MAIN_SOURCE_SET_NAME)));
					compiler.setMacro(true);
				}
				if ("true".equals(getString("test"))) {
					compiler.setModuleOutputDir(new File(buildDir, getString("moduleOutputDir")));
				} else {
					if (!"macros".equals(getString("source"))) {
						if ("APPLICATION".equals(getString("compilation_type"))) {
							compiler.setDepsOutput(new File(buildDir, getString("depsOutput")));
						}
					}
				}
			}

		});
	}

}
