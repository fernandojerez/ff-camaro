package ff.camaro.artifact;

import java.util.Arrays;

public class Artifact {

	private final String name;
	private final String org;
	private final String version;
	private final boolean transitive;
	private final String classifier;

	public Artifact(final String name, final String org, final String version, final boolean transitive,
			final String classifier) {
		super();
		this.name = name;
		this.org = org;
		this.version = version;
		this.transitive = transitive;
		this.classifier = classifier;
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

	public String getVersion() {
		return version;
	}

	public boolean isTransitive() {
		return transitive;
	}

	@Override
	public String toString() {
		return (transitive ? "" : "-") + org + "@" + name
				+ (classifier != null && classifier.length() > 0 ? "#" + classifier : "") + ";" + version;
	}

}
