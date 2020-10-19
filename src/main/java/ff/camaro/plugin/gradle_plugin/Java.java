package ff.camaro.plugin.gradle_plugin;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension;
import org.gradle.testing.jacoco.tasks.JacocoReport;
import org.gradle.testing.jacoco.tasks.JacocoReportsContainer;

import ff.camaro.Configurator;

public class Java extends GradlePlugin {

	private static void configureCompiler(final JavaCompile compile) {
		compile.getOptions().setEncoding("UTF-8");
		compile.getOptions().getCompilerArgs().add("-Xlint:deprecation");
	}

	public static void createJavaCompileTask(final Project project, final String name, final File source,
			final File output, final FileCollection classpath) {
		final TaskContainer tasks = project.getTasks();
		final String compilerTaskName = name + "_compiler";
		tasks.create(compilerTaskName, JavaCompile.class, new Action<JavaCompile>() {

			@Override
			public void execute(final JavaCompile compiler) {
				Java.configureCompiler(compiler);
				compiler.setClasspath(classpath);
				compiler.setSource(project.fileTree(source, new Action<ConfigurableFileTree>() {

					@Override
					public void execute(final ConfigurableFileTree tree) {
						tree.include("**/*.java");
					}

				}));
				compiler.setDestinationDir(output);
			}

		});

		tasks.create(name, Copy.class, new Action<Copy>() {

			@Override
			public void execute(final Copy copy) {
				copy.dependsOn(compilerTaskName);
				copy.from(source);
				copy.into(output);
				copy.exclude("**/*.java");
			}

		});
	}

	@Override
	public void apply(final Project project, final Configurator configurator) {
		project.getPluginManager().apply(JavaPlugin.class);

		final JavaPluginConvention javaSettings = project.getConvention().getPlugin(JavaPluginConvention.class);
		javaSettings.setSourceCompatibility(JavaVersion.VERSION_14);
		javaSettings.setTargetCompatibility(JavaVersion.VERSION_14);

		final JavaCompile compile = (JavaCompile) project.getTasks().getByName("compileJava");
		Java.configureCompiler(compile);

		final Jar jar = (Jar) project.getTasks().getByName("jar");
		jar.getManifest().attributes(configurator.getManifestAttributes(""));

		final Test test = (Test) project.getTasks().getByName("test");
		test.useJUnitPlatform();
		test.setIgnoreFailures(true);
		test.setFailFast(false);

		setup_jacoco(project);
	}

	private void setup_jacoco(final Project project) {
		project.getPluginManager().apply(JacocoPlugin.class);

		final JacocoPluginExtension extension = project.getExtensions().getByType(JacocoPluginExtension.class);
		extension.setToolVersion("0.8.6");

		final JacocoReport report = (JacocoReport) project.getTasks().getByName("jacocoTestReport");
		final JacocoReportsContainer reports = report.getReports();
		reports.getXml().setEnabled(false);
		reports.getCsv().setEnabled(false);
		reports.getHtml().setDestination(new File(project.getBuildDir(), "jacocoHtml"));

		final Test test = (Test) project.getTasks().getByName("test");
		final JacocoTaskExtension jacoco = test.getExtensions().getByType(JacocoTaskExtension.class);
		jacoco.setDestinationFile(new File(project.getBuildDir(), "jacoco/test.exec"));
		jacoco.setClassDumpDir(new File(project.getBuildDir(), "build/classes/test"));
	}

}
