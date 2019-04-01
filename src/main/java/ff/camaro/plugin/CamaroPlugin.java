package ff.camaro.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Rule;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.Upload;
import org.gradle.jvm.tasks.Jar;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettings;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;

import ff.camaro.ConfigLoader;
import ff.camaro.Configurator;
import ff.camaro.FFSourceSet;
import ff.camaro.Util;
import ff.camaro.plugin.gradle_plugin.GradlePlugin;
import ff.camaro.plugin.tasks.BaseTask;
import ff.camaro.plugin.tasks.builder.PublishBuilder;
import ff.camaro.plugin.tasks.builder.TaskBuilder;

public abstract class CamaroPlugin extends Configurator implements Plugin<Project> {

	private final ObjectFactory objectFactory;

	public CamaroPlugin(final ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}

	private void addFFMainSourceSet(final Project prj, final String name, final boolean test) {
		final DependencyHandler dependencies = prj.getDependencies();
		final JavaPluginConvention javaConvenion = prj.getConvention().getPlugin(JavaPluginConvention.class);

		final SourceSetContainer sourceSets = javaConvenion.getSourceSets();

		sourceSets.all(new Action<SourceSet>() {
			@Override
			public void execute(final SourceSet sourceSet) {
				if (!test) {
					if (sourceSet.getName().equals(SourceSet.TEST_SOURCE_SET_NAME)) {
						return;
					}
				}
				final FFSourceSet ffSourceSet = new FFSourceSet(((DefaultSourceSet) sourceSet).getDisplayName(),
						objectFactory, sourceSet.getExtensions());
				new DslObject(sourceSet).getConvention().getPlugins().put(name, ffSourceSet);

				prj.mkdir(prj.file(ConfigLoader.src_path(name, sourceSet.getName())));
				final File outdir = new File(prj.getBuildDir(),
						ConfigLoader.output_path(prj, name, sourceSet.getName()));
				prj.mkdir(outdir);
				ffSourceSet.getFf().srcDir(ConfigLoader.src_path(name, sourceSet.getName()));
				ffSourceSet.getFf().setOutputDir(outdir);
				sourceSets.add(ffSourceSet);
			}
		});

		final SourceSet main_set = javaConvenion.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		final FFSourceSet ff_set = (FFSourceSet) new DslObject(main_set).getConvention().getPlugins().get(name);
		dependencies.add("compile", prj.files(ff_set.getFf().getOutputDir()));

		prj.afterEvaluate(new Action<Project>() {
			@Override
			public void execute(final Project project) {
				final TaskContainer tasks = project.getTasks();
				tasks.getByName("jar", new Action<Task>() {
					@Override
					public void execute(final Task task) {
						final Jar jar = (Jar) task;
						jar.from(ff_set.getFf().getOutputDir()).include("**/*.class");
					}
				});
			}
		});
	}

	@Override
	public final void apply(final Project target) {
		// configure rules
		target.getTasks().addRule(new Rule() {

			@Override
			public void apply(final String text) {
				if (text.startsWith("kitt_")) {
					target.getTasks().create(text, new Action<Task>() {

						@SuppressWarnings("unchecked")
						@Override
						public void execute(final Task t) {
							try {
								final Configuration kitt_conf = project.getConfigurations().getByName("kitt");
								final Set<File> files = kitt_conf.resolve();
								final Set<URL> urls = new HashSet<>();
								for (final File f : files) {
									System.out.println("libraries are " + f);
									urls.add(f.toURI().toURL());
								}
								try (final URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]))) {
									final LoadSettings settings = new LoadSettingsBuilder().setLabel("KITT").build();
									final Load load = new Load(settings);
									Map<String, Object> cfg = null;
									try (FileInputStream in = new FileInputStream(project.file("kitt.yml"))) {
										cfg = (Map<String, Object>) load.loadFromInputStream(in);
									}
									final String code = text.substring("kitt_".length());
									final int ix = code.indexOf("#");
									if (ix == -1) {
										throw new GradleException("kitt task need a package: pck#function");
									}

									final Object obj_pck = cfg.get(code.substring(0, ix));
									if (obj_pck == null) {
										throw new GradleException("FF module not found " + code.substring(0, ix));
									}
									final String pck = String.valueOf(obj_pck);
									Class<?> module = null;
									if (module == null) {
										try {
											module = loader.loadClass(pck);
										} catch (final Exception e) {
										}
									}
									if (module == null) {
										try {
											module = loader.loadClass(pck + ".Module");
										} catch (final Exception e) {
										}
									}
									if (module == null) {
										try {
											final int lix = pck.lastIndexOf(".");
											if (lix != -1) {
												final String lastName = pck.substring(lix + 1);
												module = loader
														.loadClass(pck + "." + Character.toUpperCase(lastName.charAt(0))
																+ lastName.substring(1));
											}
										} catch (final Exception e) {
										}
									}
									if (module == null) {
										throw new ClassNotFoundException("Class not found " + pck);
									}
									final String function = code.substring(ix + 1);
									Object result = null;
									boolean process = true;
									if (process) {
										try {
											result = module.getMethod(function, Project.class).invoke(null, project);
											process = false;
										} catch (final NoSuchMethodException e) {
										}
									}
									if (process) {
										try {
											result = module.getMethod(function, String.class, String.class).invoke(null,
													project.getRootDir().getAbsolutePath(),
													project.getBuildDir().getAbsolutePath());
											process = false;
										} catch (final NoSuchMethodException e) {
										}
									}
									if (process) {
										try {
											result = module.getMethod(function, String.class).invoke(null,
													project.getRootDir().getAbsolutePath());
											process = false;
										} catch (final NoSuchMethodException e) {
										}
									}
									if (process) {
										try {
											result = module.getMethod(function).invoke(null);
											process = false;
										} catch (final NoSuchMethodException e) {
										}
									}
									if (process) {
										throw new NoSuchMethodException(module.getName() + "#" + function);
									}
									loader.loadClass("ff.kitt.Utils").getMethod("waitUntil", Object.class).invoke(null,
											result);
								}
							} catch (final Exception e) {
								throw new RuntimeException(e);
							}
						}

					});

				}
			}

			@Override
			public String getDescription() {
				return "Execute KITT tasks";
			}

		});
		final String file = System.getenv("FF_BUILD_DIR");
		final File ffBuildDir = new File(file);
		final File buildDir = new File(ffBuildDir, target.getName() + "/build");
		buildDir.mkdirs();
		target.setBuildDir(buildDir);

		new File(ffBuildDir, target.getName() + "/.gradle").mkdirs();
		new File(ffBuildDir, target.getName() + "/eclipse").mkdirs();

		setup(target, ConfigLoader.plugin.load(target, getConfiguration()));
		final RepositoryHandler repositories = project.getRepositories();
		repositories.add(repositories.maven(new Action<MavenArtifactRepository>() {
			@Override
			public void execute(final MavenArtifactRepository repo) {
				repo.setUrl(Util.getFFRepo() + "/repo");
			}
		}));
		repositories.add(repositories.maven(new Action<MavenArtifactRepository>() {
			@Override
			public void execute(final MavenArtifactRepository repo) {
				repo.setUrl(Util.getFFRepo());
			}
		}));
		try {
			final List<String> plugins = getList("plugins");
			for (final String plugin : plugins) {
				loadGradlePlugin(plugin).apply(project, this);
			}
			final ConfigurationContainer c = project.getConfigurations();
			final Map<String, Object> configurations = getMap("configurations");
			for (final Map.Entry<String, Object> entry : configurations.entrySet()) {
				final Configuration cinstance = c.maybeCreate(entry.getKey());
				final List<String> extendsList = toList(entry.getValue());
				final List<Configuration> cfgs = new ArrayList<>(extendsList.size());
				for (final String e : extendsList) {
					cfgs.add(c.maybeCreate(e));
				}
				cinstance.extendsFrom(cfgs.toArray(new Configuration[0]));
			}

			final Map<String, Object> dependencies = getMap("dependencies");
			final DependencyHandler d = project.getDependencies();
			for (final Map.Entry<String, Object> entry : dependencies.entrySet()) {
				final List<String> values = toList(entry.getValue());
				for (final String dependency : values) {
					d.add(entry.getKey(), dependency);
				}
			}

			final Map<String, Object> tasks = getMap("tasks");
			for (final Map.Entry<String, Object> entry : tasks.entrySet()) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> task = (Map<String, Object>) entry.getValue();
				final String clazz = toString(task.get("class"));
				if (clazz == null) {
					project.getTasks().create(entry.getKey(), BaseTask.class, new Action<BaseTask>() {

						@Override
						public void execute(final BaseTask t) {
							t.setup(project, task, CamaroPlugin.this);
						}

					});
				} else {
					if (clazz.startsWith("$")) {
						@SuppressWarnings("unchecked")
						final Class<? extends TaskBuilder> cls = (Class<? extends TaskBuilder>) Class
								.forName("ff.camaro.plugin.tasks.builder." + clazz.substring(1).trim() + "Builder");
						final TaskBuilder builder = cls.getConstructor().newInstance();
						builder.setDefinition(task, this);
						builder.define(project, entry.getKey());
					} else {
						@SuppressWarnings("unchecked")
						final Class<? extends BaseTask> cls = (Class<? extends BaseTask>) Class
								.forName("ff.camaro.plugin.tasks." + clazz.trim());
						project.getTasks().create(entry.getKey(), cls, new Action<BaseTask>() {

							@Override
							public void execute(final BaseTask t) {
								t.setup(project, task, CamaroPlugin.this);
							}

						});
					}
				}
			}

			final Map<String, Object> sources = getMap("sources");
			for (final Map.Entry<String, Object> entry : sources.entrySet()) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> source = (Map<String, Object>) entry.getValue();
				addFFMainSourceSet(project, entry.getKey(), test(source.get("test")));
			}

			final Map<String, Object> artifacts = getMap("artifacts");
			final ArtifactHandler a = project.getArtifacts();
			for (final Map.Entry<String, Object> entry : artifacts.entrySet()) {
				final List<String> values = toList(entry.getValue());
				for (final String artifact : values) {
					if (artifact.startsWith("$task")) {
						a.add(entry.getKey(),
								project.getTasks().getByName(artifact.substring("$task".length()).trim()));
					} else {
						a.add(entry.getKey(), project.file(artifact));
					}
				}
			}

			final Map<String, Object> publish = getMap("publish_to");
			final Upload upload = (Upload) project.getTasks().findByName("uploadArchives");
			PublishBuilder.add_publishers(upload, this, publish);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public abstract String getConfiguration();

	private GradlePlugin loadGradlePlugin(final String plugin) throws Exception {
		return loadClass("ff.camaro.plugin.gradle_plugin", plugin);
	}
}
