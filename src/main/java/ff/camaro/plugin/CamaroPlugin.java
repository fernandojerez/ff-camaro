package ff.camaro.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.Upload;
import org.gradle.jvm.tasks.Jar;

import ff.camaro.ConfigLoader;
import ff.camaro.Configurator;
import ff.camaro.FFSourceSet;
import ff.camaro.Util;
import ff.camaro.plugin.gradle_plugin.GradlePlugin;
import ff.camaro.plugin.tasks.BaseTask;
import ff.camaro.plugin.tasks.builder.PublishBuilder;
import ff.camaro.plugin.tasks.builder.TaskBuilder;

public abstract class CamaroPlugin extends Configurator implements Plugin<Project> {

	private final SourceDirectorySetFactory sourceDirectorySetFactory;

	public CamaroPlugin(final SourceDirectorySetFactory sourceDirectorySetFactory) {
		this.sourceDirectorySetFactory = sourceDirectorySetFactory;
	}

	private void addFFMainSourceSet(final Project prj, final String name,
			final SourceDirectorySetFactory sourceDirectorySetFactory, final boolean test) {
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
						sourceDirectorySetFactory);
				new DslObject(sourceSet).getConvention().getPlugins().put(name, ffSourceSet);

				prj.mkdir(prj.file(ConfigLoader.src_path(name, sourceSet.getName())));
				final File outdir = prj.file(ConfigLoader.output_path(name, sourceSet.getName()));
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
		setup(target, ConfigLoader.plugin.load(getConfiguration()));
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
				addFFMainSourceSet(project, entry.getKey(), sourceDirectorySetFactory, test(source.get("test")));
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
