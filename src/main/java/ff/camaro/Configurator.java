package ff.camaro;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;

public class Configurator extends Store {

	protected Map<String, Object> config;
	protected Project project;

	public ArtifactInfo getArtifactInfo() {
		final Map<String, ?> properties = project.getProperties();
		return new ArtifactInfo((String) properties.get("project_name"), (String) properties.get("project_group"),
				(String) properties.get("project_version"));
	}

	public HashMap<String, String> getManifestAttributes(final String suffix) {
		final ArtifactInfo info = getArtifactInfo();
		final HashMap<String, String> jarAttributes = new HashMap<>();
		jarAttributes.put("Implementation-Title", info.getGroup() + "@" + info.getName() + suffix);
		jarAttributes.put("Implementation-Version", info.getVersion());
		jarAttributes.put("Generated", new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss").format(new Date()));
		return jarAttributes;
	}

	@Override
	public Object getValue(final String key) {
		return config.get(key);
	}

	public void init(final Project project, final Map<String, Object> config) {
		this.config = config;
		this.project = project;
	}

	public boolean test(final Object object) {
		if (object == null) {
			return false;
		}
		return "true".equals(String.valueOf(object));
	}

	@Override
	public String toString() {
		return ConfigLoader.plugin.toYaml(config);
	}

}
