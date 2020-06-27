package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;

public class FFMultiMustangPlugin extends CamaroPlugin {

	@Inject
	public FFMultiMustangPlugin(final ObjectFactory objectFactory) {
		super(objectFactory);
	}

	@Override
	public String getConfiguration() {
		return "multi_mustang";
	}

}
