package ff.camaro.plugin.tasks;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import ff.camaro.Configurator;
import ff.camaro.facet.Facet;
import ff.camaro.plugin.CamaroMetadata;
import ff.camaro.plugin.CamaroPlugin;
import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;

public class UpdateCamaro extends DefaultTask {

	public static final String TASK_LABEL = "Update Camaro";
	public static final String TASK_NAME = "update_camaro";

	private List<String> facets;

	public UpdateCamaro() {
		setGroup("camaro");
		setDescription(UpdateCamaro.TASK_LABEL);
	}

	@SuppressWarnings("unchecked")
	@TaskAction
	public void run() throws Exception {
		final Map<String, Object> base_map = new HashMap<>();
		final File output = new File(getProject().getProjectDir(), "camaro.json");
		final File camaro_build = new File(getProject().getProjectDir(), "camaro.build.json");
		if (output.exists()) {
			final JsonSlurper sluper = new JsonSlurper();
			final Map<String, Object> result = (Map<String, Object>) sluper.parse(output);
			result.remove("menu");
			base_map.putAll(result);
		}
		final Map<String, Object> camaro_build_map = CamaroPlugin.camaro_build(getProject());
		for (final String facet : facets) {
			final Facet f = (Facet) Class.forName("ff.camaro.facet." + Configurator.capitalize(facet) + "Facet")
					.getConstructor().newInstance();
			f.apply(getProject(), base_map, (Map<String, Object>) camaro_build_map.get("kitt"));
		}
		final CamaroMetadata metadata = CamaroPlugin.metadata(getProject());

		final List<Object> languages = (List<Object>) camaro_build_map.get("languages");
		final Set<Object> languages_set = new HashSet<>(languages);
		final Map<String, Object> dependencies = (Map<String, Object>) camaro_build_map.get("dependencies");
		for (final String name : metadata.getLanguages()) {
			if (languages_set.contains("-" + name)) {
				continue;
			}
			if (!languages_set.contains(name)) {
				languages.add(name);
			}
			if (!dependencies.containsKey(name)) {
				dependencies.put(name, new ArrayList<>());
			}
			if (metadata.getConfigurations().contains(name + "_test")) {
				if (!dependencies.containsKey(name + "_test")) {
					dependencies.put(name + "_test", new ArrayList<>());
				}
			}
			if (metadata.getConfigurations().contains(name + "_libs")) {
				if (!dependencies.containsKey(name + "_libs")) {
					dependencies.put(name + "_libs", new ArrayList<>());
				}
			}
		}
		try (final PrintStream stream = new PrintStream(output)) {
			stream.print(JsonOutput.prettyPrint(JsonOutput.toJson(base_map)));
		}
		try (final PrintStream camaro_stream = new PrintStream(camaro_build)) {
			camaro_stream.print(JsonOutput.prettyPrint(JsonOutput.toJson(camaro_build_map)));
		}
	}

	public void setFacets(final List<String> facet) {
		facets = facet;
	}

}
