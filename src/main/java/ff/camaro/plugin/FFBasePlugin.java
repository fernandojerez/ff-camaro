package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.reflect.Instantiator;

public class FFBasePlugin extends CamaroPlugin {

	@Inject
	public FFBasePlugin(final ObjectFactory objectFactory, final Instantiator instantiator) {
		super(objectFactory, instantiator);
	}

	@Override
	public String getConfiguration() {
		return "base_camaro";
	}

}
