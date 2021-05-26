package ff.camaro;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Internal;

public abstract class CamaroTask extends DefaultTask {

	protected void fillClassPath(final Set<URL> urls, final String name) throws MalformedURLException {
		final Configuration conf = getProject().getConfigurations().getByName(name);
		for (final File file : conf.resolve()) {
			urls.add(file.toURI().toURL());
		}
	}

	@Internal
	protected URLClassLoader getClassLoader() throws MalformedURLException {
		final Set<URL> urls = new HashSet<>();
		fillClassPath(urls, "camaro");
		return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
	}

}
