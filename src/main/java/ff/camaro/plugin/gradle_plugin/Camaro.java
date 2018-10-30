package ff.camaro.plugin.gradle_plugin;

import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;

import ff.camaro.Configurator;
import ff.camaro.plugin.tasks.UpdateCamaro;

public class Camaro implements GradlePlugin {

	@Override
	public void apply(final Project prj, final Configurator configurator) {
		prj.getTasks().create(UpdateCamaro.TASK_NAME, UpdateCamaro.class, new Action<UpdateCamaro>() {

			@Override
			public void execute(final UpdateCamaro task) {
				task.setFacets(configurator.getList("facets"));
			}

		});
		prj.afterEvaluate(new Action<Project>() {
			@Override
			public void execute(final Project p) {
				hide_task(prj);
			}

			private void hide_task(final Project project) {
				final Map<Project, Set<Task>> tasks = project.getAllTasks(true);
				for (final Set<Task> st : tasks.values()) {
					for (final Task t : st) {
						if (t.getGroup() != null && hideGroup(t)) {
							t.setGroup(null);
						}
					}
				}
			}
		});
	}

	private boolean hideGroup(final Task t) {
		return false;
		// return !t.getGroup().equals("ff");
	}
}
