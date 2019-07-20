package ff.camaro.artifact;

import java.util.Arrays;

public class Artifact {

	private final String name;
	private final String org;
	private final String version;
	private final boolean transitive;
	private final String classifier;
	private final String cfg;

	public Artifact(final String name, final String org, final String version, final boolean transitive,
			final String classifier, final String cfg) {
		super();
		this.name = name;
		this.org = org;
		this.version = version;
		this.transitive = transitive;
		this.classifier = classifier;
		this.cfg = cfg;
	}

	public String getCfg() {
		return cfg;
	}

	public Iterable<String> getClassifier() {
		if (classifier == null || classifier.length() == 0) {
			return null;
		}
		return Arrays.asList(classifier.split(","));
	}

	public String getName() {
		return name;
	}

	public String getOrg() {
		return org;
	}

	public boolean getTransitive() {
		return transitive;
	}

	public String getVersion() {
		return version;
	}

	public boolean isTransitive() {
		return transitive;
	}

	@Override
	public String toString() {
		return (cfg != null ? cfg + "|" : "") + (transitive ? "" : "-") + org + "@" + name
				+ (classifier != null && classifier.length() > 0 ? "#" + classifier : "") + ";" + version;
	}

}
