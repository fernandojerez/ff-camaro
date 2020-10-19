package ff.camaro.plugin.tasks;

import org.gradle.api.tasks.TaskAction;

import ff.camaro.CamaroTask;
import ff.camaro.Configurator;
import ff.camaro.MapStore;
import ff.camaro.plugin.tasks.builder.TaskBuilder;

public abstract class BaseTask extends CamaroTask {

	protected MapStore config;
	protected Configurator configurator;

	public BaseTask() {
		return;
	}

	protected abstract void execute() throws Exception;

	public void init(final MapStore config, final Configurator configurator) {
		TaskBuilder.init_task(config, this);
		this.config = config;
		this.configurator = configurator;
	}

	@TaskAction
	public void process() throws Exception {
		execute();
	}
}
