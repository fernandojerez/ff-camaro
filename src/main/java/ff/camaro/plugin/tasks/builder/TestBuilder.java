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
import org.gradle.testing.jacoco.tasks.JacocoReport;

import ff.camaro.ConfigLoader;
import ff.camaro.plugin.tasks.FFCleanTest;

public class TestBuilder extends TaskBuilder<Test> {

	@Override
	public void configure(final Project project, final String taskName, final Test test) {
		String lang = config.getString("lang");
		if (lang == null) {
			lang = "java";
		}
		String browser = config.getString("browser");
		if (browser == null) {
			browser = "";
		}
		test.useJUnitPlatform();
		test.getSystemProperties().put("ff.test.lang", lang);
		if (!"java".equals(lang)) {
			test.setFailFast(true);
		}
		final String flang = lang;
		project.afterEvaluate(new Action<Project>() {

			@Override
			public void execute(final Project arg0) {
				final Set<File> files = new HashSet<>();
				files.addAll(project.getConfigurations().getByName(flang + "_test").resolve());
				files.add(new File(project.getBuildDir(), ConfigLoader.output_main_path(project, "ff_" + flang)));
				files.add(new File(project.getBuildDir(),
						ConfigLoader.output_main_path(project, "interfaces\\" + flang)));
				files.add(new File(project.getBuildDir(), ConfigLoader.output_main_path(project, "macros")));
				files.add(new File(project.getBuildDir(), ConfigLoader.output_test_path(project, "ff_" + flang)));
				files.add(new File(project.getBuildDir(), ConfigLoader.output_test_path(project, "ff")));

				test.setClasspath(project.files(files.toArray()));
			}
		});

		final Map<String, String> suites = new HashMap<>();
		suites.put("chrome", "FFChromeTestSuite");
		suites.put("firefox", "FFFirefoxTestSuite");
		suites.put("edge", "FFEdgeTestSuite");
		suites.put("java", "FFJavaTestSuite");
		suites.put("dart", "FFDartTestSuite");
		suites.put("python", "FFPythonTestSuite");

		final Set<String> excludes = new HashSet<>();
		for (final Map.Entry<String, String> entry : suites.entrySet()) {
			if (entry.getKey().equals(lang)) {
				continue;
			}
			if (entry.getKey().equals(browser)) {
				continue;
			}
			excludes.add("ff/test/" + entry.getValue() + ".class");
			excludes.add("ff/test/" + entry.getValue() + ".java");
		}

		test.setExcludes(excludes);
		test.setTestClassesDirs(
				project.files(new File(project.getBuildDir(), ConfigLoader.output_test_path(project, "ff"))));
		test.getTestLogging().setShowStandardStreams(true);
		test.getTestLogging().setShowStackTraces(true);

		final JacocoTaskExtension jacoco = test.getExtensions().getByType(JacocoTaskExtension.class);
		jacoco.setDestinationFile(new File(project.getBuildDir(), "jacoco/java/test.exec"));
		jacoco.setEnabled(lang.equals("java"));
		jacoco.getExcludes().add("**launcher**");

		project.getTasks().maybeCreate("ff_test_clean", FFCleanTest.class);
		if ("java".equals(lang)) {
			project.getTasks().create(taskName + "_report", JacocoReport.class, new Action<JacocoReport>() {

				@Override
				public void execute(final JacocoReport report) {
					report.getReports().getXml().setEnabled(false);
					report.getReports().getCsv().setEnabled(false);
					report.getReports().getHtml().setEnabled(true);
					report.getReports().getHtml()
							.setDestination(new File(project.getBuildDir(), "reports/jacoco/html"));
					report.executionData(new File(project.getBuildDir(), "jacoco/java/test.exec"));
					report.additionalSourceDirs(new File("src/main/ff"), new File("src/main/interfaces/java"));
					report.additionalClassDirs(new File(project.getBuildDir(), "classes/ff_java/main"),
							new File(project.getBuildDir(), "classes/interfaces/java/main"));
					report.setEnabled(true);
				}

			});
		}
	}

	@Override
	public Class<Test> getTaskClass() {
		return Test.class;
	}

}
