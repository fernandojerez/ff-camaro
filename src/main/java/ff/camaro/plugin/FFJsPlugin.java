package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;

public class FFJsPlugin extends CamaroPlugin {

	@Inject
	public FFJsPlugin(final ObjectFactory objectFactory) {
		super(objectFactory);
	}

	@Override
	public String getConfiguration() {
		return "js";
	}
}
