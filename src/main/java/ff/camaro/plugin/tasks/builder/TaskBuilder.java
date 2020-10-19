package ff.camaro.plugin.tasks.builder;

import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;

import ff.camaro.Configurator;
import ff.camaro.MapStore;
import ff.camaro.plugin.tasks.BaseTask;

public abstract class TaskBuilder<T extends Task> {

	public static final void init_task(final MapStore config, final Task task) {
		final String group = config.getString("group");
		if (group != null) {
			task.setGroup(group);
		}

		final String description = config.getString("description");
		if (description != null) {
			task.setDescription(description);
		}

		final List<String> depends = config.getList("depends");
		task.setDependsOn(depends);

		final String log_end = config.getString("log-end");
		if (log_end != null) {
			task.doLast(new Action<Task>() {

				@Override
				public void execute(final Task t) {
					System.out.println(log_end);
				}

			});
		}
	}

	protected MapStore config;

	protected Configurator configurator;

	public abstract void configure(Project project, String taskName, T task);

	public boolean create() {
		return true;
	}

	public void define(final Project project, final String taskName) {
		final TaskContainer tasks = project.getTasks();
		if (!create()) {
			@SuppressWarnings("unchecked")
			final T task = (T) tasks.findByName(taskName);
			doConfigure(project, taskName, task);
		} else {
			project.getTasks().create(taskName, getTaskClass(), new Action<T>() {

				@Override
				public void execute(final T task) {
					doConfigure(project, taskName, task);
				}

			});
		}
	}

	private void doConfigure(final Project project, final String taskName, final T task) {
		if (task instanceof BaseTask) {
			((BaseTask) task).init(config, configurator);
		} else {
			TaskBuilder.init_task(config, task);
		}
		configure(project, taskName, task);
	}

	public abstract Class<T> getTaskClass();

	public void init(final MapStore config, final Configurator configurator) {
		this.config = config;
		this.configurator = configurator;
	}
}
