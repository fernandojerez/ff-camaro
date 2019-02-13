package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;

public class FFMustangPlugin extends CamaroPlugin {

	@Inject
	public FFMustangPlugin(final ObjectFactory objectFactory) {
		super(objectFactory);
	}

	@Override
	public String getConfiguration() {
		return "mustang";
	}

}
