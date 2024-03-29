package ff.camaro.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import ff.camaro.ConfigLoader;
import ff.camaro.Configurator;
import groovy.json.JsonSlurper;

public abstract class Files extends Configurator {

	public void apply(final Project project, final Map<String, Object> config, final Map<String, Object> kitt)
			throws IOException {
		init(project, ConfigLoader.files.load(project, getConfiguration()));

		final File projectDir = project.getProjectDir();
		final Properties props = new Properties();
		props.load(new FileInputStream(new File(projectDir, "gradle.properties")));

		final File notes = new File(projectDir,
				props.getProperty("project_group") + "@" + props.getProperty("project_name") + ".txt");
		if (!notes.exists()) {
			try (PrintStream stream = new PrintStream(notes)) {
				stream.println("# Notes");
			}
		}

		final List<String> commands = getList("commands");
		for (final String command : commands) {
			final Map<String, Object> base = loadJson("/ff/camaro/files/" + command + ".json");
			merge_two_maps(config, base);

			final Map<String, Object> yml = loadYml("/ff/camaro/files/kitt/" + command + ".yml");
			if (yml != null) {
				merge_two_maps(kitt, yml);
			}
		}

		String gradleVersion = System.getenv("FF_GRADLE_VERSION");
		if (gradleVersion == null) {
			gradleVersion = get_gradle_version();
		}
		final List<Map<String, String>> files = getList("files");
		for (final Map<String, String> file : files) {
			copyTextFile(file.get("file"), new File(projectDir, file.get("to")), "#project.name#",
					props.getProperty("project_group") + "@" + props.getProperty("project_name"), "#FF_JAVA_HOME#",
					System.getenv("FF_JAVA_HOME").replace("\\", "/"), "#GRADLE_VERSION#", gradleVersion);
		}
	}

	protected void copyTextFile(final String route, final File output, final Object... args) throws IOException {
		try (InputStream input = getClass().getResourceAsStream(route)) {
			final InputStreamReader reader = new InputStreamReader(input);
			final StringBuilder result = new StringBuilder();
			final char[] cs = new char[1024];
			while (true) {
				final int i = reader.read(cs);
				if (i == -1) {
					break;
				}
				result.append(cs, 0, i);
			}
			String strResult = result.toString();
			for (int i = 0; i < args.length; i += 2) {
				strResult = strResult.replace(String.valueOf(args[i]), String.valueOf(args[i + 1]));
			}
			output.getParentFile().mkdirs();
			try (PrintStream stream = new PrintStream(output)) {
				stream.print(strResult);
				stream.close();
			}
		}
	}

	private String get_gradle_version() {
		try {
			final Configuration kitt_conf = project.getConfigurations().getByName("kitt");
			final Set<File> files = kitt_conf.resolve();
			final Set<URL> urls = new HashSet<>();
			for (final File f : files) {
				urls.add(f.toURI().toURL());
			}
			try (final URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]))) {
				final Class<?> module = loader.loadClass("ff.kitt.gradle");
				final Method method = module.getMethod("version");
				return (String) method.invoke(null);
			}
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected abstract String getConfiguration();

	@SuppressWarnings("unchecked")
	protected Map<String, Object> loadJson(final String route) {
		final JsonSlurper sluper = new JsonSlurper();
		return (Map<String, Object>) sluper.parse(getClass().getResource(route));
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> loadYml(final String route) throws IOException {
		try {
			final LoadSettings settings = LoadSettings.builder().setLabel("KITT").build();
			final Load load = new Load(settings);
			Map<String, Object> cfg = null;
			try (InputStream in = getClass().getResourceAsStream(route)) {
				cfg = (Map<String, Object>) load.loadFromInputStream(in);
			}
			return cfg;
		} catch (final Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	protected void merge_two_maps(final Map<String, Object> config, final Map<String, Object> base) {
		for (final Map.Entry<String, Object> entry : base.entrySet()) {
			final Object result = config.get(entry.getKey());
			if (result == null) {
				config.put(entry.getKey(), entry.getValue());
				continue;
			}
			if (result instanceof Map) {
				merge_two_maps((Map<String, Object>) result, (Map<String, Object>) entry.getValue());
				continue;
			}
			config.put(entry.getKey(), entry.getValue());
		}
	}

}
