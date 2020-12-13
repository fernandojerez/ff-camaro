package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.model.ObjectFactory;

public class FFMustangPlugin extends CamaroPlugin {

	@Inject
	public FFMustangPlugin(final ObjectFactory objectFactory, final SoftwareComponentFactory componentFactory) {
		super(objectFactory, componentFactory);
	}

	@Override
	public String getConfiguration() {
		return "mustang";
	}

}
