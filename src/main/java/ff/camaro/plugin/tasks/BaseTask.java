package ff.camaro.plugin.tasks;

import java.util.List;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;

import ff.camaro.CamaroTask;
import ff.camaro.Configurator;

public class BaseTask extends CamaroTask {

	public static void base_setup(final Task baseTask, final Map<String, Object> config,
			final Configurator configurator) {
		final String group = configurator.toString(config.get("group"));
		if (group != null) {
			baseTask.setGroup(group);
		}

		final String description = configurator.toString(config.get("description"));
		if (description != null) {
			baseTask.setDescription(description);
		}

		final List<String> depends = configurator.toList(config.get("depends"));
		baseTask.setDependsOn(depends);

		final String log_end = configurator.toString(config.get("log-end"));
		if (log_end != null) {
			baseTask.doLast(new Action<Task>() {

				@Override
				public void execute(final Task t) {
					System.out.println(log_end);
				}

			});

		}
	}

	protected Map<String, Object> config;

	protected Configurator configurator;

	public BaseTask() {
		return;
	}

	protected void custom_process() throws Exception {
		return;
	}

	@TaskAction
	public void process() throws Exception {
		custom_process();
	}

	public void setup(final Project project, final Map<String, Object> config, final Configurator configurator) {
		BaseTask.base_setup(this, config, configurator);

		this.config = config;
		this.configurator = configurator;
	}
}
