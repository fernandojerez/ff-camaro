package ff.camaro.plugin.tasks.builder;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import ff.camaro.ConfigLoader;
import ff.camaro.plugin.tasks.BaseTask;
import ff.camaro.plugin.tasks.FFCompiler;

public class FFCompilerBuilder extends TaskBuilder {

	@Override
	public void define(final Project project, final String taskName) {
		project.getTasks().create(taskName, FFCompiler.class, new Action<FFCompiler>() {

			@Override
			public void execute(final FFCompiler compiler) {
				final File buildDir = project.getBuildDir();
				BaseTask.base_setup(compiler, getDefinition(), getConfiguration());
				compiler.setAnalizedOutput(new File(buildDir, getString("analizedOutput")));
				compiler.setCompilerClass(getString("compilerClass"));
				if (!getString("definitionOutput").equals("none")) {
					compiler.setDefinitionOutput(new File(buildDir, getString("definitionOutput")));
				}
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
				}
				if ("true".equals(getString("test"))) {
					compiler.setModuleOutputDir(new File(buildDir, getString("moduleOutputDir")));
				}
			}

		});
	}

}
