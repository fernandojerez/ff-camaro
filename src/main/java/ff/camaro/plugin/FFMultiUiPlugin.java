package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;

public class FFMultiUiPlugin extends CamaroPlugin {

	@Inject
	public FFMultiUiPlugin(final ObjectFactory objectFactory) {
		super(objectFactory);
	}

	@Override
	public String getConfiguration() {
		return "multi_ui";
	}

}
