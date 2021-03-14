package ff.camaro.plugin.tasks.builder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.jvm.tasks.Jar;

import ff.camaro.ArtifactInfo;
import ff.camaro.ConfigLoader;
import ff.camaro.MapStore;

public class JarBuilder extends TaskBuilder<Jar> {

	private String cleanGroup(final String group) {
		if (group.equals("org.ff-lang")) {
			return "ff";
		}
		if (group.startsWith("org.ff-lang.")) {
			return "ff." + group.substring("org.ff-lang.".length());
		}
		return group;
	}

	@Override
	public void configure(final Project project, final String taskName, final Jar jar) {
		final Map<String, Object> from = config.getMap("from");
		for (final Map.Entry<String, Object> entry : from.entrySet()) {
			@SuppressWarnings("unchecked")
			final MapStore f = new MapStore((Map<String, Object>) entry.getValue());
			jar.from("output".equals(f.getString("type")) ? //
					new File(project.getBuildDir(), ConfigLoader.output_main_path(project, entry.getKey()))
							.getAbsolutePath() //
					: ConfigLoader.src_main_path(entry.getKey()), new Action<CopySpec>() {
						@Override
						public void execute(final CopySpec spec) {
							final List<String> include = f.getList("include");
							spec.include(include.toArray(new String[0]));

							final List<String> exclude = f.getList("exclude");
							spec.exclude(exclude.toArray(new String[0]));
						}
					});
		}
		String suffix = config.getString("suffix", "");
		if (suffix != null) {
			suffix = "-" + suffix;
			jar.getArchiveAppendix().set(suffix);
		}
		final String classifier = config.getString("classifier");
		if (classifier != null) {
			jar.getArchiveClassifier().set(classifier);
			jar.getManifest().attributes(getManifestAttributes(project, suffix, "-" + classifier));
		} else {
			jar.getManifest().attributes(getManifestAttributes(project, suffix, ""));
		}

		final String extension = config.getString("extension");
		if (extension != null) {
			jar.getArchiveExtension().set(extension);
		}
	}

	public ArtifactInfo getArtifactInfo(final Project project, final String suffix) {
		final Map<String, ?> properties = project.getProperties();
		return new ArtifactInfo((String) properties.get("project_name"), (String) properties.get("project_group"),
				(String) properties.get("project_version"));
	}

	protected HashMap<String, String> getManifestAttributes(final Project project, final String suffix,
			final String classifier) {
		final ArtifactInfo info = getArtifactInfo(project, suffix);
		final HashMap<String, String> jarAttributes = new HashMap<>();
		jarAttributes.put("Implementation-Title", info.getGroup() + "@" + info.getName() + classifier);
		jarAttributes.put("Implementation-Version", info.getVersion());
		jarAttributes.put("Generated", new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss").format(new Date()));
		jarAttributes.put("Automatic-Module-Name", cleanGroup(info.getGroup()) + "." + info.getName() + classifier);
		return jarAttributes;
	}

	@Override
	public Class<Jar> getTaskClass() {
		return Jar.class;
	}
}
