package ff.camaro.plugin.gradle_plugin;

import java.io.File;
import java.util.LinkedHashMap;
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
import org.gradle.plugins.ide.eclipse.model.EclipseProject;
import org.gradle.plugins.ide.eclipse.model.Link;
import org.gradle.plugins.ide.eclipse.model.SourceFolder;

import ff.camaro.ConfigLoader;
import ff.camaro.Configurator;
import groovy.util.Node;

public class Eclipse extends GradlePlugin {

	protected void addEclipseSourceFolder(final Classpath cp, final String path, final boolean test) {
		cp.getEntries().add(new SourceFolder(ConfigLoader.src_path(path, SourceSet.MAIN_SOURCE_SET_NAME),
				ConfigLoader.src_path(path, SourceSet.MAIN_SOURCE_SET_NAME)));
		if (test) {
			cp.getEntries().add(new SourceFolder(ConfigLoader.src_path(path, SourceSet.TEST_SOURCE_SET_NAME),
					ConfigLoader.src_path(path, SourceSet.TEST_SOURCE_SET_NAME)));
		}
	}

	protected void addEclipseSourceWithOutputFolder(final Project prj, final Classpath cp, final String path,
			final boolean test) {
		cp.getEntries().add(new SourceFolder(ConfigLoader.src_path(path, SourceSet.MAIN_SOURCE_SET_NAME),
				ConfigLoader.eclipse_output_path(path, SourceSet.MAIN_SOURCE_SET_NAME)));
		if (test) {
			cp.getEntries().add(new SourceFolder(ConfigLoader.src_path(path, SourceSet.TEST_SOURCE_SET_NAME),
					ConfigLoader.eclipse_output_path(path, SourceSet.TEST_SOURCE_SET_NAME)));
		}
	}

	@Override
	public void apply(final Project project, final Configurator configurator) {
		project.getPluginManager().apply(EclipsePlugin.class);

		final EclipseModel model = project.getExtensions().getByType(EclipseModel.class);
		model.getJdt().setJavaRuntimeName("JavaSE-14");
		model.getJdt().setSourceCompatibility(JavaVersion.VERSION_14);
		model.getJdt().setTargetCompatibility(JavaVersion.VERSION_14);

		model.project(new Action<EclipseProject>() {

			@Override
			public void execute(final EclipseProject item) {
				item.getLinkedResources()
						.add(new Link("build", "2", null, "FF_BUILD_DIR/" + project.getName() + "/build"));
				item.getLinkedResources()
						.add(new Link(".gradle", "2", null, "FF_BUILD_DIR/" + project.getName() + "/.gradle"));
			}

		});

		final EclipseClasspath classpath = model.getClasspath();
		classpath.setDefaultOutputDir(new File("build/eclipse_classes"));
		classpath.file(new Action<XmlFileContentMerger>() {
			@Override
			public void execute(final XmlFileContentMerger merger) {
				System.out.println("The merger is " + merger.getClass());
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
								addEclipseSourceWithOutputFolder(project, cp, entry.getKey(),
										configurator.test(f.get("test")));
							} else {
								addEclipseSourceFolder(cp, entry.getKey(), configurator.test(f.get("test")));
							}
						}

						final List<String> builds = configurator.getList("eclipse_build");
						for (final String path : builds) {
							cp.getEntries().add(new ClasspathEntry() {

								@Override
								public void appendNode(final Node node) {
									System.out.println("Build the entry");
									final Map<String, String> attrs = new LinkedHashMap<>();
									attrs.put("kind", "lib");
									attrs.put("path", "build/classes/" + path + "/main");
									node.appendNode("classpathentry", attrs);
									System.out.println(node);
								}

								@Override
								public String getKind() {
									return "lib";
								}

								@Override
								public String toString() {
									return "lib to " + "build/classes/" + path + "/main";
								}
							});
						}
					}
				});
			}
		});
	}
}
