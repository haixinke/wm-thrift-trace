package com.wm.rpc.thrift.expand;

import java.util.HashMap;
import java.util.Map;

/**
 * Desc: ExpandContexts
 *
 * @author wangmin
 * Date: 2021/3/12
 * Time: 10:39 上午
 */
public class ExpandContexts {

    private final static ThreadLocal<Map<String, String>> THREAD_LOCAL_EXPAND = ThreadLocal.withInitial(HashMap::new);

    public static void put(String key, String value) {
        THREAD_LOCAL_EXPAND.get().put(key, value);
    }

    public static String get(String key) {
        return THREAD_LOCAL_EXPAND.get().get(key);
    }

    public static String remove(String key) {
        return THREAD_LOCAL_EXPAND.get().remove(key);
    }

    public static Map<String, String> getExpandContext() {
        return THREAD_LOCAL_EXPAND.get();
    }

    public static void clearExpandContext() {
        THREAD_LOCAL_EXPAND.remove();
    }

}
