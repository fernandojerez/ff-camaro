package ff.camaro.plugin.tasks;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.StopActionException;
import org.gradle.api.tasks.TaskAction;

import ff.camaro.CamaroSourceSet;
import ff.camaro.CamaroTask;
import ff.camaro.ConfigLoader;
import ff.camaro.plugin.CamaroMetadata;
import ff.camaro.plugin.CamaroPlugin;

public class FFCompiler extends CamaroTask {

	private Path interfaces;
	private String[] configuration;
	private String compilationType;
	private boolean macroMode;
	private String language;
	private boolean testMode;

	public FFCompiler() {
		setGroup("ff");
	}

	@TaskAction
	public void compile() throws Exception {
		final CamaroMetadata metadata = CamaroPlugin.metadata(getProject());
		if (metadata.isLanguageEnabled(language)) {
			doCompile();
		} else {
			System.out.println("Language disabled in camaro.build.json");
		}
	}

	private void doCompile() throws Exception {
		final String sourceSetName = testMode ? SourceSet.TEST_SOURCE_SET_NAME : SourceSet.MAIN_SOURCE_SET_NAME;
		final JavaPluginConvention javaConvenion = getProject().getConvention().getPlugin(JavaPluginConvention.class);
		final SourceSet sourceSet = javaConvenion.getSourceSets().getByName(sourceSetName);
		final CamaroSourceSet ffSet = (CamaroSourceSet) new DslObject(sourceSet).getConvention().getPlugins()
				.get(macroMode ? "macros" : "ff");

		final Set<File> classes = sourceSet.getOutput().getClassesDirs().getFiles();
		Set<File> classpathFiles = sourceSet.getCompileClasspath().getFiles();

		final Set<URL> macroFiles = new HashSet<>();
		if (configuration != null && configuration.length > 0) {
			final Set<File> files = new HashSet<>();
			for (final String str : configuration) {
				final Set<File> filesResolved = getProject().getConfigurations().getByName(str).resolve();
				files.addAll(filesResolved);
			}
			classpathFiles = files;
		}

		final Set<File> macroConf = getProject().getConfigurations().getByName("macros").resolve();
		for (final File f : macroConf) {
			macroFiles.add(f.toURI().toURL());
		}

		if (interfaces != null) {
			classpathFiles.add(interfaces.toFile());
			macroFiles.add(interfaces.toUri().toURL());
		}

		final var project = getProject();
		final var buildDir = getProject().getBuildDir().toPath();
		final Path macrosOutput = buildDir.resolve(
				ConfigLoader.output_path(project, macroMode ? "ff_java" : "macros", SourceSet.MAIN_SOURCE_SET_NAME));
		final Path javaOutput = buildDir
				.resolve(ConfigLoader.output_path(project, "ff_java", SourceSet.MAIN_SOURCE_SET_NAME));
		if (macroMode) {
			classpathFiles.add(macrosOutput.toFile());
		} else {
			macroFiles.add(macrosOutput.toUri().toURL());
		}

		final String source = macroMode ? "macros" : "ff_" + language;
		Path output = null;
		if (testMode) {
			output = buildDir.resolve(ConfigLoader.output_test_path(getProject(), source));
		} else {
			output = buildDir.resolve(ConfigLoader.output_main_path(getProject(), source));
		}

		final String suffix = testMode ? "_test" : "";

		final Path depsOutput = macroMode ? null : //
				compilationType.equals("APPLICATION") ? //
						buildDir.resolve("deps" + suffix + "/" + source) : //
						null;
		final Path definitionOutput = "java".equals(language) || "macros".equals(language) ? //
				null : //
				output;
		final Path macros = buildDir.resolve("macros" + suffix + "/" + source);
		final Path analized = buildDir.resolve("analized" + suffix + "/" + source);

		if (macroMode) {
			macroFiles.add(output.toUri().toURL());
		}

		final Set<URL> urls = new HashSet<>();
		for (final File f : classpathFiles) {
			urls.add(f.toURI().toURL());
		}
		for (final File f : classes) {
			urls.add(f.toURI().toURL());
		}

		if (testMode) {
			urls.add(buildDir.resolve(ConfigLoader.output_main_path(getProject(), source)).toUri().toURL());
		}

		final URLClassLoader baseLibs = getClassLoader();
		final URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), baseLibs);

		if (javaOutput != null) {
			macroFiles.add(javaOutput.toUri().toURL());
			macroFiles.add(macros.toUri().toURL());
		}

		final URLClassLoader macro_loader = macroFiles.isEmpty() ? baseLibs
				: new URLClassLoader(macroFiles.toArray(new URL[0]), baseLibs);

		final File[] sources = ffSet.getSrcDir().getSrcDirs().toArray(new File[0]);

		if (depsOutput != null) {
			Files.createDirectories(depsOutput);
		}
		Files.createDirectories(macros);
		Files.createDirectories(analized);

		final Class<?> ff_compiler_class = macro_loader.loadClass("ff.lang.compiler.camaro.FFCamaroCompiler");
		final Object ff_compiler = ff_compiler_class.getMethod("create", String.class).invoke(null,
				language.toUpperCase());

		final Object profile = ff_compiler_class.getMethod("getProfile", String.class) //
				.invoke(ff_compiler, "MASTER");

		System.out.println("Compiling using profile: " + profile);

		ff_compiler_class.getMethod("setType", String.class) //
				.invoke(ff_compiler, compilationType);

		ff_compiler_class
				.getMethod("setup", profile.getClass(), ClassLoader.class, ClassLoader.class, File.class, File.class,
						File.class, File.class, File.class, File.class)//
				.invoke(ff_compiler, profile, macro_loader, loader, output.toFile(),
						depsOutput != null ? depsOutput.toFile() : null,
						definitionOutput != null ? definitionOutput.toFile() : null, //
						macros.toFile(), analized.toFile(), sources[0]);

		final Method compile = ff_compiler.getClass().getMethod("compile", File.class);
		try {
			for (final File source_dir : sources) {
				if (Boolean.FALSE.equals(compile.invoke(ff_compiler, source_dir))) {
					throw new StopActionException("Failed to compile ff sources");
				}
			}
		} catch (final InvocationTargetException e) {
			final Throwable t = e.getCause();
			if (t instanceof Error) {
				throw (Error) t;
			} else {
				throw (Exception) t;
			}
		} finally {
			if (Boolean.FALSE.equals(ff_compiler.getClass().getMethod("end").invoke(ff_compiler))) {
				throw new StopActionException("Failed to compile ff sources");
			}
		}
	}

	public void setCompilationType(final String compilationType) {
		this.compilationType = compilationType.toUpperCase();
	}

	public void setConfiguration(final String[] configuration) {
		this.configuration = configuration;
	}

	public void setInterfaces(final Path interfaces) {
		this.interfaces = interfaces;
	}

	public void setLanguage(final String language) {
		this.language = language;
	}

	public void setMacroMode(final boolean macroMode) {
		this.macroMode = macroMode;
	}

	public void setTestMode(final boolean testMode) {
		this.testMode = testMode;
	}
}
