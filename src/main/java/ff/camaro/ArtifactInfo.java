package ff.camaro;

public class ArtifactInfo {

	private final String name;
	private final String group;
	private final String version;

	public ArtifactInfo(final String name, final String group, final String version) {
		super();
		this.name = name;
		this.group = group;
		this.version = version;
	}

	public String getGroup() {
		return this.group;
	}

	public String getName() {
		return this.name;
	}

	public String getVersion() {
		return this.version;
	}
}
