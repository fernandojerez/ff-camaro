package ff.camaro.facet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gradle.api.Project;

import ff.camaro.ConfigLoader;
import ff.camaro.Configurator;
import groovy.json.JsonSlurper;

public abstract class Facet extends Configurator {

	public void apply(final Project project, final Map<String, Object> config) throws IOException {
		System.out.println(getConfiguration());
		setup(project, ConfigLoader.facet.load(project, getConfiguration()));

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
			final Map<String, Object> base = loadJson("/ff/camaro/facet/" + command + ".json");
			config.putAll(base);
		}

		final List<Map<String, String>> files = getList("files");
		for (final Map<String, String> file : files) {
			copyTextFile(file.get("file"), new File(projectDir, file.get("to")), "#project.name#",
					props.getProperty("project_group") + "@" + props.getProperty("project_name"));
		}

		copyTextFile("/ff/camaro/eclipse/project.txt", new File(projectDir, ".project"), "#ProjectName#",
				props.getProperty("project_group") + "@" + props.getProperty("project_name"));
	}

	protected void copyTextFile(final String route, final File output, final Object... args) throws IOException {
		System.out.println(route);
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

	protected abstract String getConfiguration();

	@SuppressWarnings("unchecked")
	protected Map<String, Object> loadJson(final String route) {
		final JsonSlurper sluper = new JsonSlurper();
		return (Map<String, Object>) sluper.parse(getClass().getResource(route));
	}

}
