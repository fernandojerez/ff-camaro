package ff.camaro.artifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

@SuppressWarnings("deprecation")
public class ModelResolverImpl implements ModelResolver {

	private final String taskName;
	private final Project project;
	private final ModelResolveListener listener;
	private final AtomicInteger configurationCount = new AtomicInteger(0);

	public ModelResolverImpl(final String taskName, final Project project, final ModelResolveListener listener) {
		super();
		this.taskName = taskName;
		this.project = project;
		this.listener = listener;
	}

	@Override
	public void addRepository(final Repository arg0) {
		return;
	}

	@Override
	public void addRepository(final Repository repository, final boolean replace) {
		return;
	}

	@Override
	public ModelResolver newCopy() {
		return this;
	}

	@Override
	public ModelSource resolveModel(final Dependency dependency) {
		return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
	}

	@Override
	public ModelSource resolveModel(final Parent parent) {
		return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
	}

	@Override
	public ModelSource resolveModel(final String groupId, final String artifactId, final String version) {
		final String configName = String.format("%s%s", taskName, configurationCount.getAndIncrement());
		final Configuration config = project.getConfigurations().create(configName);
		config.setTransitive(false);
		final String depNotation = String.format("%s:%s:%s@pom", groupId, artifactId, version);
		final org.gradle.api.artifacts.Dependency dependency = project.getDependencies().create(depNotation);
		config.getDependencies().add(dependency);

		final File pomXml = config.getSingleFile();
		listener.onResolveModel(groupId, artifactId, version, pomXml);
		return new ModelSource() {
			@Override
			public InputStream getInputStream() throws IOException {
				return new FileInputStream(pomXml);
			}

			@Override
			public String getLocation() {
				return pomXml.getAbsolutePath();
			}
		};
	}

}
