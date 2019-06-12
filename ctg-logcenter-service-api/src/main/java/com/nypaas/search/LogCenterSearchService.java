package com.nypaas.search;

import com.nypaas.search.common.Constants;
import com.yonyou.cloud.middleware.rpc.RemoteCall;

import java.util.List;
import java.util.Map;

@RemoteCall(Constants.REMOTECALL_SEARCH_SERVICE)
public interface LogCenterSearchService {
    /**
     * 测试
     * @return
     */
    String hello();

    /**
     * 查询数据（加载分页数据）
     * @param index
     * @param type
     * @param offset
     * @param pagesize
     * @param matchFilters
     * @param includeFields
     * @return
     * @throws Exception
     */
    Map<String, Object> pagedQuery(String index, String type, int offset, int pagesize, Map<String, Object> matchFilters, String[] includeFields) throws Exception;

    /**
     * 查询数据（加载所有数据，无分页）
     * @param index
     * @param type
     * @param matchFilters
     * @return
     * @throws Exception
     */
    Map<String, Object> queryNoPage(String index, String type, Map<String, Object> matchFilters) throws Exception;


    String add(String index, String type, List<Map> list) throws Exception;
    String delete(String index, String type, String id) throws Exception;
    boolean deleteIndex(String indexName) throws Exception;

    /**
     * 反复调用 getDataByIndex();
     * @param index
     * @param type
     * @param filed
     * @param distinct
     * @return
     * @throws Exception
     */
    Map<String, Object> transferGetData(String index, String type, String filed, boolean distinct) throws Exception;

    /**
     * 获取index 下的数据总量
     */
    String getDataByIndex(String index, String type, String filed, boolean distinct) throws Exception;


    /**
     * 访问趋势
     */
    Map<String,Object> getAccessTrend() throws Exception;

    /**
     * 访问失败维度
     */
    Map<String,Object> getErrAccessCount() throws Exception;


}
