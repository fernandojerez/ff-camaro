package ff.camaro.plugin.tasks;

import java.io.File;
import java.io.FileFilter;

import org.gradle.api.tasks.TaskAction;

import ff.camaro.CamaroTask;

public class FFCleanTest extends CamaroTask {

	@TaskAction
	public void compile() throws Exception {
		getProject().delete(new File(getProject().getBuildDir(), "test-results"));
		getProject().delete(new File(getProject().getBuildDir(), "reports/tests"));
		getProject().delete(new File(getProject().getBuildDir(), "reports/jacoco"));
		getProject().delete(new File(getProject().getBuildDir(), "jacoco"));

		File dir = new File(getProject().getBuildDir(), "analized_test");
		if (dir.exists()) {
			for (final File folder : dir.listFiles(new FileFilter() {

				@Override
				public boolean accept(final File pathname) {
					return pathname.exists();
				}
			})) {
				getProject().delete(folder);
			}
		}

		dir = new File(getProject().getBuildDir(), "macros_test");
		if (dir.exists()) {
			for (final File folder : dir.listFiles(new FileFilter() {

				@Override
				public boolean accept(final File pathname) {
					return pathname.exists();
				}
			})) {
				getProject().delete(folder);
			}
		}

		dir = new File(getProject().getBuildDir(), "classes");
		if (dir.exists()) {
			for (final File folder : dir.listFiles(new FileFilter() {

				@Override
				public boolean accept(final File pathname) {
					return pathname.exists() && pathname.getName().startsWith("ff_");
				}
			})) {
				getProject().delete(new File(folder, "test"));
			}
		}
	}

}
