package com.nypaas.search.controller;

import com.nypaas.search.LogCenterSearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
@Api(value = "/search", description = "测试接口")
public class SearchControllerTest {
    @Autowired
    LogCenterSearchService imsSearchService;

    @ApiOperation(value = "测试", notes = "测试")
    @RequestMapping(value = "/test", method = {RequestMethod.GET})
    String test() {
        return imsSearchService.hello();
    }

    @ApiOperation(value = "数据新增", notes = "向ES插入数据")
    @RequestMapping(value = "/add", method = {RequestMethod.POST})
    String addData(String index, String type, @RequestBody List<Map> list) {
        try {

//            for (int i=0;i<1000;i++){
//                Map map=new HashMap();
//                map.put("autudt",  DateUtil.getDateTimeFormat(DateUtil.getDayAfterDays(i)));
//                map.put("userid","000"+i);
//                list.add(map);
//            }

            return  imsSearchService.add(index, type, list);
        } catch (Exception e) {
            return "error! "+e.getMessage();
        }
    }

    @ApiOperation(value = "数据删除", notes = "从ES中删除数据")
    @RequestMapping(value = "/delete", method = {RequestMethod.POST})
    String deleteData(String index, String type, String id) {
        try {
            return imsSearchService.delete(index, type, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @ApiOperation(value = "索引删除", notes = "从ES中删除指定索引以及该索引下全部数据")
    @RequestMapping(value = "/deleteIndex", method = {RequestMethod.POST})
    String deleteIndex(String indexName) {
        try {
            boolean result = imsSearchService.deleteIndex(indexName);
            return result?"删除成功！":"刪除失败！";
        } catch (Exception e) {
            return "error! "+e.getMessage();
        }
    }

    @ApiOperation(value = "数据查询", notes = "从ES中检索数据")
    @RequestMapping(value = "/query", method = {RequestMethod.POST})
    Map<String, Object> query(String index, String type, @ApiParam(name = "curpage", value = "当前页") @RequestParam("curpage") Integer curpage,@ApiParam(name = "pagesize", value = "每页显示行数") @RequestParam ("pagesize")Integer pagesize, @ApiParam(name = "matchFilters", value = "查询参数 如： {\"beginTime\":\"2019-02-27\",\"endTime\":\"2019-02-28\",\"userName\":\"\"}") @RequestBody Map<String, Object> matchFilters) {
        try {
            Map<String, Object> search = imsSearchService.pagedQuery(index,type, curpage, pagesize, matchFilters,new String[0]);
            return search;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @ApiOperation(value = "数据查询", notes = "从ES中检索数据-无分页返回")
    @RequestMapping(value = "/querynopage", method = {RequestMethod.POST})
    Map<String, Object> queryNoPage(String index, String type, @ApiParam(name = "matchFilters", value = "查询参数 如： {\"beginTime\":\"2019-02-27\",\"endTime\":\"2019-02-28\",\"userName\":\"\"}") @RequestBody Map<String, Object> matchFilters) {
        try {
            Map<String, Object> search = imsSearchService.queryNoPage(index,type,matchFilters);
            return search;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



}






