package ff.camaro.plugin.tasks;

import java.awt.Desktop;
import java.net.URI;

public class Browse extends BaseTask {

	public String uri;

	@Override
	public void execute() throws Exception {
		Desktop.getDesktop().browse(URI.create(uri.replace("\\", "/")));
	}

	public void setUri(final String uri) {
		this.uri = uri;
	}

}
