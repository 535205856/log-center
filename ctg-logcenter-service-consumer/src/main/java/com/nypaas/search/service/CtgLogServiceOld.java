package com.nypaas.search.service;

import com.nypaas.search.LogCenterSearchService;
import com.nypaas.search.common.Constants;
import com.nypaas.search.common.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CtgLogServiceOld {
    @Autowired
    LogCenterSearchService imsSearchService;
    final String esIndex = Constants.ES_INDEX;
    final String esType = Constants.ES_TYPE;


    /**
     * 数据总览
     * @return
     * @throws Exception
     */
    public Map<String, Object> homeOverview() throws Exception {
        Map<String, Object> matchFilters = new HashMap<>();
        Map<String, Object> curResult = totalCurCount(esIndex, esType, matchFilters);
        Map<String, Object> yesterdayResult = totalYesterdayCount(esIndex, esType, matchFilters);
        Map<String, Object> curYearResult = totalCurYearCount(esIndex, esType, matchFilters);
        Map<String, Object> result = new HashMap<>();
        result.put("today", curResult);
        result.put("yesterday", yesterdayResult);
        result.put("currentYear", curYearResult);
        return result;
    }

    /**
     * 访问趋势
     * @param matchFilters
     * @return
     * @throws Exception
     */
    public List<Map> trendsInVisits(Map<String, Object> matchFilters) throws Exception {
        if(!matchFilters.containsKey("beginTime")||!matchFilters.containsKey("endTime")){
            String todayDateStr = DateUtil.getDateFormat(DateUtil.getNowDate());//今天
            matchFilters.put("beginTime", todayDateStr);
            matchFilters.put("endTime", todayDateStr);
        }
        String beginTime = matchFilters.get("beginTime").toString();
        String endTime = matchFilters.get("endTime").toString();
        String endDateTime = endTime + " 23:59:59";
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) imsSearchService.queryNoPage(esIndex, esType, matchFilters).get("content");//查询当前条件 不带分页信息
        List<Map<String, Object>> yestordayResultList = new ArrayList<>();//昨天数据集合
        String yesterdayDateStr = DateUtil.getDateFormat(DateUtil.getDayBeforeDays(1));//昨天
        boolean iscurdayQuery = false;
        if (beginTime.equals(endTime) && beginTime.equals(DateUtil.getDateFormat(DateUtil.getNowDate()))) {//是否是查询的今天,如果查询的是当天，则包含昨日数据曲线
            iscurdayQuery = true;
            matchFilters.put("beginTime", yesterdayDateStr);
            matchFilters.put("endTime", yesterdayDateStr);
            yestordayResultList = (List<Map<String, Object>>) imsSearchService.queryNoPage(esIndex, esType, matchFilters).get("content");//查询昨日数据
        }

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
        if (iscurdayQuery) {
            map.put("yestordayvalue", 0);
        }
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

            if (iscurdayQuery) {
                map.put("yestordayvalue", queryTrendsInVisitsCount(yestordayResultList, yesterdayDateStr + beginTimeStr.substring(10, 19), yesterdayDateStr + endTimeStr.substring(10, 19)));
            }
            listArray.add(map);
        }
        return listArray;
    }

    /**
     * 依据日期区间，统计访问次数
     *
     * @param resultList
     * @param beginTime
     * @param endTime
     * @return
     */
    public int queryTrendsInVisitsCount(List<Map<String, Object>> resultList, String beginTime, String endTime) {
        List<Map<String, Object>> filterList = new ArrayList<>();
        for (int i = 0; i < resultList.size(); i++) {
            Map<String, Object> map = resultList.get(i);
            long time = DateUtil.getDateTimeFormat(map.get(Constants.FIELD_DATE).toString()).getTime();
            if (time >= DateUtil.getDateTimeFormat(beginTime).getTime() && time < DateUtil.getDateTimeFormat(endTime).getTime()) {
                filterList.add(map);
            }
        }
        return filterList.size();
    }


    private Map<String, Object> totalCurCount(String index, String type, Map<String, Object> matchFilters) throws Exception {
        String todayDateStr = DateUtil.getDateFormat(DateUtil.getNowDate());//今天
        matchFilters.put("beginTime", todayDateStr);
        matchFilters.put("endTime", todayDateStr);
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) imsSearchService.queryNoPage(index, type, matchFilters).get("content");//不带分页信息
        return totalResult(resultList);
    }

    private Map<String, Object> totalYesterdayCount(String index, String type, Map<String, Object> matchFilters) throws Exception {
        String yesterdayDateStr = DateUtil.getDateFormat(DateUtil.getDayBeforeDays(1));//昨天
        matchFilters.put("beginTime", yesterdayDateStr);
        matchFilters.put("endTime", yesterdayDateStr);
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) imsSearchService.queryNoPage(index, type, matchFilters).get("content");
        return totalResult(resultList);
    }

    private Map<String, Object> totalCurYearCount(String index, String type, Map<String, Object> matchFilters) throws Exception {
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
        for (Map<String, Object> map : resultList) {//依据用户id去除重复数据
            useridSet.add(map.get(Constants.FIELD_USER_ID));
            ipSet.add(map.get(Constants.FIELD_IP));
            if (!("true".equals(map.get(Constants.FIELD_STATUS)) || "200".equals(map.get(Constants.FIELD_STATUS)))) {//登录认证状态为不为true或是200都认为是失败
                errorCount++;
            }
        }
        Map map = new HashMap();
        map.put("loginCount", resultList.size());//登录数极为日志记录的行数
        map.put("personCount", useridSet.size());
        map.put("ipCount", ipSet.size());
        map.put("errorCount", errorCount);

        return map;
    }

    /***
     * 终端分布
     * @param matchFilters
     * @return
     */
    public Map<String, Integer> terminalDistribution(Map<String, Object> matchFilters) throws Exception {
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) imsSearchService.queryNoPage(esIndex, esType, matchFilters).get("content");//查询当前条件 不带分页信息
        Map<String,Integer> map=new HashMap<>();
        for (int i = 0; i < resultList.size(); i++) {
            Map<String,Object> valueMap= resultList.get(i);
            String terminalType=(valueMap.get(Constants.FIELD_TERMINALTYPE)==null||"Unknown".equals(valueMap.get(Constants.FIELD_TERMINALTYPE).toString()))?"Other":valueMap.get(Constants.FIELD_TERMINALTYPE).toString();
            if(map.containsKey(terminalType)){
                map.put(terminalType ,map.get(terminalType)+1);
            }else{
                map.put(terminalType ,1);
            }
        }

        return map;
    }

    /***
     * 浏览器分布
     * @param matchFilters
     * @return
     */
    public Map<String, Integer> browserDistribution(Map<String, Object> matchFilters) throws Exception {
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) imsSearchService.queryNoPage(esIndex, esType, matchFilters).get("content");//查询当前条件 不带分页信息
        Map<String,Integer> map=new HashMap<>();
       int appBroswer=0;//app端登陆的次数
        for (int i = 0; i < resultList.size(); i++) {
            Map<String,Object> valueMap= resultList.get(i);
            if(valueMap.get("index").toString().indexOf(Constants.ES_INDEX_IDP)!=-1){//门户认证
                String browserType=(valueMap.get(Constants.FIELD_BROWSERTYPE)==null||"Unknown".equals(valueMap.get(Constants.FIELD_BROWSERTYPE).toString()))?"Other":valueMap.get(Constants.FIELD_BROWSERTYPE).toString();
                if(map.containsKey(browserType)){
                    map.put(browserType ,map.get(browserType)+1);
                }else{
                    map.put(browserType ,1);
                }
            }else {
                appBroswer+=1;
                map.put("APP",appBroswer);
            }
        }
        return map;
    }


    public Map<String, Object> query(Integer curpage, Integer pagesize, Map<String, Object> matchFilters) throws Exception {
        Map<String, Object> result = imsSearchService.pagedQuery(esIndex, esType, curpage, pagesize, matchFilters, Constants.ES_SHOWFIELDS);
        return result;
    }

}
