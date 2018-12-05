package ff.camaro.plugin.tasks;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.CopySpec;
import org.gradle.jvm.tasks.Jar;
import org.gradle.process.ExecSpec;

public class RunServer extends BaseTask {

	public RunServer() {
		setGroup("ff server");
		setDescription("Run a server");
		setDependsOn(Arrays.asList("ff_compile", "jar"));
	}

	private File copyConfiguration(final File server_location, final String dir, final Configuration conf) {
		final File output = new File(server_location, dir);
		if (output.exists()) {
			getProject().delete(output);
		}
		output.mkdirs();

		for (final ResolvedArtifact artifact : conf.getResolvedConfiguration().getResolvedArtifacts()) {
			getProject().copy(new Action<CopySpec>() {
				@Override
				public void execute(final CopySpec copySpec) {
					final ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
					copySpec.from(artifact.getFile());
					copySpec.rename(".+",
							id.getGroup() + "." + id.getName() + "-" + id.getVersion() + "." + artifact.getExtension());
					copySpec.into(output);
				}
			});
		}
		return output;
	}

	@Override
	public void custom_process() throws Exception {
		final Map<String, ?> properties = getProject().getProperties();
		final String location = (String) properties.get("location");

		if (location == null || properties.get("main") == null) {
			throw new NullPointerException("Please configure into gradle.properties, location and main properties");
		}

		final File server_location = new File(location);
		server_location.mkdirs();

		final Configuration server_conf = getProject().getConfigurations().getByName("server");
		final File output = copyConfiguration(server_location, "runtime", server_conf);

		final Jar jar = (Jar) getProject().getTasks().getByName("jar");

		getProject().copy(new Action<CopySpec>() {
			@Override
			public void execute(final CopySpec copySpec) {
				copySpec.from(jar.getArchivePath());
				copySpec.rename(".+", properties.get("project_group") + "." + properties.get("project_name") + "-"
						+ properties.get("project_version") + ".jar");
				copySpec.into(output);
			}
		});

		final Configuration plugin_conf = getProject().getConfigurations().getByName("plugin");
		copyConfiguration(server_location, "www/plugins", plugin_conf);

		final List<URL> classpath_files = new ArrayList<>();
		for (final File f : output.listFiles()) {
			classpath_files.add(f.toURI().toURL());
		}

		try (URLClassLoader loader = new URLClassLoader(classpath_files.toArray(new URL[0]))) {
			final InputStream script_stream = loader.loadClass("ff.mustang.ServiceRunner")
					.getResourceAsStream("/ff/mustang/run_server.py");
			final StringBuilder builder = new StringBuilder();
			final byte[] bs = new byte[1024];
			while (true) {
				final int i = script_stream.read(bs);
				if (i == -1) {
					break;
				}
				builder.append(new String(bs, 0, i));
			}
			script_stream.close();
			final String python = builder.toString().replace("#server#", (String) properties.get("main"))
					.replace("#root#", location);

			final PrintStream stream = new PrintStream(new File(server_location, "run_server.py"));
			stream.print(python);
			stream.close();

			getProject().exec(new Action<ExecSpec>() {
				@Override
				public void execute(final ExecSpec execSpec) {
					execSpec.workingDir(server_location);
					execSpec.executable(getPythonExecutable());
					execSpec.args("run_server.py", "start", "debug", "9192");
				}
			});
		}
	}

	private String getPythonExecutable() {
		String python_exec = System.getenv("FF_PYTHON_HOME");
		if (python_exec == null) {
			final File f = new File("/usr/bin/python3");
			if (f.exists()) {
				python_exec = f.getAbsolutePath();
			} else {
				python_exec = "/usr/bin/python";
				if (!new File(python_exec).exists()) {
					python_exec = "D:\\data\\devtools\\sdk\\anaconda\\python";
				}
			}
		}
		return python_exec;
	}

}
