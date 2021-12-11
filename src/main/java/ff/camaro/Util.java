package ff.camaro;

import java.util.*;

public class Util {

    public static List<Object> arrayList(final Object... values) {
        final List<Object> list = new ArrayList<>();
        Collections.addAll(list, values);
        return list;
    }

    /**
     * if FF_REPO variable is not defined the local repo will be
     * &lt;user.home&gt;/.ff
     *
     * @return path to local repository
     */
    public static String getFFRepo() {
        if (System.getenv("FF_REPO") != null) {
            final String ffrepo = System.getenv("FF_REPO");
            if (ffrepo.contains("://")) {
                return ffrepo;
            }
            return "file://" + ffrepo;
        }
        final String usrHome = System.getProperty("user.home");
        return "file://" + usrHome + "/.ff";
    }

    public static Map<String, Object> map(final Object... props) {
        final Map<String, Object> map = new HashMap<>();
        for (int i = 0, len = props.length; i < len; i += 2) {
            map.put(String.valueOf(props[i]), props[i + 1]);
        }
        return map;
    }

    @SafeVarargs
    public static <T> Set<T> set(final T... values) {
        final Set<T> list = new HashSet<>();
        Collections.addAll(list, values);
        return list;
    }
}
