package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.reflect.Instantiator;

public class FFMultiPlugin extends CamaroPlugin {

	@Inject
	public FFMultiPlugin(final ObjectFactory objectFactory, final Instantiator instantiator) {
		super(objectFactory, instantiator);
	}

	@Override
	public String getConfiguration() {
		return "multi";
	}

}
