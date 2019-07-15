package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;

public class FFMultiUiLibPlugin extends CamaroPlugin {

	@Inject
	public FFMultiUiLibPlugin(final ObjectFactory objectFactory) {
		super(objectFactory);
	}

	@Override
	public String getConfiguration() {
		return "multi_ui_lib";
	}

}
