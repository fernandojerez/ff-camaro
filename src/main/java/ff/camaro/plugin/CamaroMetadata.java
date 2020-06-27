package ff.camaro.plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class CamaroMetadata {

	private final Set<String> languages;
	private final Set<String> configurations;
	private final Set<String> enabled_languages;

	public CamaroMetadata() {
		languages = new TreeSet<>();
		configurations = new TreeSet<>();
		enabled_languages = new HashSet<>();
	}

	public void enabled_languages(final String language) {
		enabled_languages.add(language);
	}

	public Set<String> getConfigurations() {
		return configurations;
	}

	public Set<String> getLanguages() {
		return languages;
	}

	public boolean isLanguageEnabled(final String language) {
		if (enabled_languages.isEmpty()) {
			return true;
		}
		return enabled_languages.contains(language);
	}

}
