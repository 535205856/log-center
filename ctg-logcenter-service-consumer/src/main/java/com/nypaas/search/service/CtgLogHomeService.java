package com.nypaas.search.service;

import com.nypaas.search.LogCenterSearchService;
import com.nypaas.search.common.Constants;
import com.nypaas.search.common.DateUtil;
import com.nypaas.search.common.PubUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CtgLogHomeService {
    @Autowired
    LogCenterSearchService imsSearchService;

    public Map<String, Object> homeData() throws Exception {
        Map<String, Object> finalResult = new HashMap<>();

        String todayDateStr = DateUtil.getDateFormat(DateUtil.getNowDate());//今天
        Map<String, Object> matchFilters = new HashMap<>();
        matchFilters.put("beginTime", todayDateStr);
        matchFilters.put("endTime", todayDateStr);
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) imsSearchService.queryNoPage(Constants.ES_INDEX, Constants.ES_TYPE, matchFilters).get("content");//不带分页信息
        String yesterdayDateStr = DateUtil.getDateFormat(DateUtil.getDayBeforeDays(1));//昨天

        //昨日数据曲线
        Map<String, Object> matchFilters1 = new HashMap<>();
        matchFilters1.put("beginTime", yesterdayDateStr);
        matchFilters1.put("endTime", yesterdayDateStr);
        List<Map<String, Object>> yestordayResultList = (List<Map<String, Object>>) imsSearchService.queryNoPage(Constants.ES_INDEX, Constants.ES_TYPE, matchFilters1).get("content");//查询昨日数据


        finalResult.put("top", homeOverview(resultList, yestordayResultList));//顶部概况数据
        finalResult.put("trends", trendsInVisits(resultList, yestordayResultList));//访问趋势维度统计
        finalResult.put("systemerror", systemErrorDistribution(resultList)); //依据系统维度统计访问失败次数
        finalResult.put("terminal", terminalDistribution(resultList));//终端分布
        finalResult.put("browser", browserDistribution(resultList));//浏览器分布
        finalResult.put("org", orgDistribution(resultList, yestordayResultList));//组织维度统计
        finalResult.put("system", systemDistribution(resultList)); //系统维度统计

        return finalResult;
    }


    /**
     * 数据总览
     *
     * @return
     * @throws Exception
     */
    public Map<String, Object> homeOverview(List<Map<String, Object>> resultList, List<Map<String, Object>> yestordayResultList) {
        Map<String, Object> finalResult = new HashMap<>();
        try {
            Map<String, Object> curResult = totalCurCount(Constants.ES_INDEX, Constants.ES_TYPE, resultList);
            Map<String, Object> yesterdayResult = totalYesterdayCount(Constants.ES_INDEX, Constants.ES_TYPE, yestordayResultList);
            Map<String, Object> curYearResult = totalCurYearCount(Constants.ES_INDEX, Constants.ES_TYPE);
            Map<String, Object> result = new HashMap<>();
            result.put("today", curResult);
            result.put("yesterday", yesterdayResult);
            result.put("currentYear", curYearResult);
            finalResult.put("status", "success");
            finalResult.put("content", result);
        } catch (Exception e) {
            finalResult.put("status", "error");
            finalResult.put("content", e.getMessage());
        }
        return finalResult;
    }

    /**
     * 访问趋势
     *
     * @param resultList
     * @return
     * @throws Exception
     */
    public Map<String, Object> trendsInVisits(List<Map<String, Object>> resultList, List<Map<String, Object>> yestordayResultList) {
        Map<String, Object> finalResult = new HashMap<>();
        try {
            String todayDateStr = DateUtil.getDateFormat(DateUtil.getNowDate());//今天
            String beginTime = todayDateStr;
            String endTime = todayDateStr;
            String endDateTime = endTime + " 23:59:59";
            String yesterdayDateStr = DateUtil.getDateFormat(DateUtil.getDayBeforeDays(1));//昨天

            List<Date> list = DateUtil.getEveryDay(DateUtil.getDateFormat(beginTime), DateUtil.getDateFormat(endTime));//查询起止时间中包含的日期
            int hourSeparate = list.size() * 24 / 24;//将天数转换为小时，算出每个点的时间间隔

            //数据中存的是开始日期和结束日期 大于等于 开始时间[0],小于结束时间[1]
            List<String[]> dateList = new ArrayList<>();
            Date beginDate = DateUtil.getDateFormat(beginTime);//开始日期
            for (int i = 0; i < 24; i++) {//推算出每个点的时间间隔
                Date endDate = DateUtil.getDayAfterHours(beginDate, hourSeparate);
                String endDateStr = DateUtil.getDateTimeFormat(endDate);
                dateList.add(new String[]{DateUtil.getDateTimeFormat(beginDate), endDateStr});
                beginDate = endDate;
            }
            List<Map> listArray = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            map.put("xname", beginTime + " 00:00:00");
            map.put("xvalue", 0);
            map.put("yestordayvalue", 0);
            listArray.add(map);//第一个点的数据认为是0

            //循环区间日期，查询在区间内的访问次数
            for (int i = 0; i < dateList.size(); i++) {
                String beginTimeStr = dateList.get(i)[0];
                String endTimeStr = dateList.get(i)[1];
                map = new HashMap<>();

                if (i == dateList.size() - 1) {//最后一个取日期的24点
                    map.put("xname", endDateTime);
                    endTimeStr = endDateTime;
                } else {
                    map.put("xname", dateList.get(i)[1]);
                }
                map.put("xvalue", queryTrendsInVisitsCount(resultList, beginTimeStr, endTimeStr));

                map.put("yestordayvalue", queryTrendsInVisitsCount(yestordayResultList, yesterdayDateStr + beginTimeStr.substring(10, 19), yesterdayDateStr + endTimeStr.substring(10, 19)));
                listArray.add(map);
            }
            finalResult.put("status", "success");
            finalResult.put("content", listArray);

        } catch (Exception e) {
            finalResult.put("status", "error");
            finalResult.put("content", e.getMessage());
        }
        return finalResult;
    }


    /**
     * 依据日期区间，统计访问次数
     *
     * @param resultList
     * @param beginTime
     * @param endTime
     * @return
     */
    private int queryTrendsInVisitsCount(List<Map<String, Object>> resultList, String beginTime, String endTime) {
        List<Map<String, Object>> filterList = new ArrayList<>();
        for (int i = 0; i < resultList.size(); i++) {
            Map<String, Object> valueMap = resultList.get(i);
            if (!isSuccess(valueMap)) {//访问失败则不进行累加
                continue;
            }
            long time = DateUtil.getDateTimeFormat(valueMap.get(Constants.FIELD_DATE).toString()).getTime();
            if (time >= DateUtil.getDateTimeFormat(beginTime).getTime() && time < DateUtil.getDateTimeFormat(endTime).getTime()) {
                filterList.add(valueMap);
            }
        }
        return filterList.size();
    }


    private Map<String, Object> totalCurCount(String index, String type, List<Map<String, Object>> resultList) throws Exception {
        return totalResult(resultList);
    }

    private Map<String, Object> totalYesterdayCount(String index, String type, List<Map<String, Object>> yestordayResultList) throws Exception {
//        String yesterdayDateStr = DateUtil.getDateFormat(DateUtil.getDayBeforeDays(1));//昨天
//        Map<String, Object> matchFilters = new HashMap<>();
//        matchFilters.put("beginTime", yesterdayDateStr);
//        matchFilters.put("endTime", yesterdayDateStr);
//        resultList = (List<Map<String, Object>>) imsSearchService.queryNoPage(index, type, matchFilters).get("content");
        return totalResult(yestordayResultList);
    }

    private Map<String, Object> totalCurYearCount(String index, String type) throws Exception {
        Map<String, Object> matchFilters = new HashMap<>();
        matchFilters.put("beginTime", DateUtil.getNowYear() + "-01-01");
        matchFilters.put("endTime", DateUtil.getDateFormat(DateUtil.getNowDate()));
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) imsSearchService.queryNoPage(index, type, matchFilters).get("content");
        return totalResult(resultList);
    }

    /**
     * 统计概况数据
     *
     * @param resultList
     * @return
     */
    private Map<String, Object> totalResult(List<Map<String, Object>> resultList) {
        int errorCount = 0;
        Set<Object> useridSet = new HashSet<>();
        Set<Object> ipSet = new HashSet<>();
        for (Map<String, Object> map : resultList) {
            if (!isSuccess(map)) {//统计失败次数
                errorCount++;
            } else {//统计访问成功的
                useridSet.add(map.get(Constants.FIELD_USER_ID));//依据用户id去除重复数据
                ipSet.add(map.get(Constants.FIELD_IP));//依据IP去除重复数据
            }
        }
        Map map = new HashMap();
        map.put("loginCount", resultList.size() - errorCount);//登录数极为日志记录的行数(去掉访问失败的次数)
        map.put("personCount", useridSet.size());
        map.put("ipCount", ipSet.size());
        map.put("errorCount", errorCount);

        return map;
    }

    /***
     * 终端分布
     * @param resultList
     * @return
     */
    public Map<String, Object> terminalDistribution(List<Map<String, Object>> resultList) {
        Map<String, Object> finalResult = new HashMap<>();
        try {
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < resultList.size(); i++) {
                Map<String, Object> valueMap = resultList.get(i);
                if (!isSuccess(valueMap)) {//访问失败则不进行累加
                    continue;
                }
                String terminalType = (valueMap.get(Constants.FIELD_TERMINALTYPE) == null || "Unknown".equals(valueMap.get(Constants.FIELD_TERMINALTYPE).toString())) ? "Other" : valueMap.get(Constants.FIELD_TERMINALTYPE).toString();
                if (map.containsKey(terminalType)) {
                    map.put(terminalType, map.get(terminalType) + 1);
                } else {
                    map.put(terminalType, 1);
                }
            }
            finalResult.put("status", "success");
            finalResult.put("content", map);
        } catch (Exception e) {
            finalResult.put("status", "error");
            finalResult.put("content", e.getMessage());
        }
        return finalResult;
    }

    /***
     * 浏览器分布，移动端数据不做统计
     * @param resultList
     * @return
     */
    public Map<String, Object> browserDistribution(List<Map<String, Object>> resultList) {
        Map<String, Object> finalResult = new HashMap<>();
        try {
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < resultList.size(); i++) {
                Map<String, Object> valueMap = resultList.get(i);
                if (!isSuccess(valueMap)) {//访问失败则不进行累加
                    continue;
                }
                if (valueMap.get("index").toString().indexOf(Constants.ES_INDEX_IDP) != -1) {//门户认证索引，不含移动端数据
                    String browserType = (valueMap.get(Constants.FIELD_BROWSERTYPE) == null || "Unknown".equals(valueMap.get(Constants.FIELD_BROWSERTYPE).toString())) ? "Other" : valueMap.get(Constants.FIELD_BROWSERTYPE).toString();
                    if (map.containsKey(browserType)) {
                        map.put(browserType, map.get(browserType) + 1);
                    } else {
                        map.put(browserType, 1);
                    }
                }
            }
            finalResult.put("status", "success");
            finalResult.put("content", map);
        } catch (Exception e) {
            finalResult.put("status", "error");
            finalResult.put("content", e.getMessage());
        }
        return finalResult;
    }

    /**
     * 系统访问维度统计
     *
     * @param resultList
     */
    private Map<String, Object> systemDistribution(List<Map<String, Object>> resultList) {
        Map<String, Object> finalResult = new HashMap<>();
        try {
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < resultList.size(); i++) {
                Map<String, Object> valueMap = resultList.get(i);
                if (!isSuccess(valueMap)) {//访问失败则不进行累加
                    continue;
                }
                String system = (valueMap.get(Constants.FIELD_APPID) == null || valueMap.get(Constants.FIELD_APPID).toString().length() == 0) ? "Other" : valueMap.get(Constants.FIELD_APPID).toString();
                if (map.containsKey(system)) {
                    map.put(system, map.get(system) + 1);
                } else {
                    map.put(system, 1);
                }
            }
            finalResult.put("status", "success");
            finalResult.put("content", PubUtil.sortByVisitCount(map));
        } catch (Exception e) {
            finalResult.put("status", "error");
            finalResult.put("content", e.getMessage());
        }
        return finalResult;
    }

    /**
     * 依据访问状态判断访问是否成功，true表示是pc端认证的成功结果，200表示的是移动端认证成功的结果
     *
     * @param valueMap
     * @return
     */
    public boolean isSuccess(Map<String, Object> valueMap) {
        if (("true".equals(valueMap.get(Constants.FIELD_STATUS)) || "200".equals(valueMap.get(Constants.FIELD_STATUS)))) {//登录认证状态为true或是200都认为是访问成功
            return true;
        }
        return false;
    }

    /**
     * 系统访问失败维度统计
     *
     * @param resultList
     */
    private Map<String, Object> systemErrorDistribution(List<Map<String, Object>> resultList) {
        Map<String, Object> finalResult = new HashMap<>();
        try {
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < resultList.size(); i++) {
                Map<String, Object> valueMap = resultList.get(i);
                if (isSuccess(valueMap)) {//访问成功则不进行累加
                    continue;
                }
                String system = (valueMap.get(Constants.FIELD_APPID) == null || valueMap.get(Constants.FIELD_APPID).toString().length() == 0) ? "Other" : valueMap.get(Constants.FIELD_APPID).toString();
                if (map.containsKey(system)) {
                    map.put(system, map.get(system) + 1);
                } else {
                    map.put(system, 1);
                }
            }
            finalResult.put("status", "success");
            finalResult.put("content", PubUtil.sortByVisitCount(map));
        } catch (Exception e) {
            finalResult.put("status", "error");
            finalResult.put("content", e.getMessage());
        }
        return finalResult;
    }
    /**
     * 组织维度统计
     *
     * @param resultList
     */
    private Map<String, Object> orgDistribution(List<Map<String, Object>> resultList, List<Map<String, Object>> yestordayResultList) {
        Map<String, Object> finalResult = new HashMap<>();
        try {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            //今日数据，累加访问次数
            for (int i = 0; i < resultList.size(); i++) {
                Map<String, Object> valueMap = resultList.get(i);
                if (!isSuccess(valueMap)) {//访问失败则不进行累加
                    continue;
                }
                String orgId = valueMap.get(Constants.FIELD_ORGID) == null ? "Other" : valueMap.get(Constants.FIELD_ORGID).toString();
                if (map.containsKey(orgId)) {
                    Map<String, Integer> vmap = map.get(orgId);
                    vmap.put("today", vmap.get("today") + 1);
                    map.put(orgId, vmap);
                } else {
                    Map<String, Integer> vmap = new HashMap<>();
                    vmap.put("today", 1);
                    vmap.put("yestorday", 0);
                    map.put(orgId, vmap);
                }
            }

            //昨日数据，累加访问次数
            for (int i = 0; i < yestordayResultList.size(); i++) {
                Map<String, Object> valueMap = yestordayResultList.get(i);
                if (!isSuccess(valueMap)) {//访问失败则不进行累加
                    continue;
                }
                String orgId = valueMap.get(Constants.FIELD_ORGID) == null ? "Other" : valueMap.get(Constants.FIELD_ORGID).toString();
                if (map.containsKey(orgId)) {
                    Map<String, Integer> vmap = map.get(orgId);
                    vmap.put("yestorday", vmap.get("yestorday") + 1);
                    map.put(orgId, vmap);
                }
            }


            finalResult.put("status", "success");
            finalResult.put("content", PubUtil.sortByVisitCount(map,"today"));//依据今天的访问次数对结果集进行排序
        } catch (Exception e) {
            finalResult.put("status", "error");
            finalResult.put("content", e.getMessage());
        }
        return finalResult;
    }
}
