package ff.camaro;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

public abstract class CamaroTask extends DefaultTask {

    protected URLClassLoader getClassLoader(Set<URL> seed) throws MalformedURLException {
        final Set<URL> urls = seed;
        fillClassPath(urls, "camaro");
        return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }

    protected void fillClassPath(Set<URL> urls, String name) throws MalformedURLException {
        final Configuration conf = getProject().getConfigurations().getByName(name);
        for (final File file : conf.getFiles()) {
            urls.add(file.toURI().toURL());
        }
    }

}
