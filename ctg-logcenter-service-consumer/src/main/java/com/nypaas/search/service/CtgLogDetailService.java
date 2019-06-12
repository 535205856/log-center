package com.nypaas.search.service;

import com.nypaas.search.LogCenterSearchService;
import com.nypaas.search.common.Constants;
import com.nypaas.search.common.PubUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CtgLogDetailService {
    @Autowired
    LogCenterSearchService imsSearchService;

    public Object totalDetail(Map<String, Object> matchFilters) throws Exception {
        /**
         * 基本查询条件处理
         */
        Map<String, Object> queryConditionMap = (Map<String, Object>) matchFilters.get("normal");
        String beginDate = ((ArrayList<String>) queryConditionMap.get("time")).get(0);
        String endDate = ((ArrayList<String>) queryConditionMap.get("time")).get(1);

        String[] userIds = ((ArrayList<String>) queryConditionMap.get("userIds")).toArray(new String[0]);
        String[] systemIds = ((ArrayList<String>) queryConditionMap.get("systemIds")).toArray(new String[0]);
        String[] orgIds = ((ArrayList<String>) queryConditionMap.get("orgIds")).toArray(new String[0]);
        String detailTab = queryConditionMap.get(Constants.DETAILTABNAME).toString();
        Map<String, Object> matchMap = new HashMap<>();
        matchMap.put("beginTime", beginDate);
        matchMap.put("endTime", endDate);
        if (userIds.length > 0) {
            matchMap.put("userIds", userIds);
        }
        if (systemIds.length > 0) {
            matchMap.put("systemIds", systemIds);
        }
        matchMap.put(Constants.DETAILTABNAME, detailTab);
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) imsSearchService.queryNoPage(Constants.ES_INDEX, Constants.ES_TYPE, matchMap).get("content");
        if (Constants.DETAILTAB[0].equals(detailTab)) {//访问失败统计
            return queryByFail(resultList);
        } else if (Constants.DETAILTAB[1].equals(detailTab)) {//终端统计
            return queryByTerminal(resultList);
        }else if (Constants.DETAILTAB[2].equals(detailTab)) {//浏览器统计
            return queryByBrowser(resultList);
        } else if (Constants.DETAILTAB[3].equals(detailTab)) {//系统统计
            return queryBySystem(resultList);
        } else if (Constants.DETAILTAB[4].equals(detailTab)) {//组织统计
            return queryByOrg(resultList);
        }
        return resultList;
    }

    /**
     * 统计登录失败维度数据
     *
     * @param resultList
     * @return
     */
    public Map<String, Object> queryByFail(List<Map<String, Object>> resultList) throws Exception{
        Map<String, Object> finalResult = new HashMap<>();
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Map<String, Set<String>>> psnCountMap = new HashMap<>();//依据人员id去重，统计登录人数pk集合
            Map<String, Map<String, Integer>> finalPsnCountMap = new HashMap<>();//依据人员id去重，统计登录人数
            for (int i = 0; i < resultList.size(); i++) {
                Map<String, Object> valueMap = resultList.get(i);
                String system = (valueMap.get(Constants.FIELD_APPID) == null || valueMap.get(Constants.FIELD_APPID).toString().length() == 0) ? "Other" : valueMap.get(Constants.FIELD_APPID).toString();
                //统计登录失败次数
                if (map.containsKey(system)) {
                    Map<String, Integer> errMap = map.get(system);
                    if (checkFailType(valueMap)) {//登录失败记录次数
                        errMap.put("loginerror", errMap.get("loginerror") + 1);
                    } else {
                        errMap.put("systemerror", errMap.get("systemerror") + 1);
                    }
                    errMap.put("errorcount", errMap.get("errorcount") + 1);
                    map.put(system, errMap);
                } else {
                    Map<String, Integer> errMap = new HashMap<>();
                    errMap.put("loginerror", 0);
                    errMap.put("systemerror", 0);
                    errMap.put("errorcount", 1);
                    if (checkFailType(valueMap)) {//登录失败记录次数
                        errMap.put("loginerror", 1);
                    } else {
                        errMap.put("systemerror", 1);
                    }
                    map.put(system, errMap);
                }

                //访问人数统计
                String psnid = valueMap.get(Constants.FIELD_USER_ID).toString();
                if (psnCountMap.containsKey(system)) {
                    Map<String, Set<String>> countMap = psnCountMap.get(system);
                    Set<String> psncount = countMap.get("psncount");
                    psncount.add(psnid);
                    Set<String> loginerror = countMap.get("psncount_loginerror");
                    Set<String> systemerror = countMap.get("psncount_systemerror");
                    if (checkFailType(valueMap)) {//登录失败记录次数
                        loginerror.add(psnid);
                    } else {
                        systemerror.add(psnid);
                    }
                    psnCountMap.put(system, countMap);
                } else {
                    Map<String, Set<String>> countMap = new HashMap<>();
                    Set<String> psncount = new HashSet<>();
                    psncount.add(psnid);
                    Set<String> loginerror = new HashSet<>();
                    Set<String> systemerror = new HashSet<>();
                    if (checkFailType(valueMap)) {//登录失败记录次数
                        loginerror.add(psnid);
                    } else {
                        systemerror.add(psnid);
                    }
                    countMap.put("psncount_loginerror", loginerror);
                    countMap.put("psncount_systemerror", systemerror);
                    countMap.put("psncount", psncount);
                    psnCountMap.put(system, countMap);
                }
            }
            finalResult.put("numberOfTimes", PubUtil.sortByVisitCount(map, "errorcount"));

            //将set集合转换为统计的数量
            for (String key : psnCountMap.keySet()) {
                Map<String, Set<String>> curMap = psnCountMap.get(key);
                Map<String, Integer> curCountMap = new HashMap<>();
                for (String curKey : curMap.keySet()) {
                    curCountMap.put(curKey, curMap.get(curKey).size());
                }
                finalPsnCountMap.put(key, curCountMap);
            }
            finalResult.put("numberOfUsers", PubUtil.sortByVisitCount(finalPsnCountMap, "psncount"));

//            List<Map> data=new ArrayList<>();
//            for (String key:map.keySet()){
//
//
//            }
//
//        finalResult.put("data",data);
            return finalResult;
    }

    /**
     * 判断登录失败的类型，如果是人为因素的失败则返回true，
     * 其他因素的失败返回false
     *
     * @param map
     * @return
     */
    public boolean checkFailType(Map<String, Object> map) {
        Object obj = map.get(Constants.FIELD_AUTHDETAIL);
        if (obj != null && obj.toString().contains("用户名或密码错误")) {
            return true;
        }
        return false;
    }

    /**
     * 统计终端维度数据
     *
     * @param resultList
     * @return
     */
    public Map<String, Object> queryByTerminal(List<Map<String, Object>> resultList) throws Exception{
        Map<String, Integer> map = new HashMap<>();//终端访问次数
        Map<String, Integer> psnMap = new HashMap<>();//终端访问人数
        Map<String, Set<String>>psnSetMap=new HashMap<>();
        for (int i = 0; i < resultList.size(); i++) {
            Map<String, Object> valueMap = resultList.get(i);
            String psnId=valueMap.get(Constants.FIELD_USER_ID).toString();
            String terminalType = (valueMap.get(Constants.FIELD_TERMINALTYPE) == null || "Unknown".equals(valueMap.get(Constants.FIELD_TERMINALTYPE).toString())) ? "Other" : valueMap.get(Constants.FIELD_TERMINALTYPE).toString();
            countBydimension(map, psnSetMap, psnId, terminalType);
            psnMap.put(terminalType,psnSetMap.get(terminalType).size());//终端访问人数
        }

        Map<String,Object> finalResult=new HashMap<>();
        finalResult.put("numberOfTimes",map);//终端访问次数
        finalResult.put("numberOfUsers",psnMap);//终端访问人数
        return finalResult;
    }
    /**
     * 统计浏览器维度数据
     *
     * @param resultList
     * @return
     */
    public Map<String, Object> queryByBrowser(List<Map<String, Object>> resultList) throws Exception{
        Map<String, Integer> map = new HashMap<>();
        Map<String, Integer> psnMap = new HashMap<>();//浏览器访问人数
        Map<String, Set<String>>psnSetMap=new HashMap<>();
            for (int i = 0; i < resultList.size(); i++) {
                Map<String, Object> valueMap = resultList.get(i);
                String psnId=valueMap.get(Constants.FIELD_USER_ID).toString();
                if (valueMap.get("index").toString().indexOf(Constants.ES_INDEX_IDP) != -1) {//门户认证索引，不含移动端数据
                    String browserType = (valueMap.get(Constants.FIELD_BROWSERTYPE) == null || "Unknown".equals(valueMap.get(Constants.FIELD_BROWSERTYPE).toString())) ? "Other" : valueMap.get(Constants.FIELD_BROWSERTYPE).toString();
                    countBydimension(map, psnSetMap, psnId, browserType);
                    psnMap.put(browserType,psnSetMap.get(browserType).size());//浏览器访问人数
                }
            }

        Map<String,Object> finalResult=new HashMap<>();
        finalResult.put("numberOfTimes",map);//浏览器访问次数
        finalResult.put("numberOfUsers",psnMap);//浏览器访问人数
        return finalResult;
    }

    /**
     * 依据维度统计访问数量以及访问人数
     * @param countMap 访问次数结果
     * @param psnSetMap 访问人数结果
     * @param psnId  人员id
     * @param dimensionField 统计维度字段
     */
    private void countBydimension(Map<String, Integer> countMap, Map<String, Set<String>> psnSetMap, String psnId, String dimensionField) {
        if (countMap.containsKey(dimensionField)) {//如果包含了已经记录的维度，则取出历史值进行累加
            countMap.put(dimensionField, countMap.get(dimensionField) + 1);
            Set psnSet=psnSetMap.get(dimensionField);
            psnSet.add(psnId);
            psnSetMap.put(dimensionField,psnSet);
        } else {//初始统计次数
            countMap.put(dimensionField, 1);
            Set psnSet=new HashSet();
            psnSet.add(psnId);
            psnSetMap.put(dimensionField,psnSet);
        }
    }

    /**
     * 统计系统维度数据
     *
     * @param resultList
     * @return
     */
    public Map<String, Object> queryBySystem(List<Map<String, Object>> resultList)throws Exception {
        Map<String, Integer> map = new HashMap<>();
        Map<String, Integer> psnMap = new HashMap<>();//系统访问人数
        Map<String, Set<String>>psnSetMap=new HashMap<>();
            for (int i = 0; i < resultList.size(); i++) {
                Map<String, Object> valueMap = resultList.get(i);
                String psnId=valueMap.get(Constants.FIELD_USER_ID).toString();
                String system = (valueMap.get(Constants.FIELD_APPID) == null || valueMap.get(Constants.FIELD_APPID).toString().length() == 0) ? "Other" : valueMap.get(Constants.FIELD_APPID).toString();
                countBydimension(map, psnSetMap, psnId, system);
                psnMap.put(system,psnSetMap.get(system).size());//系统访问人数
            }

        Map<String,Object> finalResult=new HashMap<>();
        finalResult.put("numberOfTimes",map);//系统访问次数
        finalResult.put("numberOfUsers",psnMap);//系统访问人数

            return finalResult;
    }

    /**
     * 统计组织维度数据
     *
     * @param resultList
     * @return
     */
    public Map<String, Object> queryByOrg(List<Map<String, Object>> resultList) throws Exception{
        Map<String, Integer> map = new HashMap<>();
        Map<String, Integer> psnMap = new HashMap<>();//组织访问人数
        Map<String, Set<String>>psnSetMap=new HashMap<>();
        for (int i = 0; i < resultList.size(); i++) {
            Map<String, Object> valueMap = resultList.get(i);
            String psnId=valueMap.get(Constants.FIELD_USER_ID).toString();
            String orgId = valueMap.get(Constants.FIELD_ORGID) == null ? "Other" : valueMap.get(Constants.FIELD_ORGID).toString();
            countBydimension(map, psnSetMap, psnId, orgId);
            psnMap.put(orgId,psnSetMap.get(orgId).size());//组织访问人数
        }

        Map<String,Object> finalResult=new HashMap<>();
        finalResult.put("numberOfTimes",map);//组织访问次数
        finalResult.put("numberOfUsers",psnMap);//组织访问人数
        return finalResult;

    }

}
