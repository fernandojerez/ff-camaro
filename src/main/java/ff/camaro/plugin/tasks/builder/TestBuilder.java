package ff.camaro.plugin.tasks.builder;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension;

import ff.camaro.ConfigLoader;
import ff.camaro.plugin.tasks.BaseTask;

public class TestBuilder extends TaskBuilder {

	public boolean create() {
		return true;
	}

	@Override
	public void define(final Project project, final String taskName) {
		final Action<Test> action = new Action<>() {

			@Override
			public void execute(final Test test) {
				BaseTask.base_setup(test, getDefinition(), getConfiguration());
				String lang = getString("lang");
				if (lang == null) {
					lang = "java";
				}
				test.useJUnitPlatform();
				test.getSystemProperties().put("ff.test.lang", lang);

				final String flang = lang;
				project.afterEvaluate(new Action<Project>() {

					@Override
					public void execute(final Project arg0) {
						final Set<File> files = new HashSet<>();
						files.addAll(project.getConfigurations().getByName(flang + "_test").resolve());
						files.add(project.file(ConfigLoader.output_test_path("ff_" + flang)));
						files.add(project.file(ConfigLoader.output_test_path("ff")));

						test.setClasspath(project.files(files.toArray()));
					}
				});

				final Map<String, String> suites = new HashMap<>();
				suites.put("java", "FFJavaTestSuite");
				suites.put("js", "FFJsTestSuite");

				final Set<String> excludes = new HashSet<>();
				for (final Map.Entry<String, String> entry : suites.entrySet()) {
					if (entry.getKey().equals(lang)) {
						continue;
					}
					excludes.add("ff/test/" + entry.getValue() + ".class");
					excludes.add("ff/test/" + entry.getValue() + ".java");
				}

				test.setExcludes(excludes);
				test.setTestClassesDirs(project.files(project.file(ConfigLoader.output_test_path("ff"))));
				test.getTestLogging().setShowStandardStreams(true);
				test.getTestLogging().setShowStackTraces(true);

				final JacocoTaskExtension jacoco = test.getExtensions().getByType(JacocoTaskExtension.class);
				jacoco.setDestinationFile(new File(project.getBuildDir(), "jacoco/test.exec"));
				jacoco.setEnabled(lang.equals("java"));
			}

		};
		project.getTasks().create(taskName, Test.class, action);
	}

}
