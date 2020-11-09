package ff.camaro.plugin;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Rule;
import org.gradle.api.Task;
import org.gradle.api.XmlProvider;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.ivy.IvyArtifact;
import org.gradle.api.publish.ivy.IvyConfiguration;
import org.gradle.api.publish.ivy.IvyConfigurationContainer;
import org.gradle.api.publish.ivy.IvyPublication;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.jvm.tasks.Jar;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettings;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;

import ff.camaro.ArtifactInfo;
import ff.camaro.CamaroSourceSet;
import ff.camaro.ConfigLoader;
import ff.camaro.Configurator;
import ff.camaro.MapStore;
import ff.camaro.Util;
import ff.camaro.artifact.Artifact;
import ff.camaro.artifact.Artifacts;
import ff.camaro.plugin.gradle_plugin.GradlePlugin;
import ff.camaro.plugin.gradle_plugin.Java;
import ff.camaro.plugin.tasks.BaseTask;
import ff.camaro.plugin.tasks.DefaultTask;
import ff.camaro.plugin.tasks.builder.TaskBuilder;
import groovy.json.JsonSlurper;
import groovy.util.Node;
import groovy.util.NodeList;

public abstract class CamaroPlugin extends Configurator implements Plugin<Project> {

	private static final String CAMARO = "__camaro__";

	public static Set<String> group_configurations = Util.set("macros", "java", "dart", "python", "js", //
			"macro_test", "java_test", "dart_test", "python_test", "js_test");

	public static void add_repositories(final PublishingExtension publisher, final Configurator config,
			final Map<String, Object> map) {
		final RepositoryHandler repositories = publisher.getRepositories();
		for (final Map.Entry<String, Object> entry : map.entrySet()) {
			final String repo = config.toString(entry.getValue());
			if (repo.startsWith("$local")) {
				final IvyArtifactRepository ivy = repositories.ivy(new Action<IvyArtifactRepository>() {

					@Override
					public void execute(final IvyArtifactRepository repo) {
						repo.setUrl(Util.getFFRepo());
					}

				});
				repositories.add(ivy);
			} else {
				throw new UnsupportedOperationException("repo not supported: " + repo);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> camaro_project_config(final Project target) {
		File camaro_build = new File(target.getProjectDir(), "camaro.config.json");
		if (camaro_build.exists()) {
			final JsonSlurper sluper = new JsonSlurper();
			final Map<String, Object> result = (Map<String, Object>) sluper.parse(camaro_build);
			return result;
		}
		camaro_build = new File(target.getProjectDir(), "camaro.config.yml");
		if (camaro_build.exists()) {
			final LoadSettings settings = new LoadSettingsBuilder().setLabel("Camaro Config").build();
			final Load load = new Load(settings);
			try (FileReader reader = new FileReader(camaro_build)) {
				return (Map<String, Object>) load.loadFromReader(reader);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
		final JsonSlurper sluper = new JsonSlurper();
		final Map<String, Object> result = (Map<String, Object>) sluper.parseText("{\r\n" + //
				"				\"kitt\": {\r\n" + //
				"					\"kitt\": \"ff.kitt\"\r\n" + //
				"				},\r\n" + //
				"				\"languages\": [],\r\n" + //
				"				\"dependencies\": {},\r\n" + //
				"				\"variables\": {}	\r\n" + //
				"			}");
		return result;
	}

	public static CamaroMetadata metadata(final Project prj) {
		return (CamaroMetadata) prj.getConvention().findByName(CamaroPlugin.CAMARO);
	}

	private final ObjectFactory objectFactory;

	public CamaroPlugin(final ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}

	protected ModuleDependency add_dependency(final Project project, final Map<String, Object> props, final String cfg,
			final Artifact artifact) {
		final DependencyHandler d = project.getDependencies();
		final String classifier = artifact.getClassifierString();
		final Dependency dep = d.add(cfg, artifact.getOrg() + ":" + artifact.getName() + ":" + artifact.getVersion()
				+ (classifier != null ? ":" + classifier : ""));
		final ModuleDependency moduleDependency = (ModuleDependency) dep;
		if (artifact.getCfg() != null) {
			add_target_configuration(moduleDependency, d, cfg, artifact.getCfg());
		}
		moduleDependency.setTransitive(artifact.isTransitive());
		return moduleDependency;
	}

	protected ModuleDependency add_dependency(final Project project, final Map<String, Object> props, final String cfg,
			final String dependency) {
		final String line = replace_str(dependency, props);
		final Artifact artifact = Artifacts.parse(line);
		return add_dependency(project, props, cfg, artifact);
	}

	private void add_target_configuration(final ModuleDependency dep, final DependencyHandler d, final String dConf,
			final String conf) {
		if (dep.getArtifacts() != null && dep.getArtifacts().size() > 0) {
			final Dependency dep2 = d.add(dConf, dep.getGroup() + ":" + dep.getName() + ":" + dep.getVersion());
			((ModuleDependency) dep2).setTargetConfiguration(conf);
		} else {
			dep.setTargetConfiguration(conf);
		}
	}

	private void addSourceSet(final Project prj, final String name, final String type, final boolean test) {
		final DependencyHandler dependencies = prj.getDependencies();
		final JavaPluginConvention javaConvenion = prj.getConvention().getPlugin(JavaPluginConvention.class);

		final SourceSetContainer sourceSets = javaConvenion.getSourceSets();

		if (type == null) {
			sourceSets.all(new Action<SourceSet>() {
				@Override
				public void execute(final SourceSet sourceSet) {
					final String sName = sourceSet.getName();
					if (!(sName.equals(SourceSet.TEST_SOURCE_SET_NAME)
							|| sName.equals(SourceSet.MAIN_SOURCE_SET_NAME))) {
						return;
					}
					if (!test) {
						if (sourceSet.getName().equals(SourceSet.TEST_SOURCE_SET_NAME)) {
							return;
						}
					}
					final CamaroSourceSet ffSourceSet = new CamaroSourceSet(
							((DefaultSourceSet) sourceSet).getDisplayName(), objectFactory, sourceSet.getExtensions());
					new DslObject(sourceSet).getConvention().getPlugins().put(name, ffSourceSet);

					prj.mkdir(prj.file(ConfigLoader.src_path(name, sourceSet.getName())));
					final File outdir = new File(prj.getBuildDir(),
							ConfigLoader.output_path(prj, name, sourceSet.getName()));
					prj.mkdir(outdir);

					ffSourceSet.getSrcDir().srcDir(ConfigLoader.src_path(name, sourceSet.getName()));
					ffSourceSet.getSrcDir().setOutputDir(outdir);
				}
			});

			final SourceSet main_set = javaConvenion.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			final CamaroSourceSet ff_set = (CamaroSourceSet) new DslObject(main_set).getConvention().getPlugins()
					.get(name);
			dependencies.add("compileOnly", prj.files(ff_set.getSrcDir().getOutputDir()));
			dependencies.add("compileOnly", project.files(project.getBuildDir().toPath()
					.resolve(ConfigLoader.output_main_path(project, "ff_java")).toFile()));

			prj.afterEvaluate(new Action<Project>() {
				@Override
				public void execute(final Project project) {
					final TaskContainer tasks = project.getTasks();
					tasks.getByName("jar", new Action<Task>() {
						@Override
						public void execute(final Task task) {
							final Jar jar = (Jar) task;
							jar.from(ff_set.getSrcDir().getOutputDir()).include("**/*.class");
						}
					});
				}
			});
		} else if ("definition".equals(type)) {
			final SourceSet sourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			final String src_path = ConfigLoader.src_path(name, sourceSet.getName());
			prj.mkdir(prj.file(src_path));
			final File outdir = new File(prj.getBuildDir(), ConfigLoader.output_path(prj, name, sourceSet.getName()));
			prj.mkdir(outdir);
			sourceSet.getJava().srcDir(prj.file(src_path));
			sourceSet.getJava().setOutputDir(outdir);

			final String language = name.substring("interfaces/".length());
			Java.createJavaCompileTask(project, name.replace("/", "_"), prj.file(src_path), outdir,
					project.files(prj.getConfigurations().getByName(language).resolve()));
		} else {
			sourceSets.all(new Action<SourceSet>() {

				@Override
				public void execute(final SourceSet sourceSet) {
					if (!test) {
						if (sourceSet.getName().equals(SourceSet.TEST_SOURCE_SET_NAME)) {
							return;
						}
					}

					final String src_path = ConfigLoader.src_path(name, sourceSet.getName());
					prj.mkdir(prj.file(src_path));
					if ("java".equals(type)) {
						sourceSet.getJava().srcDir(src_path);
					} else if ("resources".equals(type)) {
						sourceSet.getResources().srcDir(src_path);
					} else {
						final CamaroSourceSet camaroSourceSet = (CamaroSourceSet) new DslObject(sourceSet)
								.getConvention().getPlugins().get(type);
						if (camaroSourceSet != null) {
							camaroSourceSet.getSrcDir().srcDir(src_path);
						}
					}
				}

			});
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public final void apply(final Project target) {
		final CamaroMetadata metadata = new CamaroMetadata();
		target.getConvention().add(CamaroPlugin.CAMARO, metadata);
		final MapStore camaro_build = new MapStore(CamaroPlugin.camaro_project_config(target));
		final List<String> languages = camaro_build.getList("languages");
		for (final String str : languages) {
			if (str.startsWith("-")) {
				continue;
			}
			metadata.enabled_languages(str);
		}
		// configure rules
		target.getTasks().addRule(new Rule() {

			@Override
			public void apply(final String text) {
				if (text.startsWith("kitt_")) {
					target.getTasks().create(text, new Action<Task>() {

						@Override
						public void execute(final Task t) {
							try {
								final String code = text.substring("kitt_".length());
								final int ix = code.indexOf("#");
								if (ix == -1) {
									throw new GradleException("kitt task need a package: pck#function");
								}
								final Configuration kitt_conf = project.getConfigurations().getByName("kitt");
								final Set<File> files = kitt_conf.resolve();
								final Set<URL> urls = new HashSet<>();
								for (final File f : files) {
									urls.add(f.toURI().toURL());
								}

								try (final URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]))) {
									final Map<String, Object> camaro_cfg = CamaroPlugin.camaro_project_config(project);
									final Map<String, Object> cfg = (Map<String, Object>) camaro_cfg.get("kitt");
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

		init(target, ConfigLoader.plugin.load(target, getConfiguration()));
		final RepositoryHandler repositories = project.getRepositories();
		repositories.add(repositories.maven(new Action<MavenArtifactRepository>() {
			@Override
			public void execute(final MavenArtifactRepository repo) {
				repo.setUrl(Util.getFFRepo() + "/repo");
			}
		}));
		repositories.add(repositories.ivy(new Action<IvyArtifactRepository>() {
			@Override
			public void execute(final IvyArtifactRepository repo) {
				repo.setUrl(Util.getFFRepo());
			}
		}));
		repositories.add(repositories.mavenCentral());
		repositories.add(repositories.google());

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
				if (entry.getKey().equals("camaro")) {
					continue;
				}
				if (entry.getKey().equals("kitt")) {
					continue;
				}
				metadata.getConfigurations().add(entry.getKey());
			}

			final Map<String, Object> dependencies = getMap("dependencies");
			final DependencyHandler d = project.getDependencies();
			for (final Map.Entry<String, Object> entry : dependencies.entrySet()) {
				final Set<String> values = new HashSet<>(toList(entry.getValue()));
				for (String dependency : values) {
					final int ix = dependency.lastIndexOf(";");
					String conf = null;
					if (ix != -1) {
						conf = dependency.substring(ix + 1).trim();
						dependency = dependency.substring(0, ix).trim();
					}
					final Dependency dep = d.add(entry.getKey(), dependency);
					if (conf != null) {
						if (!"none".equals(conf)) {
							add_target_configuration((ModuleDependency) dep, d, entry.getKey(), conf);
						}
					} else {
						if (CamaroPlugin.group_configurations.contains(entry.getKey())) {
							add_target_configuration((ModuleDependency) dep, d, entry.getKey(), entry.getKey());
						}
					}
				}
			}

			final Map<String, Object> props = camaro_build.getMap("variables");
			final Map<String, Object> custom_deps = camaro_build.getMap("dependencies");
			for (final Map.Entry<String, Object> custom_cfg : custom_deps.entrySet()) {
				final List<Object> deps = (List<Object>) custom_cfg.getValue();
				for (final Object dep : deps) {
					if (dep instanceof String) {
						add_dependency(project, props, custom_cfg.getKey(), (String) dep);
					} else {
						final List<Object> list_dep = (List<Object>) dep;
						final Iterator<Object> iterator = list_dep.iterator();
						final String line = next_item(iterator);
						final Artifact artifact = Artifacts.parse(replace_str(line, props));
						ModuleDependency module_dep = null;
						if (artifact.is_complete()) {
							module_dep = add_dependency(target, props, custom_cfg.getKey(), artifact);
							final String is_exclude = next_item(iterator);
							if ("exclude".equals(is_exclude)) {
								apply_excludes(props, iterator, module_dep);
							}
						} else {
							while (true) {
								final String sline = next_item(iterator);
								if (sline == null) {
									break;
								}
								if ("exclude".equals(sline)) {
									if (module_dep == null) {
										break;
									}
									apply_excludes(props, iterator, module_dep);
								}
								final Artifact sArtifact = Artifacts.parse(replace_str(sline, props));
								final Artifact mArtifact = merge_artifact(artifact, sArtifact);
								if (mArtifact.is_complete()) {
									module_dep = add_dependency(target, props, custom_cfg.getKey(), mArtifact);
								}
							}
						}

					}
				}
			}

			final Map<String, Object> tasks = getMap("tasks");
			for (final Map.Entry<String, Object> entry : tasks.entrySet()) {
				final Map<String, Object> task = (Map<String, Object>) entry.getValue();
				final String clazz = toString(task.get("class"));
				if (clazz == null) {
					project.getTasks().create(entry.getKey(), DefaultTask.class, new Action<DefaultTask>() {

						@Override
						public void execute(final DefaultTask t) {
							t.init(new MapStore(task), CamaroPlugin.this);
						}

					});
				} else {
					if (clazz.startsWith("$")) {
						final Class<? extends TaskBuilder<?>> cls = (Class<? extends TaskBuilder<?>>) Class
								.forName("ff.camaro.plugin.tasks.builder." + clazz.substring(1).trim() + "Builder");
						final TaskBuilder<?> builder = cls.getConstructor().newInstance();
						builder.init(new MapStore(task), this);
						builder.define(project, entry.getKey());
					} else {
						final Class<? extends BaseTask> cls = (Class<? extends BaseTask>) Class
								.forName("ff.camaro.plugin.tasks." + clazz.trim());
						project.getTasks().create(entry.getKey(), cls, new Action<BaseTask>() {

							@Override
							public void execute(final BaseTask t) {
								t.init(new MapStore(task), CamaroPlugin.this);
							}

						});
					}
				}
			}

			final Map<String, Object> sources = getMap("sources");
			for (final Map.Entry<String, Object> entry : sources.entrySet()) {
				final MapStore source = new MapStore((Map<String, Object>) entry.getValue());
				addSourceSet(project, entry.getKey(), source.getString("type"), test(source.getString("test")));
			}

			final PublishingExtension publisher = project.getExtensions().getByType(PublishingExtension.class);
			final Map<String, Object> publish = getMap("publish_to");
			CamaroPlugin.add_repositories(publisher, this, publish);

			final PublicationContainer publications = publisher.getPublications();

			final ArtifactInfo info = getArtifactInfo();
			final IvyPublication publication = publications.create("ff_publishing", IvyPublication.class,
					new Action<IvyPublication>() {

						@Override
						public void execute(final IvyPublication ivy) {
							ivy.setOrganisation(info.getGroup());
							ivy.setModule(info.getName());
							ivy.setRevision(info.getVersion());
						}

					});

			final List<String> artifacts = getList("artifacts");
			for (String artifact : artifacts) {
				final int ix = artifact.lastIndexOf(";");
				String conf = null;
				if (ix != -1) {
					conf = artifact.substring(ix + 1).trim();
					artifact = artifact.substring(0, ix).trim();
				}
				if (artifact.startsWith("$task")) {
					final IvyArtifact a = publication
							.artifact(project.getTasks().getByName(artifact.substring("$task".length()).trim()));
					if (conf != null) {
						a.setConf(conf);
					}
				} else {
					final IvyArtifact a = publication.artifact(project.file(artifact));
					if (conf != null) {
						a.setConf(conf);
					}
				}
			}

			publication.configurations(new Action<IvyConfigurationContainer>() {

				@Override
				public void execute(final IvyConfigurationContainer ivy) {
					for (final Map.Entry<String, Object> entry : configurations.entrySet()) {
						if (entry.getKey().equals("camaro")) {
							continue;
						}
						if (entry.getKey().equals("kitt")) {
							continue;
						}
						final IvyConfiguration ivyConfiguration = ivy.create(entry.getKey());
						ivy.add(ivyConfiguration);
						final List<String> extendsList = toList(entry.getValue());
						for (final String e : extendsList) {
							ivyConfiguration.getExtends().add(e);
						}
					}
				}
			});

			publication.getDescriptor().withXml(new Action<XmlProvider>() {

				@SuppressWarnings({ "rawtypes" })
				@Override
				public void execute(final XmlProvider xml) {
					final NodeList list = (NodeList) xml.asNode().get("dependencies");
					final Node node = (Node) list.get(0);
					for (final Configuration cfg : c) {
						if (cfg.getName().equals("camaro")) {
							continue;
						}
						if (cfg.getName().equals("kitt")) {
							continue;
						}
						if (cfg.getName().equals("jacocoAgent")) {
							continue;
						}
						if (cfg.getName().equals("jacocoAnt")) {
							continue;
						}
						final DependencySet dependencies = cfg.getDependencies();
						for (final Dependency dep : dependencies) {
							final Map<String, Object> attrs = Util.map("org", dep.getGroup(), //
									"name", dep.getName(), //
									"rev", dep.getVersion() //
							);
							if (dep instanceof ModuleDependency) {
								final ModuleDependency mdep = (ModuleDependency) dep;
								attrs.put("conf", cfg.getName() + "->" + nvl_dep(mdep.getTargetConfiguration()));
								final Set<DependencyArtifact> artifacts = mdep.getArtifacts();
								final Node depNode = node.appendNode("dependency", attrs);
								final Map depAttrs = depNode.attributes();
								for (final DependencyArtifact artifact : artifacts) {
									final String classifier = artifact.getClassifier();
									final Map<String, Object> artifactAttrs = new HashMap<>();
									if (classifier != null) {
										depAttrs.put("xmlns:m", "http://ant.apache.org/ivy/maven");
										depAttrs.put("m:classifier", classifier);

										artifactAttrs.put("xmlns:m", "http://ant.apache.org/ivy/maven");
										artifactAttrs.put("m:classifier", classifier);
									}
									final String extension = artifact.getExtension();
									if (extension != null) {
										artifactAttrs.put("ext", extension);
									}
									artifactAttrs.put("name", artifact.getName());
									artifactAttrs.put("type", artifact.getType());
									depNode.appendNode("artifact", artifactAttrs);
								}
								final Set<ExcludeRule> rules = mdep.getExcludeRules();
								for (final ExcludeRule rule : rules) {
									final Map<String, Object> excludeAttrs = new HashMap<>();
									final String group = rule.getGroup();
									if (group != null) {
										excludeAttrs.put("org", group);
									}
									final String module = rule.getModule();
									if (module != null) {
										excludeAttrs.put("module", module);
									}
									depNode.appendNode("exclude", excludeAttrs);
								}
							} else {
								continue;
							}
						}
					}
				}

				private String nvl_dep(final String conf) {
					if (conf == null) {
						return "default";
					}
					return conf;
				}

			});

		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void apply_excludes(final Map<String, Object> props, final Iterator<Object> iterator,
			final ModuleDependency module_dep) {
		while (true) {
			final String exclude = next_item(iterator);
			if (exclude == null) {
				break;
			}
			final Artifact eArtifact = Artifacts.parse(replace_str(exclude, props));
			final Map<String, String> rule = new HashMap<>();
			if (eArtifact.getOrg() != null && eArtifact.getOrg().length() > 0) {
				rule.put("group", eArtifact.getOrg());
			}
			if (eArtifact.getName() != null && eArtifact.getName().length() > 0) {
				rule.put("module", eArtifact.getName());
			}
			module_dep.exclude(rule);
		}
	}

	public abstract String getConfiguration();

	private GradlePlugin loadGradlePlugin(final String plugin) throws Exception {
		return loadClass("ff.camaro.plugin.gradle_plugin", plugin);
	}

	private Artifact merge_artifact(final Artifact artifact, final Artifact sArtifact) {
		return new Artifact(nvl(sArtifact.getName(), artifact.getName()), //
				nvl(sArtifact.getOrg(), artifact.getOrg()), //
				nvl(sArtifact.getVersion(), artifact.getVersion()), //
				artifact.isTransitive(), //
				nvl(sArtifact.getClassifierString(), artifact.getClassifierString()), //
				nvl(sArtifact.getCfg(), artifact.getCfg()));
	}

	protected String next_item(final Iterator<Object> iterator) {
		if (iterator.hasNext()) {
			return (String) iterator.next();
		}
		return null;
	}

	private String nvl(final String val1, final String val2) {
		if (val1 == null || val1.length() == 0) {
			return val2;
		}
		return val1;
	}

	protected String replace_str(String txt, final Map<String, Object> props) {
		for (final Map.Entry<String, Object> entry : props.entrySet()) {
			txt = txt.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
		}
		return txt;
	}
}
