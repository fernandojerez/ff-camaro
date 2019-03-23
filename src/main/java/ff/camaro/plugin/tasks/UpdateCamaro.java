package ff.camaro.plugin.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.snakeyaml.engine.v1.api.Dump;
import org.snakeyaml.engine.v1.api.DumpSettings;
import org.snakeyaml.engine.v1.api.DumpSettingsBuilder;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettings;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;
import org.snakeyaml.engine.v1.common.FlowStyle;
import org.snakeyaml.engine.v1.common.ScalarStyle;

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

	@SuppressWarnings("unchecked")
	@TaskAction
	public void run() throws Exception {
		final Map<String, Object> base_map = new HashMap<>();
		final Map<String, Object> yml_map = new HashMap<>();
		final File output = new File(getProject().getProjectDir(), "camaro.json");
		final File yml_output = new File(getProject().getProjectDir(), "kitt.yml");
		if (output.exists()) {
			final JsonSlurper sluper = new JsonSlurper();
			final Map<String, Object> result = (Map<String, Object>) sluper.parse(output);
			result.remove("menu");
			base_map.putAll(result);
		}

		if (yml_output.exists()) {
			final LoadSettings settings = new LoadSettingsBuilder().setLabel("KITT").build();
			final Load load = new Load(settings);
			Map<String, Object> cfg = null;
			try (InputStream in = new FileInputStream(yml_output)) {
				cfg = (Map<String, Object>) load.loadFromInputStream(in);
			}
			if (cfg != null) {
				yml_map.putAll(cfg);
			}
		}

		for (final String facet : facets) {
			final Facet f = (Facet) Class.forName("ff.camaro.facet." + Configurator.capitalize(facet) + "Facet")
					.getConstructor().newInstance();
			f.apply(getProject(), base_map, yml_map);
		}
		final String config = JsonOutput.toJson(base_map);
		final PrintStream stream = new PrintStream(output);
		stream.print(config);
		stream.close();

		final DumpSettings settings = new DumpSettingsBuilder()//
				.setDefaultScalarStyle(ScalarStyle.PLAIN)//
				.setCanonical(false) //
				.setDefaultFlowStyle(FlowStyle.BLOCK) //
				.setMultiLineFlow(true) //
				.build();
		final Dump dump = new Dump(settings);
		final PrintStream yml_stream = new PrintStream(yml_output);
		yml_stream.print(dump.dumpToString(yml_map));
		yml_stream.close();

	}

	public void setFacets(final List<String> facet) {
		facets = facet;
	}

}
