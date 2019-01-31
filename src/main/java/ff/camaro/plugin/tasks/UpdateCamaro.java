package ff.camaro.plugin.tasks;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import ff.camaro.Configurator;
import ff.camaro.facet.Facet;
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

	@TaskAction
	public void run() throws Exception {
		final Map<String, Object> base_map = new HashMap<>();
		final File output = new File(getProject().getProjectDir(), "camaro.json");
		if (output.exists()) {
			final JsonSlurper sluper = new JsonSlurper();
			@SuppressWarnings("unchecked")
			final Map<String, Object> result = (Map<String, Object>) sluper.parse(output);
			result.remove("menu");
			base_map.putAll(result);
		}

		for (final String facet : facets) {
			final Facet f = (Facet) Class.forName("ff.camaro.facet." + Configurator.capitalize(facet) + "Facet")
					.getConstructor().newInstance();
			f.apply(getProject(), base_map);
		}
		final String config = JsonOutput.toJson(base_map);
		final PrintStream stream = new PrintStream(output);
		stream.print(config);
		stream.close();
	}

	public void setFacets(final List<String> facet) {
		facets = facet;
	}

}
