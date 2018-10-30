package ff.camaro.plugin.gradle_plugin;

import java.util.List;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.plugins.ide.api.XmlFileContentMerger;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.AccessRule;
import org.gradle.plugins.ide.eclipse.model.Classpath;
import org.gradle.plugins.ide.eclipse.model.ClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.SourceFolder;

import ff.camaro.ConfigLoader;
import ff.camaro.Configurator;

public class Eclipse implements GradlePlugin {

	protected void addEclipseSourceFolder(final Classpath cp, final String path, final boolean test) {
		cp.getEntries().add(new SourceFolder(ConfigLoader.src_path(path, SourceSet.MAIN_SOURCE_SET_NAME),
				ConfigLoader.src_path(path, SourceSet.MAIN_SOURCE_SET_NAME)));
		if (test) {
			cp.getEntries().add(new SourceFolder(ConfigLoader.src_path(path, SourceSet.TEST_SOURCE_SET_NAME),
					ConfigLoader.src_path(path, SourceSet.TEST_SOURCE_SET_NAME)));
		}
	}

	protected void addEclipseSourceWithOutputFolder(final Classpath cp, final String path, final boolean test) {
		cp.getEntries().add(new SourceFolder(ConfigLoader.src_path(path, SourceSet.MAIN_SOURCE_SET_NAME),
				ConfigLoader.output_path(path, SourceSet.MAIN_SOURCE_SET_NAME)));
		if (test) {
			cp.getEntries().add(new SourceFolder(ConfigLoader.src_path(path, SourceSet.TEST_SOURCE_SET_NAME),
					ConfigLoader.output_path(path, SourceSet.TEST_SOURCE_SET_NAME)));
		}
	}

	@Override
	public void apply(final Project project, final Configurator configurator) {
		project.getPluginManager().apply(EclipsePlugin.class);

		final EclipseModel model = project.getExtensions().getByType(EclipseModel.class);
		model.getJdt().setJavaRuntimeName("JavaSE-11");
		model.getJdt().setSourceCompatibility(JavaVersion.VERSION_11);
		model.getJdt().setTargetCompatibility(JavaVersion.VERSION_11);

		final EclipseClasspath classpath = model.getClasspath();
		classpath.file(new Action<XmlFileContentMerger>() {
			@Override
			public void execute(final XmlFileContentMerger merger) {
				merger.whenMerged(new Action<>() {
					@Override
					public void execute(final Object item) {
						final Classpath cp = (Classpath) item;
						final List<ClasspathEntry> entries = cp.getEntries();
						for (final ClasspathEntry entry : entries) {
							if (entry.getKind().equals("con")) {
								if (entry instanceof AbstractClasspathEntry) {
									final AbstractClasspathEntry aentry = (AbstractClasspathEntry) entry;
									if (aentry.getPath().contains("org.eclipse.jdt.launching.JRE_CONTAINER")) {
										aentry.getAccessRules()
												.add(new AccessRule("3", "jdk/nashorn/internal/runtime/**"));
										aentry.getAccessRules()
												.add(new AccessRule("3", "jdk/nashorn/api/scripting/**"));
										aentry.getAccessRules().add(new AccessRule("3", "com/sun/net/httpserver/**"));
									}
								}
							}
						}

						final Map<String, Object> map = configurator.getMap("eclipse");
						for (final Map.Entry<String, Object> entry : map.entrySet()) {
							@SuppressWarnings("unchecked")
							final Map<String, Object> f = (Map<String, Object>) entry.getValue();

							if (configurator.test(f.get("output"))) {
								addEclipseSourceWithOutputFolder(cp, entry.getKey(), configurator.test(f.get("test")));
							} else {
								addEclipseSourceFolder(cp, entry.getKey(), configurator.test(f.get("test")));
							}
						}

					}
				});
			}
		});
	}
}
