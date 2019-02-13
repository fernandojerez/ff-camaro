package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;

public class FFDartPlugin extends CamaroPlugin {

	@Inject
	public FFDartPlugin(final ObjectFactory objectFactory) {
		super(objectFactory);
	}

	@Override
	public String getConfiguration() {
		return "dart";
	}
}
