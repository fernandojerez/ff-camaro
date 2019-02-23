package ff.camaro.plugin.tasks.builder;

import org.gradle.api.Action;
import org.gradle.api.Project;

import ff.camaro.plugin.tasks.BaseTask;
import ff.camaro.plugin.tasks.Browse;

public class BrowseBuilder extends TaskBuilder {

	@Override
	public void define(final Project project, final String taskName) {
		project.getTasks().create(taskName, Browse.class, new Action<Browse>() {

			@Override
			public void execute(final Browse browse) {
				BaseTask.base_setup(browse, getDefinition(), getConfiguration());
				browse.setUri(getString("uri").replace("$build_dir$", project.getBuildDir().getAbsolutePath()));
			}

		});

	}

}
