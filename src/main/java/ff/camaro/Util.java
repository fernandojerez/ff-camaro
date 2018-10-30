package ff.camaro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Util {

	public static Map<String, Object> map(Object... props){
		Map<String, Object> map = new HashMap<String, Object>();
		for(int i = 0, len = props.length; i < len; i+=2) {
			map.put(String.valueOf(props[i]), props[i+1]);
		}
		return map;
	}
	
	public static List<Object> arrayList(Object... values){
		List<Object> list = new ArrayList<Object>();
		for(Object val : values) {
			list.add(val);
		}
		return list;
	}
	
	public static String getFFRepo() {
    	if(System.getenv("FF_REPO") != null){
    		return System.getenv("FF_REPO"); 
    	}
    	String usrHome = System.getProperty("user.home");
    	return "file://" + usrHome + "/.ff";
	}
}
