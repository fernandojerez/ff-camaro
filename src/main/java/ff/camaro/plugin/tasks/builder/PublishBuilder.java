package ff.camaro.plugin.tasks.builder;

import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.maven.MavenDeployer;
import org.gradle.api.artifacts.maven.MavenPom;
import org.gradle.api.internal.HasConvention;
import org.gradle.api.plugins.MavenRepositoryHandlerConvention;
import org.gradle.api.tasks.Upload;

import ff.camaro.ArtifactInfo;
import ff.camaro.Configurator;
import ff.camaro.Util;

public class PublishBuilder extends TaskBuilder {

	public static void add_publishers(final Upload upload, final Configurator config, final Map<String, Object> map) {
		for (final Map.Entry<String, Object> entry : map.entrySet()) {
			final String repo = config.toString(entry.getValue());
			if (repo.startsWith("$local")) {
				PublishBuilder.addFFLocalDeployer(config.getArtifactInfo(), upload,
						repo.substring("$local".length()).trim());
			} else {
				throw new UnsupportedOperationException("repo not supported: " + repo);
			}
		}
	}

	public static void addFFLocalDeployer(final ArtifactInfo info, final Upload upload, final String suffix) {
		final RepositoryHandler repo = upload.getRepositories();

		final HasConvention convention = (HasConvention) repo;
		final MavenRepositoryHandlerConvention maven = (MavenRepositoryHandlerConvention) convention.getConvention()
				.getPlugins().get("maven");

		final MavenDeployer deployer = maven.mavenDeployer();
		try {
			System.out.println("Publicando a " + Util.getFFRepo());
			deployer.setRepository(deployer.getClass().getMethod("repository", Map.class)//
					.invoke(deployer, Util.map("url", Util.getFFRepo())));
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		final MavenPom pom = deployer.getPom();
		pom.setGroupId(info.getGroup());
		pom.setArtifactId(info.getName() + suffix);
		pom.setVersion(info.getVersion());
		repo.add(deployer);
	}

	@Override
	public void define(final Project project, final String taskName) {
		project.getTasks().create(taskName, Upload.class, new Action<Upload>() {

			@Override
			public void execute(final Upload upload) {
				upload.setConfiguration(project.getConfigurations().getByName(getString("configuration")));
				PublishBuilder.add_publishers(upload, getConfiguration(), getMap("publish_to"));
			}

		});
	}
}
