package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;

public class FFMultiChargerPlugin extends CamaroPlugin {

	@Inject
	public FFMultiChargerPlugin(final ObjectFactory objectFactory) {
		super(objectFactory);
	}

	@Override
	public String getConfiguration() {
		return "multi_charger";
	}

}
