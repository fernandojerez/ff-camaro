package ff.camaro.plugin.tasks;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.StopActionException;
import org.gradle.api.tasks.TaskAction;

import ff.camaro.CamaroTask;
import ff.camaro.FFSourceSet;

public class FFCompiler extends CamaroTask {

	private File analizedOutput;
	private File definitionOutput;
	private File macroOutput;
	private File javaOutput;
	private File output;
	private File interfaces;
	private File macros;
	private String compilerClass;
	private String folder;
	private String sourceSet;
	private String[] configuration;
	private File moduleOutputDir;
	private String type;

	public FFCompiler() {
		setGroup("ff");
	}

	@TaskAction
	public void compile() throws Exception {
		compile(sourceSet, folder);
	}

	void compile(final String sourceset, final String lang) throws Exception {
		final JavaPluginConvention javaConvenion = getProject().getConvention().getPlugin(JavaPluginConvention.class);
		final SourceSet main_set = javaConvenion.getSourceSets().getByName(sourceset);
		final FFSourceSet ff_set = (FFSourceSet) new DslObject(main_set).getConvention().getPlugins().get(lang);

		final Set<File> classes = main_set.getOutput().getClassesDirs().getFiles();
		Set<File> classpathFiles = main_set.getCompileClasspath().getFiles();
		final Set<URL> macroFiles = new HashSet<>();
		if (configuration != null && configuration.length > 0) {
			final Set<File> files = new HashSet<>();
			for (final String str : configuration) {
				files.addAll(getProject().getConfigurations().getByName(str).resolve());
			}
			classpathFiles = files;
		}

		final Set<File> macroConf = getProject().getConfigurations().getByName("macros").resolve();
		for (final File f : macroConf) {
			macroFiles.add(f.toURI().toURL());
		}

		if (interfaces != null) {
			classpathFiles.add(interfaces);
			macroFiles.add(interfaces.toURI().toURL());
		}

		if (macros != null) {
			if (javaOutput == null) {
				classpathFiles.add(macros);
				macroFiles.add(macros.toURI().toURL());
			}
		}

		final Set<URL> urls = new HashSet<>();
		for (final File f : classpathFiles) {
			urls.add(f.toURI().toURL());
		}
		for (final File f : classes) {
			urls.add(f.toURI().toURL());
		}

		if (moduleOutputDir != null) {
			urls.add(moduleOutputDir.toURI().toURL());
		}

		final URLClassLoader baseLibs = getClassLoader();
		final URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), baseLibs);
		Thread.currentThread().setContextClassLoader(loader);
		if (javaOutput != null) {
			macroFiles.add(javaOutput.toURI().toURL());
			macroFiles.add(macros.toURI().toURL());
		}

		try {
			if (getProject().getName().equals("ff@charger")) {
				String getenv = System.getenv("FF_REPO");
				if (getenv.startsWith("file://")) {
					getenv = getenv.substring("file://".length());
				}
				final File charger = new File(getenv + "/ff/charger/0.0.1/charger-0.0.1-java.jar");
				if (charger.exists()) {
					macroFiles.add(charger.toURI().toURL());
				}
			}
			final Configuration conf = getProject().getConfigurations().getByName("macros");
			for (final File file : conf.getFiles()) {
				macroFiles.add(file.toURI().toURL());
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		final URLClassLoader macro_loader = macroFiles.isEmpty() ? baseLibs
				: new URLClassLoader(macroFiles.toArray(new URL[0]), baseLibs);

		final File[] sources = ff_set.getFf().getSrcDirs().toArray(new File[0]);

		final File output = this.output != null ? this.output : ff_set.getFf().getOutputDir();
		final File definitionOutput = getDefinitionOutput();
		final File macros = getMacroOutput();
		final File analized = getAnalizedOutput();

		macros.mkdirs();
		analized.mkdirs();

		final Class<?> ff_compiler_class = loader.loadClass(getCompilerClass());
		final Object ff_compiler = ff_compiler_class.getConstructor().newInstance();

		ff_compiler_class.getMethod("setType", String.class) //
				.invoke(ff_compiler, type);

		ff_compiler_class
				.getMethod("setup", ClassLoader.class, ClassLoader.class, File.class, File.class, File.class,
						File.class, File[].class)//
				.invoke(ff_compiler, macro_loader, loader, output, definitionOutput, macros, analized, sources);

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

	public File getAnalizedOutput() {
		return analizedOutput;
	}

	public String getCompilerClass() {
		return compilerClass;
	}

	public File getDefinitionOutput() {
		return definitionOutput;
	}

	public String getFolder() {
		return folder;
	}

	public File getMacroOutput() {
		return macroOutput;
	}

	public File getOutput() {
		return output;
	}

	public String getSourceSet() {
		return sourceSet;
	}

	public void setAnalizedOutput(final File analizedOutput) {
		this.analizedOutput = analizedOutput;
	}

	public void setCompilationType(final String type) {
		if (type == null) {
			this.type = "LIBRARY";
		} else {
			this.type = type.toUpperCase();
		}

	}

	public void setCompilerClass(final String compilerClass) {
		this.compilerClass = compilerClass;
	}

	public void setConfiguration(final String[] configuration) {
		this.configuration = configuration;
	}

	public void setDefinitionOutput(final File definitionOutput) {
		this.definitionOutput = definitionOutput;
	}

	public void setFolder(final String folder) {
		this.folder = folder;
	}

	public void setInterfaces(final File interfaces) {
		this.interfaces = interfaces;
	}

	public void setJavaOutput(final File javaOutput) {
		this.javaOutput = javaOutput;
	}

	public void setMacroOutput(final File macroOutput) {
		this.macroOutput = macroOutput;
	}

	public void setMacros(final File macros) {
		this.macros = macros;
	}

	public void setModuleOutputDir(final File file) {
		moduleOutputDir = file;
	}

	public void setOutput(final File output) {
		this.output = output;
	}

	public void setSourceSet(final String sourceSet) {
		this.sourceSet = sourceSet;
	}
}
