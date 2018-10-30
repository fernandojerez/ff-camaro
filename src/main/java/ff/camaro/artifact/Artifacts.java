package ff.camaro.artifact;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Lee el archivo de artifacts y crear una lista de artefactos
 *
 * @author fernandojerez
 */
public class Artifacts {

	private final Map<String, List<Artifact>> artifacts = new LinkedHashMap<>();

	private final Map<String, List<Artifact>> collections = new LinkedHashMap<>();

	public Artifacts() {
		return;
	}

	public List<Artifact> getArtifact(final String orgName) {
		return artifacts.get(orgName);
	}

	public List<Artifact> getArtifactFromCollection(final String collectionName) {
		return collections.get(collectionName);
	}

	public Collection<List<Artifact>> getArtifacts() {
		return artifacts.values();
	}

	public void load(final String path) throws IOException {
		final InputStream stream = new FileInputStream(path);
		if (stream != null) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = null;
			boolean transitive = true;
			String org = "";
			String name = "";
			String version = "";
			String collection = "default";
			String classifiers = "";
			while (true) {
				line = reader.readLine();
				if (line == null) {
					break;
				}
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("%")) {
					collection = line.substring(1).trim();
					continue;
				}
				transitive = true;
				String _org = "";
				String _name = "";
				String _version = "";
				String _classifiers = "";
				if (line.startsWith("-")) {
					transitive = false;
					line = line.substring(1);
				}
				final int orgIx = line.indexOf('@');
				if (orgIx != -1) {
					_org = line.substring(0, orgIx).trim();
					line = line.substring(orgIx + 1).trim();
				}
				final int classifiersIx = line.indexOf('#');
				if (classifiersIx != -1) {
					_name = line.substring(0, classifiersIx).trim();
					line = line.substring(classifiersIx + 1).trim();
				}
				final int versionIx = line.indexOf(';');
				if (versionIx != -1) {
					if (classifiersIx != -1) {
						_classifiers = line.substring(0, versionIx).trim();
					} else {
						_name = line.substring(0, versionIx).trim();
					}
					_version = line.substring(versionIx + 1).trim();
				} else {
					_name = line;
				}
				if (_org.length() > 0) {
					org = _org.replace("$", org);
				}
				if (_name.length() > 0) {
					name = _name.replace("$", org);
				}
				if (_version.length() > 0) {
					version = _version;
				}
				if (_org.length() > 0) {
					classifiers = _classifiers;
				}
				if (_name.length() == 0) {
					continue;
				}
				final Artifact artifact = new Artifact(name, org, version, transitive, classifiers);
				final String artifactKey = org + "@" + name;
				List<Artifact> artifactsList = artifacts.get(artifactKey);
				if (artifactsList == null) {
					artifactsList = new LinkedList<>();
					artifacts.put(artifactKey, artifactsList);
				}
				artifactsList.add(artifact);
				List<Artifact> colls = collections.get(collection);
				if (colls == null) {
					colls = new LinkedList<>();
					collections.put(collection, colls);
				}
				colls.add(artifact);
			}
			reader.close();
		}
	}
}
