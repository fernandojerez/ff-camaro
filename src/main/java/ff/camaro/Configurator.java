package ff.camaro;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;

public class Configurator {

	public static String capitalize(final String value) {
		if (value.length() == 0) {
			return "";
		}
		if (value.length() == 1) {
			return value.toUpperCase();
		}
		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

	protected Map<String, Object> config;

	protected Project project;

	@SuppressWarnings("unchecked")
	public <T> T cast(final Object value) {
		return (T) value;
	}

	public ArtifactInfo getArtifactInfo() {
		final Map<String, ?> properties = project.getProperties();
		return new ArtifactInfo((String) properties.get("project_name"), (String) properties.get("project_group"),
				(String) properties.get("project_version"));
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getList(final Map<String, Object> config, final String key) {
		final Object result = config.get(key);
		if (result == null) {
			return Collections.emptyList();
		}
		if (result instanceof List) {
			return (List<T>) result;
		}
		return Collections.singletonList((T) result);
	}

	public <T> List<T> getList(final String key) {
		return getList(config, key);
	}

	public HashMap<String, String> getManifestAttributes(final String suffix) {
		final ArtifactInfo info = getArtifactInfo();
		final HashMap<String, String> jarAttributes = new HashMap<>();
		jarAttributes.put("Implementation-Title", info.getGroup() + "@" + info.getName() + suffix);
		jarAttributes.put("Implementation-Version", info.getVersion());
		jarAttributes.put("Generated", new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss").format(new Date()));
		return jarAttributes;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getMap(final Map<String, Object> config, final String key) {
		final Object result = config.get(key);
		if (result == null) {
			return Collections.emptyMap();
		}
		return (Map<String, Object>) result;
	}

	public Map<String, Object> getMap(final String key) {
		return getMap(config, key);
	}

	@SuppressWarnings("unchecked")
	public <T> T loadClass(final String pck, final String name) throws Exception {
		return (T) Class.forName(pck + "." + Configurator.capitalize(name)).getConstructor().newInstance();
	}

	public void setup(final Project project, final Map<String, Object> config) {
		this.config = config;
		this.project = project;
	}

	public boolean test(final Object object) {
		if (object == null) {
			return false;
		}
		return "true".equals(String.valueOf(object));
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> toList(final Object value) {
		if (value == null) {
			return Collections.emptyList();
		}
		if (value instanceof List) {
			return (List<T>) value;
		}
		return Collections.singletonList((T) value);
	}

	@Override
	public String toString() {
		return ConfigLoader.plugin.toYaml(config);
	}

	public String toString(final Object value) {
		if (value == null) {
			return null;
		}
		return String.valueOf(value);
	}
}
