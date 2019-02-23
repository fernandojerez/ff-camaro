package ff.camaro.plugin.tasks;

import java.awt.Desktop;
import java.net.URI;

import org.gradle.api.tasks.TaskAction;

import ff.camaro.CamaroTask;

public class Browse extends CamaroTask {

	public String uri;

	public Browse() {
	}

	@TaskAction
	public void perform() throws Exception {
		Desktop.getDesktop().browse(URI.create(uri.replace("\\", "/")));
	}

	public void setUri(final String uri) {
		this.uri = uri;
	}

}
