package ff.camaro.plugin;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.reflect.Instantiator;

/**
 * @author fernandojerez
 */
public class FFJavaPlugin extends CamaroPlugin {

	@Inject
	public FFJavaPlugin(final ObjectFactory objectFactory, final Instantiator instantiator) {
		super(objectFactory, instantiator);
	}

	@Override
	public String getConfiguration() {
		return "java";
	}

}
