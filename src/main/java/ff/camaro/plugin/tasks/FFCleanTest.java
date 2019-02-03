package ff.camaro.plugin.tasks;

import java.io.File;

import org.gradle.api.tasks.TaskAction;

import ff.camaro.CamaroTask;

public class FFCleanTest extends CamaroTask {

	@TaskAction
	public void compile() throws Exception {
		getProject().delete(new File(getProject().getBuildDir(), "test-results"));
		getProject().delete(new File(getProject().getBuildDir(), "reports/tests"));
		getProject().delete(new File(getProject().getBuildDir(), "reports/jacoco"));
		getProject().delete(new File(getProject().getBuildDir(), "jacoco"));
	}

}
