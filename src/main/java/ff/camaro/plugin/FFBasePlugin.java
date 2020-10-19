package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;

public class FFBasePlugin extends CamaroPlugin {

	@Inject
	public FFBasePlugin(final ObjectFactory objectFactory) {
		super(objectFactory);
	}

	@Override
	public String getConfiguration() {
		return "base";
	}

}
