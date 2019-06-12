package com.nypaas.search.common;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PubUtil {

    /**
     * 依据map中的value值，对map的结果集进行排序，倒叙排
     * @param map
     * @return
     */
    public static Map<String, Integer> sortByVisitCount(Map<String, Integer> map) {
        int[] keyArray = new int[map.keySet().size()];
        String[] valueArray = new String[map.keySet().size()];

        for (int i = 0; i < map.keySet().size(); i++) {
            String key = map.keySet().toArray(new String[]{})[i];
            keyArray[i] = map.get(key);
            valueArray[i] = key;
        }
        for (int i = 0; i < keyArray.length - 1; i++) {
            for (int j = i + 1; j < keyArray.length; j++) {
                if (keyArray[j] < keyArray[i]) {
                    int temp = keyArray[i];
                    String tempValue = valueArray[i];
                    keyArray[i] = keyArray[j];
                    keyArray[j] = temp;
                    valueArray[i] = valueArray[j];
                    valueArray[j] = tempValue;
                }
            }
        }
        Map<String, Integer> resuMap = new LinkedHashMap();
//        for (int i = 0; i < valueArray.length; i++) {
        for (int i = valueArray.length-1; i>=0 ; i--) {
            String key = valueArray[i];
            resuMap.put(key, map.get(key));
        }
        return resuMap;
    }
    /**
     * 依据子map中，某个字段的value值，对父map的结果集进行排序
     * @param map
     * @return
     */
    public static Map<String, Map<String, Integer>> sortByVisitCount(Map<String, Map<String, Integer>> map,String field) {
        Map<String, Integer> sortMap = new HashMap<>();
        for (String key : map.keySet()) {
            sortMap.put(key, map.get(key).get(field));
        }
        String[] keyOfSortArray = PubUtil.sortByVisitCount(sortMap).keySet().toArray(new String[]{});
        //排序后结果集
        Map<String, Map<String, Integer>> resultMap = new LinkedHashMap<>();
        for (int i = 0; i < keyOfSortArray.length; i++) {
            String key = keyOfSortArray[i];
            resultMap.put(key, map.get(key));
        }
        return resultMap;
    }
}
