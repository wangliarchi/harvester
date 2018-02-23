package edu.olivet.harvester.utils.common;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/17/2018 11:52 AM
 */
public class MapUtils {

    public static boolean containsAnyKey(final Map<?, ?> map, List<Object> keys) {
        return containsAnyKey(map, keys.toArray());
    }

    public static boolean containsAnyKey(final Map<?, ?> map, Object... keys) {
        for (Object key : keys) {
            if (map.containsKey(key)) {
                return true;
            }
        }

        return false;
    }

    public static <V> V getValue(final Map<?, V> map, Object... keys) {
        for (Object key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }

        return null;
    }
}
