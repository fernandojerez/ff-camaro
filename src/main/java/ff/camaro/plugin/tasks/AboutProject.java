package ff.camaro.plugin.tasks;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class AboutProject extends BaseTask {

	public static final String TASK_LABEL = "About Project";
	public static final String TASK_NAME = "about_project";

	private final static String startPrefix = "\u001b[38;5;226m\u001b[1m";
	private final static String endPrefix = "\u001b[0m";
	private final static String bold = "\u001b[1m";

	private List<String> languages;

	public AboutProject() {
		setGroup("camaro");
		setDescription(AboutProject.TASK_LABEL);
	}

	@Override
	protected void execute() throws Exception {
		final var buildDir = getProject().getBuildDir();
		final var info = new StringWriter();
		final var out = new PrintWriter(info);
		out.println(title("## Base Information"));
		printString(out, "Organisation", getProject().getProperties().get("project_group"));
		printString(out, "Name", getProject().getProperties().get("project_name"));
		printString(out, "Version", getProject().getProperties().get("project_version"));
		printFile(out, "Project Directory", getProject().getProjectDir());
		printFile(out, "Project Build Directory", buildDir);
		out.println();
		out.println(title("## Analyzed code"));
		printOutputDirs(languages, out, new File(buildDir, "analyzed"));
		out.println();
		out.println(title("## Analyzed test"));
		printOutputDirs(languages, out, new File(buildDir, "analyzed_test"));
		out.println();
		out.println(title("## Macros generated code"));
		printOutputDirs(languages, out, new File(buildDir, "macros"));
		out.println();
		out.println(title("## Macros test generated code"));
		printOutputDirs(languages, out, new File(buildDir, "macros_test"));
		out.println();
		System.out.print(info.toString());
	}

	private void printFile(final PrintWriter out, final String label, final File outputDir) {
		out.println(AboutProject.bold + label + ": " + AboutProject.endPrefix + outputDir);
	}

	private void printOutputDirs(final List<String> languages, final PrintWriter out, final File outputRootDir) {
		for (final String str : languages) {
			if (str.startsWith("-")) {
				continue;
			}
			var outputDir = new File(outputRootDir, "ff_" + str);
			if ("macros".equals(str)) {
				outputDir = new File(outputRootDir, str);
			}
			printFile(out, str, outputDir);
		}
	}

	private void printString(final PrintWriter out, final String label, final Object value) {
		out.println(AboutProject.bold + label + ": " + AboutProject.endPrefix + value);
	}

	public void setLanguages(final List<String> languages) {
		this.languages = languages;
	}

	private String title(final String title) {
		final var str = new StringBuilder();
		str.append(AboutProject.startPrefix).append(title).append(AboutProject.endPrefix);
		return str.toString();
	}

}
