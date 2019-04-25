package ff.camaro.plugin.gradle_plugin;

import java.io.File;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension;
import org.gradle.testing.jacoco.tasks.JacocoReport;
import org.gradle.testing.jacoco.tasks.JacocoReportsContainer;

import ff.camaro.Configurator;

public class Java implements GradlePlugin {

	@Override
	public void apply(final Project project, final Configurator configurator) {
		project.getPluginManager().apply(JavaPlugin.class);

		final JavaPluginConvention javaSettings = project.getConvention().getPlugin(JavaPluginConvention.class);
		javaSettings.setSourceCompatibility(JavaVersion.VERSION_12);
		javaSettings.setTargetCompatibility(JavaVersion.VERSION_12);

		final JavaCompile compile = (JavaCompile) project.getTasks().getByName("compileJava");
		compile.getOptions().setEncoding("UTF-8");
		compile.getOptions().getCompilerArgs().add("-Xlint:deprecation");

		final Jar jar = (Jar) project.getTasks().getByName("jar");
		jar.getManifest().attributes(configurator.getManifestAttributes(""));

		final Test test = (Test) project.getTasks().getByName("test");
		test.useJUnitPlatform();

		setup_jacoco(project);
	}

	private void setup_jacoco(final Project project) {
		project.getPluginManager().apply(JacocoPlugin.class);

		final JacocoPluginExtension extension = project.getExtensions().getByType(JacocoPluginExtension.class);
		extension.setToolVersion("0.8.3");

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
