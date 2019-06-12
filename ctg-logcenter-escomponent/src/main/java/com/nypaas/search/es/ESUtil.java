package com.nypaas.search.es;

import com.nypaas.search.enty.ConditionEnty;
import com.nypaas.search.enty.ESQueryBuilder;
import com.nypaas.search.util.PropertiesUtil;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class ESUtil {
    private TransportClient instance = getInstance();

    public TransportClient getInstance() {
        if (instance == null) {
            try {
                // address
                Properties properties = PropertiesUtil.loadProperties(PropertiesUtil.DEFAULT_CONFIG);
                String esAddress = PropertiesUtil.getString(properties, "es.address");
                String clusterName = PropertiesUtil.getString(properties, "es.cluster.name");
                LoggerFactory.getLogger(ESUtil.class).info("esAddress:" + esAddress + "  clusterName:" + clusterName);
                Set<String> esAddressSet = new HashSet<String>(Arrays.asList(esAddress.split(",")));
                int i = 0;
                TransportAddress[] transportAddresses = new TransportAddress[esAddressSet.size()];
                for (String address : esAddressSet) {
                    String[] temp = address.split(":");
                    String host = temp[0];
                    int port = Integer.valueOf(temp[1]);
                    InetSocketAddress inetSocketAddress= new InetSocketAddress(InetAddress.getByName(host), port);
                    transportAddresses[i]=new TransportAddress(inetSocketAddress);
                    i++;
                }

                // Settings
                Settings settings = Settings.builder()
                        .put("cluster.name", clusterName)        // 设置集群名称：默认是elasticsearch
                        .put("client.transport.sniff", false)    // 客户端嗅探整个集群状态，把集群中的其他机器IP加入到客户端中
                        .build();

                // TransportClient
                instance = new PreBuiltTransportClient(settings).addTransportAddresses(transportAddresses);
            } catch (UnknownHostException e) {
                if (instance != null) {
                    instance.close();
                }
            }
        }
        return instance;
    }
//    public static RestHighLevelClient getInstance() {
//        if(client==null){
//            client = new RestHighLevelClient(RestClient.builder(
//                    new HttpHost("10.66.6.35", 9200, "http")));
//        }
//        return client;
//    }
    /**
     * 依据索引名称，文档类型，数据id，删除某一行数据
     *
     * @param index
     * @param type
     * @param docid
     * @return
     * @throws Exception
     */
    public String delete(String index, String type, String docid) throws Exception {
        BulkRequest request = new BulkRequest();
        request.add(new DeleteRequest(index, type, docid));
        BulkResponse bulkResponse = getInstance().bulk(request).get();
        if (bulkResponse != null) {
            for (BulkItemResponse bulkItemResponse : bulkResponse) {
                DocWriteResponse itemResponse = bulkItemResponse.getResponse();
                if (bulkItemResponse.getOpType() == DocWriteRequest.OpType.DELETE) {
                    DeleteResponse deleteResponse = (DeleteResponse) itemResponse;
                    System.out.println("delete success" + deleteResponse.status());
                }
            }
        }
        return "success";
    }

    /**
     * 依据索引名称删除索引，会清空该索引下所有数据
     *
     * @param indexName
     * @return
     * @throws Exception
     */
    public boolean deleteIndex(String indexName) throws Exception {
        AcknowledgedResponse dResponse = getInstance().admin().indices().prepareDelete(indexName).execute().actionGet();
        return dResponse.isAcknowledged();
    }

    /**
     * 创建索引，并初始化字段类型
     *
     * @param indexName
     * @param type
     */
    public void initIndex(String indexName, String type) {
        //   text, keyword, date, long, double, boolean or ip，integer
        String[] strFields = new String[]{"authentype", "authentypeDetail", "UserID", "orgFullpath", "displayname",
                "IDPName", "IDPIP", "vistorIP", "vistorBrowser", "SPID", "info", "autdesc", "taketime"};
        List<Map<String, String>> list = new ArrayList();
        //字符类型字段
        for (String field : strFields) {
            Map<String, String> map = new HashMap<>();
            map.put("field", field);
            map.put("type", "text");
            list.add(map);
        }
        Map<String, String> map = new HashMap<>();
        map.put("field", "autuDt");
        map.put("type", "date");
        list.add(map);
        map = new HashMap<>();
        map.put("field", "success");
        map.put("type", "boolean");
        list.add(map);
        this.createIndex(indexName, type, list);
    }

    /**
     * 创建索引，并指定字段类型
     *
     * @param indexName
     * @param type
     * @param list      key=field（字段名称）、type（date，text，integer）
     */
    public void createIndex(String indexName, String type, List<Map<String, String>> list) {
        XContentBuilder mapping = null;
        try {
            mapping = XContentFactory.jsonBuilder()
                    .startObject()//表示开始设置值
                    .startObject("properties");//设置只定义字段，不传参
//                        .startObject("date") //定义字段名
//                        .field("type", "date") //设置数据类型
//                        .field("format", "yyyy-MM-dd HH:mm:ss") //格式化
//                        .endObject()
//                        .startObject("loginuser")
//                        .field("type", "text")
//                        .endObject()
//                        .startObject("age")
//                        .field("type", "integer")
//                        .endObject()
            for (Map<String, String> map : list) {
                mapping.startObject(map.get("field"));//定义字段名
                mapping.field("type", map.get("type"));//设置数据类型
                if (map.get("type").equals("date")) {
                    mapping.field("format", "yyyy-MM-dd HH:mm:ss"); //格式化
                }
                mapping.endObject();
            }

            mapping.endObject().endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //创建索引
        CreateIndexRequestBuilder cib = getInstance().admin().indices().prepareCreate(indexName);
        cib.execute().actionGet();

        //添加索引映射
        PutMappingRequest mappingRequest = Requests
                .putMappingRequest(indexName).type(type)
                .source(mapping);
        getInstance().admin().indices().putMapping(mappingRequest).actionGet();
    }

    /**
     * 向指定索引文档中插入数据
     *
     * @param index
     * @param type
     * @param list
     * @return
     * @throws Exception
     */
    public String add(String index, String type, List<Map> list) throws Exception {
        BulkRequest request = new BulkRequest();
//        request.add(indexRequest .source(XContentType.JSON,"user", "bbb"));
        for (Map<String, Object> map : list) {
            IndexRequest indexRequest = new IndexRequest(index, type);
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            for (String key : map.keySet()) {
                builder.field(key, map.get(key));
            }
            builder.endObject();
            IndexRequest source = indexRequest.source(builder);
            request.add(source);
        }
        BulkResponse bulkResponse = getInstance().bulk(request).get();
        //4、处理响应
        if (bulkResponse != null) {
            for (BulkItemResponse bulkItemResponse : bulkResponse) {
                DocWriteResponse itemResponse = bulkItemResponse.getResponse();

                if (bulkItemResponse.getOpType() == DocWriteRequest.OpType.INDEX
                        || bulkItemResponse.getOpType() == DocWriteRequest.OpType.CREATE) {
                    IndexResponse indexResponse = (IndexResponse) itemResponse;
                    System.out.println("add success" + indexResponse.status());
                }
            }
        }
        return "add success";
    }


    /**
     * 查询条件转换
     *
     * @return
     */
    public BoolQueryBuilder toBoolQueryBuilder(ESQueryBuilder esQueryBuilder) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        List<ConditionEnty> includeConditions = esQueryBuilder.getIncludeConditions();
        /**
         * 等于条件
         */
        for (int i = 0; i < includeConditions.size(); i++) {
            ConditionEnty conditionEnty = includeConditions.get(i);
            switch (conditionEnty.getCondition()) {
                case ConditionEnty.equal://如果传入的值是数组类型，则直接取数组值，数组值优先级高于单赋值
                    if (conditionEnty.getValues() != null && conditionEnty.getValues().length > 0) {
                        boolQueryBuilder.must(QueryBuilders.termsQuery(conditionEnty.getField() + ".keyword", conditionEnty.getValues()));
                    } else {
                        boolQueryBuilder.must(QueryBuilders.termsQuery(conditionEnty.getField() + ".keyword", conditionEnty.getValue()));
                    }
                    break;
                case ConditionEnty.like:
                    if (conditionEnty.getValues() != null && conditionEnty.getValues().length > 0) {
                        for (String value : conditionEnty.getValues()) {
                            boolQueryBuilder.must(QueryBuilders.wildcardQuery(conditionEnty.getField() + ".keyword", "*" + value + "*"));//模糊匹配;
                        }
                    } else {
                        boolQueryBuilder.must(QueryBuilders.wildcardQuery(conditionEnty.getField() + ".keyword", "*" + conditionEnty.getValue() + "*"));//模糊匹配;
                    }

                    break;
                case ConditionEnty.between:
                    boolQueryBuilder.must(QueryBuilders.rangeQuery(conditionEnty.getField() + ".keyword").gte(conditionEnty.getBetweenFrom()).lte(conditionEnty.getBetweenTo()));//大于等于 //小于等于
                    break;
            }

        }

        /**
         * 不等于条件
         */
        List<ConditionEnty> excludeConditions = esQueryBuilder.getExcludeConditions();
        for (int i = 0; i < excludeConditions.size(); i++) {
            ConditionEnty conditionEnty = excludeConditions.get(i);
            switch (conditionEnty.getCondition()) {
                case ConditionEnty.equal:
                    if (conditionEnty.getValues() != null && conditionEnty.getValues().length > 0) {
                        boolQueryBuilder.mustNot(QueryBuilders.termsQuery(conditionEnty.getField() + ".keyword", conditionEnty.getValues()));
                    } else {
                        boolQueryBuilder.mustNot(QueryBuilders.termsQuery(conditionEnty.getField() + ".keyword", conditionEnty.getValue()));
                    }
                    break;
                case ConditionEnty.like:
                    if (conditionEnty.getValues() != null && conditionEnty.getValues().length > 0) {
                        for (String value : conditionEnty.getValues()) {
                            boolQueryBuilder.mustNot(QueryBuilders.wildcardQuery(conditionEnty.getField(), "*" + value + "*"));//模糊匹配;
                        }
                    } else {
                        boolQueryBuilder.mustNot(QueryBuilders.wildcardQuery(conditionEnty.getField(), "*" + conditionEnty.getValue() + "*"));//模糊匹配;
                    }
                    break;
                case ConditionEnty.between:
                    break;
            }
        }

        return boolQueryBuilder;
    }

    /**
     * 依据索引名称，和文档类型，查询相关数据
     *
     * @param index
     * @param type
     * @param offset
     * @param pagesize
     * @param esQueryBuilder
     * @return
     * @throws Exception
     */
    public HashMap<String, Object> pagedQuery(String index, String type, int offset, int pagesize, ESQueryBuilder esQueryBuilder, String[] includeFields, String sortField) throws Exception {
        SearchRequest searchRequest = new SearchRequest(index.split(","));
        searchRequest.searchType(SearchType.QUERY_THEN_FETCH);//查询类型
        searchRequest.types(type);

        HashMap<String, Object> resultMap = new HashMap<>();
        // 2、用SearchSourceBuilder来构造查询请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//        sourceBuilder.fetchSource(false);
        //设置返回哪些字段
//		String[] includeFields = new String[] {"user", "post_date"};
        String[] excludeFields = new String[]{"_type"};
        if (includeFields != null && includeFields.length > 0) {
            sourceBuilder.fetchSource(includeFields, excludeFields);
        }


//		sourceBuilder.query(QueryBuilders.termQuery("user", "bbb"));
        sourceBuilder.query(toBoolQueryBuilder(esQueryBuilder));//查询条件

        sourceBuilder.from(offset);//开始行号，从0开始
        sourceBuilder.size(pagesize);//显示行数
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        SortBuilder sort = SortBuilders.fieldSort(sortField).order(SortOrder.DESC);
        sourceBuilder.sort(sort);

        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = getInstance().search(searchRequest).get();

        RestStatus status = searchResponse.status();
        System.out.println(status);
        resultMap.put("status", status);

        //处理搜索命中文档结果
        SearchHits hits = searchResponse.getHits();

        long totalHits = hits.getTotalHits();
//        float maxScore = hits.getMaxScore();
        resultMap.put("totalHits", totalHits);

        SearchHit[] searchHits = hits.getHits();
        System.out.println("rows:" + totalHits);
        List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
        for (SearchHit hit : searchHits) {
            String id = hit.getId();
//            String type = hit.getType();
//            float score = hit.getScore();

            //取_source字段值
            String sourceAsString = hit.getSourceAsString(); //取成json串
//            System.out.println("json-------->:" + sourceAsString);
            Map<String, Object> sourceAsMap = hit.getSourceAsMap(); // 取成map对象
            sourceAsMap.put("id", id);
            sourceAsMap.put("index", hit.getIndex());
            resultList.add(sourceAsMap);
        }
        resultMap.put("content", resultList);

        return resultMap;
    }

    /**
     * 无分页查询数据
     *
     * @return
     * @throws Exception
     */
    public Map<String, Object> queryNoPage(String index, String type, ESQueryBuilder esQueryBuilder, String[] includeFields, String sortField) throws Exception {
        List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
        HashMap<String, Object> result = pagedQuery(index, type, 0, 1000, esQueryBuilder, includeFields, sortField);
        int totalHints = Integer.valueOf(result.get("totalHits").toString());
        if (totalHints > 1000) {//按1000行分页，如果大于1000行，则累加计算，如果小于等于1000行，则直接取值
            for (int i = 0; i < totalHints / 1000; i++) {
                HashMap<String, Object> result1 = pagedQuery(index, type, 1000 * i, 1000, esQueryBuilder, includeFields, sortField);
                List<Map<String, Object>> list1 = (List<Map<String, Object>>) result1.get("content");
                resultList.addAll(list1);
            }
            if (totalHints % 1000 != 0) {//取余数
                HashMap<String, Object> result2 = pagedQuery(index, type, (totalHints / 1000) * 1000, 1000, esQueryBuilder, includeFields, sortField);
                List<Map<String, Object>> list2 = (List<Map<String, Object>>) result2.get("content");
                resultList.addAll(list2);
            }
        } else {
            List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("content");
            resultList.addAll(list);
        }

        System.out.println("totalSize" + totalHints + "  contentsize: " + resultList.size());
        Map<String, Object> map = new HashMap();
        map.put("totalHints", totalHints);
        map.put("contentsize", resultList.size());
        map.put("content", resultList);
        return map;
    }
}