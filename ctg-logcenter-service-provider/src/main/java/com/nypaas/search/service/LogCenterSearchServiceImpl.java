package com.nypaas.search.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nypaas.search.LogCenterSearchService;
import com.nypaas.search.common.Constants;
import com.nypaas.search.enty.ConditionEnty;
import com.nypaas.search.enty.ESQueryBuilder;
import com.nypaas.search.es.ESUtil;

import com.yonyou.cloud.utils.StringUtils;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;


import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCount;

import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LogCenterSearchServiceImpl implements LogCenterSearchService {
    @Autowired
    private ESUtil esUtil;

    @Override
    public String hello() {
        return "HELLO dachengzi! " + esUtil;
    }

    /**
     * 查询es数据（带分页）
     *
     * @param index
     * @param type
     * @param offset
     * @param pagesize
     * @param matchFilters
     * @param includeFields
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> pagedQuery(String index, String type, int offset, int pagesize, Map<String, Object> matchFilters, String[] includeFields) throws Exception {
        Map<String, Object> result = esUtil.pagedQuery(index, type, offset, pagesize, toEsQueryBuilder(matchFilters), includeFields, Constants.FIELD_DATE_SORT);
        execResultData(result);
        return result;
    }

    /**
     * 查询es数据（无分页）
     *
     * @param index
     * @param type
     * @param matchFilters
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> queryNoPage(String index, String type, Map<String, Object> matchFilters) throws Exception {
        Map<String, Object> result = esUtil.queryNoPage(index, type, toEsQueryBuilder(matchFilters), Constants.ES_SHOWFIELDS, Constants.FIELD_DATE_SORT);
        execResultData(result);
        return result;
    }

    /**
     * 查询条件转换
     *
     * @param matchFilters
     * @return
     */
    public ESQueryBuilder toEsQueryBuilder(Map<String, Object> matchFilters) {
        ESQueryBuilder esQueryBuilder = ESQueryBuilder.newEsBuilder();
        if (matchFilters.containsKey("beginTime") && matchFilters.containsKey("endTime")) {
            ConditionEnty conditionEnty = new ConditionEnty();
            conditionEnty.setField(Constants.FIELD_DATE);
            conditionEnty.setCondition(ConditionEnty.between);
            conditionEnty.setBetweenFrom(matchFilters.get("beginTime") + " 00:00:00");//大于等于开始日期
            conditionEnty.setBetweenTo(matchFilters.get("endTime") + " 23:59:59");//小于等于结束日期;
            esQueryBuilder.addInclude(conditionEnty);
        }
        if (matchFilters.containsKey("userIds")) {
            ConditionEnty conditionEnty = new ConditionEnty();
            conditionEnty.setField(Constants.FIELD_USER_ID);
            conditionEnty.setCondition(ConditionEnty.equal);
            conditionEnty.setValues((String[]) matchFilters.get("userIds"));//用戶id匹配;
            esQueryBuilder.addInclude(conditionEnty);
        }
        if (matchFilters.containsKey("systemIds")) {
            ConditionEnty conditionEnty = new ConditionEnty();
            conditionEnty.setField(Constants.FIELD_APPID);
            conditionEnty.setCondition(ConditionEnty.equal);
            conditionEnty.setValues((String[]) matchFilters.get("systemIds"));//系统id匹配;
            esQueryBuilder.addInclude(conditionEnty);
        }
        if (matchFilters.containsKey(Constants.DETAILTABNAME)) {
            String tab = matchFilters.get(Constants.DETAILTABNAME).toString();
            if (tab.equals(Constants.DETAILTAB[0])) {//只统计访问失败的（不含成功状态）
                ConditionEnty conditionEnty = new ConditionEnty();
                conditionEnty.setField(Constants.FIELD_STATUS);
                conditionEnty.setCondition(ConditionEnty.equal);
                conditionEnty.setValues(new String[]{"true", "200"});//不含成功状态
                esQueryBuilder.addExclude(conditionEnty);
            } else {//统计成功条件的
                ConditionEnty conditionEnty = new ConditionEnty();
                conditionEnty.setField(Constants.FIELD_STATUS);
                conditionEnty.setCondition(ConditionEnty.equal);
                conditionEnty.setValues(new String[]{"true", "200"});//成功状态;
                esQueryBuilder.addInclude(conditionEnty);
            }
        }

        ConditionEnty conditionEnty = new ConditionEnty();
        conditionEnty.setField(Constants.FIELD_AUTHTYPE);
        conditionEnty.setCondition(ConditionEnty.like);
        conditionEnty.setValue("logout");//过滤掉登出的日志记录
        esQueryBuilder.addExclude(conditionEnty);

        return esQueryBuilder;
    }

    @Override
    public String add(String index, String type, List<Map> list) throws Exception {
        String result = esUtil.add(index, type, list);
        return result;
    }

    @Override
    public String delete(String index, String type, String id) throws Exception {
        String result = esUtil.delete(index, type, id);
        return result;
    }

    @Override
    public boolean deleteIndex(String indexName) throws Exception {
        return esUtil.deleteIndex(indexName);
    }

    /***
     * 结果处理
     * @param result
     */
    public Map<String, Object> execResultData(Map<String, Object> result) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("content");
        for (Map<String, Object> map : list) {
            if (map.get("index").toString().indexOf(Constants.ES_INDEX_IDP) != -1) {
                /****解析userAgent内容，获得系统信息，浏览器信息**/
                //转成UserAgent对象
                UserAgent userAgent = UserAgent.parseUserAgentString(map.get(Constants.FIELD_BROWSER).toString());
                //获取系统信息
                OperatingSystem os = userAgent.getOperatingSystem();
                //系统名称
                String system = os.getName();
                //获取浏览器信息
                String browserName = userAgent.getBrowser().getName();//浏览器名称
                String browserVersion = userAgent.getBrowserVersion() == null ? "" : userAgent.getBrowserVersion().getVersion();//浏览器版本
                map.put("terminalType", system);
                map.put("browserType", browserName);
                map.put("browserVersion", browserVersion);
            }
        }

        return result;
    }


    @Override
    public Map<String, Object> transferGetData(String index, String type, String filed, boolean distinct) throws Exception {

        //得到一个Calendar的实例
        Calendar ca = Calendar.getInstance();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        //当前时间 前一天
        ca.setTime(new Date());
        Date date = ca.getTime();
        String today = sf.format(date);
        today = today.replace("-", ".");
        today = "login-idp-2019.03.22";//"login-idp-"+today;

        ca.add(Calendar.DATE, -1);
        date = ca.getTime();
        String yestday = sf.format(date);
        yestday = yestday.replace("-", ".");
        yestday = "login-idp-2019.03.21";
        ;//"login-idp-"+yestday;

        System.out.println(today + ":" + yestday);

        //今日访问次数index login-idp-2019.06.05
        String todayVisits = getDataByIndex(today, "doc", "userId.keyword", false);
        //今日访问人数index login-idp-2019.06.05
        String todayPerVisits = getDataByIndex(today, "doc", "userId.keyword", true);
        //今日访问ip数index login-idp-2019.06.05
        String todayIpVisits = getDataByIndex(today, "doc", "IDPIP.keyword", true);
        //今日访问失败数index login-idp-2019.06.05
        String todayErrVisits = getDataByIndex(today, "doc", "authStatus.keyword", false);

        //昨日访问次数index login-idp-2019.06.05
        String yesterdayVisits = getDataByIndex(yestday, "doc", "userId.keyword", false);
        //昨日访问人数index login-idp-2019.06.05
        String yesterdayPerVisits = getDataByIndex(yestday, "doc", "userId.keyword", true);
        //昨日访问ip数index login-idp-2019.06.05
        String yesterdayIpVisits = getDataByIndex(yestday, "doc", "IDPIP.keyword", true);
        //昨日访问失败数index login-idp-2019.06.05
        String yesterdayErrVisits = getDataByIndex(yestday, "doc", "authStatus.keyword", false);

        //总访问次数index login-idp-2019.06.05
        String Visits = getDataByIndex("login-idp", "doc", "userId.keyword", false);
        //总访问人数index login-idp-2019.06.05
        String PerVisits = getDataByIndex("login-idp", "doc", "userId.keyword", true);
        //总访问ip数index login-idp-2019.06.05
        String ipVisits = getDataByIndex("login-idp", "doc", "IDPIP.keyword", true);
        //总访问失败数index login-idp-2019.06.05
        String errVisits = getDataByIndex("login-idp", "doc", "authStatus.keyword", false);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("today", todayVisits);
        map.put("yesterday", yesterdayVisits);
        map.put("total", Visits);
        List<Map> list = new ArrayList<>();
        list.add(map);

        Map<String, Object> map1 = new HashMap<String, Object>();
        map1.put("today", todayPerVisits);
        map1.put("yesterday", yesterdayPerVisits);
        map1.put("total", PerVisits);
        List<Map> list1 = new ArrayList<>();
        list1.add(map1);

        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put("today", todayIpVisits);
        map2.put("yesterday", yesterdayIpVisits);
        map2.put("total", ipVisits);
        List<Map> list2 = new ArrayList<>();
        list2.add(map2);

        Map<String, Object> map3 = new HashMap<String, Object>();
        map3.put("today", todayErrVisits);
        map3.put("yesterday", yesterdayErrVisits);
        map3.put("total", errVisits);
        List<Map> list3 = new ArrayList<>();
        list3.add(map3);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("visits", list);
        result.put("perVisits", list1);
        result.put("ipVisits", list2);
        result.put("errVisits", list3);

        return result;
    }


    /**
     * 访问次数 人数 ip 失败
     *
     * @param index
     * @param type
     * @param filed
     * @param distinct
     * @return
     * @throws Exception
     */
    @Override
    public String getDataByIndex(String index, String type, String filed, boolean distinct) throws Exception {
        Client client = esUtil.getInstance();
        String visitCount = "";
        AggregationBuilder agg = null;
        SearchResponse searchResponse = null;
        //模糊匹配索引
        List<String> indexArr = new ArrayList<>();
        String[] esIndexArr = client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData().getConcreteAllIndices();

        for (int i = 0; i < esIndexArr.length; i++) {
            //for (int j = 0; j < index.length; j++) {
            if (StringUtils.contains(esIndexArr[i], index)) {
                indexArr.add(esIndexArr[i]);
            }
            //}
        }
        //是否进行去重查询;
        if (distinct) {
            agg = AggregationBuilders.cardinality("count").field(filed);
            String[] s = indexArr.toArray(new String[]{});
            searchResponse = client.prepareSearch(s).setTypes(type).addAggregation(agg).get();
            InternalCardinality internalCardinality = searchResponse.getAggregations().get("count");
            visitCount = String.valueOf(internalCardinality.getValue());
        } else {
            agg = AggregationBuilders.count("count").field(filed);
            if (StringUtils.contains(filed,"authStatus")){
                Double errNum = new Double(0);

                TermQueryBuilder authStatus = QueryBuilders.termQuery("authStatus", false);

                searchResponse = client.prepareSearch(indexArr.toArray(new String[]{})).setTypes(type).setSize(0).setQuery(authStatus).addAggregation(agg).get();//.setFetchSource("authStatus","")
                ValueCount valueCount = searchResponse.getAggregations().get("count");
                visitCount = String.valueOf(valueCount.getValue());

                System.out.println("认证："+visitCount);
            }else{
                searchResponse = client.prepareSearch(indexArr.toArray(new String[]{})).setTypes(type).setSize(10000).addAggregation(agg).get();
                ValueCount valueCount = searchResponse.getAggregations().get("count");
                visitCount = String.valueOf(valueCount.getValue());
            }
        }
        System.out.println("结果：-------------------------" + visitCount);
        return visitCount;

    }

    /**
     * 访问趋势
     *
     * @param
     * @param
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> getAccessTrend() throws Exception {
        Client client = esUtil.getInstance();

        String time = new SimpleDateFormat("yyyy.MM.dd").format(new Date());

        String indexName = "login-idp-"+time;
        indexName = "login-idp-2019.03.22";
        String typeName = "doc";


        AbstractAggregationBuilder aggregation = AggregationBuilders.dateHistogram("group_by_hour")
            .field("date_sort")
            .dateHistogramInterval(DateHistogramInterval.hours(1));

        SearchResponse resp = client.prepareSearch(indexName)
                .setTypes(typeName)
                .addAggregation(aggregation).addSort("date_sort",SortOrder.ASC).execute().actionGet();

        JSONObject jsonObject = JSONObject.parseObject(resp.toString());
        System.out.println("信息："+jsonObject);

        JSONObject aggregations = JSONObject.parseObject(jsonObject.getString("aggregations"));
        JSONObject group_by_hour = JSONObject.parseObject(aggregations.getString("group_by_hour"));

        Map<String,Object> result = new HashMap<>();
        result.put("group_by_hour",group_by_hour);
        return result;
    }

    /**
     * 访问失败维度统计
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> getErrAccessCount() throws Exception {

        Client client = esUtil.getInstance();

        String time = new SimpleDateFormat("yyyy.MM.dd").format(new Date());

        String indexName = "login-idp-"+time;
        indexName = "login-idp-2019.04.02";
        String typeName = "doc";
        //根据 userId 去重
        //AggregationBuilder agg2 = AggregationBuilders.cardinality("errCount").field("userId.keyword");

        //根据appid 分组
        AggregationBuilder agg=  AggregationBuilders
                .terms("group_err_count")
                .field("appId.keyword")
                .order(BucketOrder.count(false)).size(10);

        //统计所有的失败数据
        TermQueryBuilder authStatus = QueryBuilders.termQuery("authStatus", false);

        SearchResponse searchResponse = client
                .prepareSearch(indexName)
                .setTypes(typeName)
                .setSize(10000)
                .setQuery(authStatus)
                .setFetchSource("appId","")
                .addAggregation(agg)
                .get();

        JSONObject jsonObject = JSONObject.parseObject(searchResponse.toString());
        System.out.println("信息："+jsonObject);

        JSONObject aggregations = JSONObject.parseObject(jsonObject.getString("aggregations"));
        JSONObject group_err_count = JSONObject.parseObject(aggregations.getString("group_err_count"));
        JSONArray buckets = JSONArray.parseArray(group_err_count.getString("buckets"));

        //根据appid获取所有的appname
        String sysInfo = GetSysInfo.getInfo();
        System.out.println(sysInfo);

        JSONObject systemData = JSONObject.parseObject(sysInfo);
        JSONArray jsonArray = JSONArray.parseArray(systemData.getString("data"));
        for (Object bucket : buckets) {
            JSONObject o = (JSONObject) bucket;
            String key = o.getString("key");
            for (Object sys : jsonArray) {
                JSONObject sys1 = (JSONObject) sys;
                String sysId = sys1.getString("systemid");
                String sysName = sys1.getString("systemname");

                if(key.equals(sysId)){
                    o.put("keyName",sysName);
                    break;
                }else{
                    o.put("keyName",key);
                }
            }
        }
        Map<String,Object> result = new HashMap<>();
        result.put("group_by_hour",buckets);
        return result;
    }


//    public Map<String, Object> queryData(String[] index, String type, String filed, boolean distinct) throws Exception {
//        Client client = esUtil.getInstance();
//        SearchResponse searchResponse = null;
//        if (distinct) {
//
//            AggregationBuilder agg = AggregationBuilders.cardinality("allCount").field(filed);
//            searchResponse = client.prepareSearch(index).setTypes(type).setSize(10000).addAggregation(agg).get();
//            InternalCardinality internalCardinality = searchResponse.getAggregations().get("allCount");
//            String count = String.valueOf(internalCardinality.getValue());
//            System.out.println("结果：-------------------------" + count);
//        } else {
//            SearchRequestBuilder srb = client.prepareSearch(index).setTypes(type).setFrom(0).setSize(10000);
//            searchResponse = srb.setQuery(QueryBuilders.matchAllQuery()).execute().actionGet(); // 查询所有
//        }
//
//        SearchHits hits = searchResponse.getHits();
//        Map<String, Object> resultMap = new HashMap<String, Object>();
//        resultMap.put("count", hits.totalHits);
//
//        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
//
//        SearchHit[] hit = hits.getHits();
//
//        for (SearchHit hitObj : hit) {
//
//            String id = hitObj.getId();
//            String esIndex = hitObj.getIndex();
//
//            JSONObject jsonObject = JSONObject.parseObject(String.valueOf(hitObj));
//
//            Map<String, Object> hitResult = new HashMap<String, Object>();
//            hitResult.put("id", id);
//            hitResult.put("esIndex", esIndex);
//            result.add(hitResult);
//        }
//        resultMap.put("hit", result);
//
//        return resultMap;
//
//    }


//    public static void main(String[] args) {
//        String a = "{\"buckets\":[\n" +
//                "                {\n" +
//                "                    \"doc_count\":13,\n" +
//                "                    \"key\":\"ctghub\"\n" +
//                "                },\n" +
//                "                {\n" +
//                "                    \"doc_count\":2,\n" +
//                "                    \"key\":\"oauthTest\"\n" +
//                "                }\n" +
//                "            ]}";
//        JSONObject c =  JSONObject.parseObject(a);
//        JSONArray b = (JSONArray) JSONArray.parse(c.getString("buckets"));
//        System.out.println(b);
//        for (Object o : b) {
//            JSONObject o1 = (JSONObject) o;
//            String key = o1.getString("key");
//            if(key.equals("ctghub")){
//                o1.put("keyName","门户系统-PC");
//            }else if(key.equals("oauthTest")){
//                o1.put("keyName","门户系统daye");
//            }
//        }
//        System.out.println("b+: "+b);
//    }

}


