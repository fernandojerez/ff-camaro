package ff.camaro.plugin.tasks.builder;

import org.gradle.api.Project;

import ff.camaro.plugin.tasks.Browse;

public class BrowseBuilder extends TaskBuilder<Browse> {

	@Override
	public void configure(final Project project, final String taskName, final Browse browse) {
		browse.setUri(config.getString("uri").replace("$build_dir$", project.getBuildDir().getAbsolutePath()));
	}

	@Override
	public Class<Browse> getTaskClass() {
		return Browse.class;
	}

}
