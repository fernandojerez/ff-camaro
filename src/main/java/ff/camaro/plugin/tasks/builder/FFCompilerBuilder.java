package ff.camaro.plugin.tasks.builder;

import org.gradle.api.Action;
import org.gradle.api.Project;

import ff.camaro.plugin.tasks.BaseTask;
import ff.camaro.plugin.tasks.FFCompiler;

public class FFCompilerBuilder extends TaskBuilder {

	@Override
	public void define(final Project project, final String taskName) {
		project.getTasks().create(taskName, FFCompiler.class, new Action<FFCompiler>() {

			@Override
			public void execute(final FFCompiler compiler) {
				BaseTask.base_setup(compiler, getDefinition(), getConfiguration());
				compiler.setAnalizedOutput(project.file(getString("analizedOutput")));
				compiler.setCompilerClass(getString("compilerClass"));
				if (!getString("definitionOutput").equals("none")) {
					compiler.setDefinitionOutput(project.file(getString("definitionOutput")));
				}
				compiler.setFolder(getString("folder"));
				compiler.setMacroOutput(project.file(getString("macroOutput")));
				compiler.setOutput(project.file(getString("outputDir")));
				compiler.setSourceSet(getString("sourceSet"));
				compiler.setConfiguration(getList("configuration").toArray(new String[0]));
			}

		});
	}

}
