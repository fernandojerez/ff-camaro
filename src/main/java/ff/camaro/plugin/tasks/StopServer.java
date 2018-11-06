package ff.camaro.plugin.tasks;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.process.ExecSpec;

public class StopServer extends BaseTask {

	public StopServer() {
		setGroup("ff server");
		setDescription("Stop a server");
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
		final List<URL> classpath_files = new ArrayList<>();
		for (final ResolvedArtifact artifact : server_conf.getResolvedConfiguration().getResolvedArtifacts()) {
			classpath_files.add(artifact.getFile().toURI().toURL());
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
					execSpec.args("run_server.py", "stop");
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
