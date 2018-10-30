package ff.camaro.plugin.tasks.builder;

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
import ff.camaro.plugin.tasks.BaseTask;

public class JarBuilder extends TaskBuilder {

	public boolean create() {
		return true;
	}

	@Override
	public void define(final Project project, final String taskName) {
		final Action<Jar> action = new Action<Jar>() {

			@Override
			public void execute(final Jar jar) {
				BaseTask.base_setup(jar, getDefinition(), getConfiguration());
				final Map<String, Object> from = getMap("from");
				for (final Map.Entry<String, Object> entry : from.entrySet()) {
					@SuppressWarnings("unchecked")
					final Map<String, Object> f = (Map<String, Object>) entry.getValue();
					jar.from("output".equals(f.get("type")) ? //
					ConfigLoader.output_main_path(entry.getKey()) //
							: ConfigLoader.src_main_path(entry.getKey()), new Action<CopySpec>() {
								@Override
								public void execute(final CopySpec spec) {
									final List<String> include = getList(f, "include");
									spec.include(include.toArray(new String[0]));
								}
							});
				}
				final String classifier = getString("classifier");
				if (classifier != null) {
					jar.setClassifier(classifier);
					jar.getManifest().attributes(getManifestAttributes(project, "-" + classifier));
				} else {
					jar.getManifest().attributes(getManifestAttributes(project, ""));
				}

				final String extension = getString("extension");
				if (extension != null) {
					jar.setExtension(extension);
				}

			}

		};
		if (!create()) {
			action.execute((Jar) project.getTasks().findByName(taskName));
		} else {
			project.getTasks().create(taskName, Jar.class, action);
		}
	}

	public ArtifactInfo getArtifactInfo(final Project project) {
		final Map<String, ?> properties = project.getProperties();
		return new ArtifactInfo((String) properties.get("project_name"), (String) properties.get("project_group"),
				(String) properties.get("project_version"));
	}

	protected HashMap<String, String> getManifestAttributes(final Project project, final String suffix) {
		final ArtifactInfo info = getArtifactInfo(project);
		final HashMap<String, String> jarAttributes = new HashMap<>();
		jarAttributes.put("Implementation-Title", info.getGroup() + "@" + info.getName() + suffix);
		jarAttributes.put("Implementation-Version", info.getVersion());
		jarAttributes.put("Generated", new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss").format(new Date()));
		return jarAttributes;
	}
}
