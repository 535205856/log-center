package com.nypaas.search.controller;

import com.nypaas.search.LogCenterSearchService;
import com.nypaas.search.service.CtgLogDetailService;
import com.nypaas.search.service.CtgLogHomeService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/log-center")
@Api(value = "/log-center", description = "登录日志-主页数据查询")
public class LogCenterController extends BaseController {
    @Autowired
    CtgLogHomeService logHomeService;
    @Autowired
    CtgLogDetailService logDetailService;
    @Autowired
    LogCenterSearchService imsSearchService;

    @RequestMapping(value = "/home", method = {RequestMethod.POST})
    @ApiOperation(value = "总览", notes = "总览")
    public Map<String, Object> homeData() {
        try {
            return successResult(logHomeService.homeData());
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    final String matchFilterParaExam="查询参数 如：" +
            "{\n" +
            "\t\"normal\": {\n" +
            "\t\t\"time\": [\"2019-02-27\",\n" +
            "\t\t\t\"2019-02-28\"\n" +
            "\t\t],\n" +
            "\t\t\"systemIds\": [\"SYSTEM1\", \"SYSTEM2\"],\n" +
            "\t\t\"orgIds\": [\"ORG1\", \"ORG2\"],\n" +
            "\t\t\"userIds\": [\"USER1\", \"USER2\"],\n" +
            "\t\"detailTab\":\"tab_fail\"},\n" +
            "\t\"page\": {\n" +
            "\t\t\"currentpage\": 1,\n" +
            "\t\t\"pagesize\": 10\n" +
            "\t}\n" +
            "}";
    @RequestMapping(value = "/detail", method = {RequestMethod.POST})
    @ApiOperation(value = "统计详情", notes = "统计详情")
    public Map<String, Object> totalDetail(@ApiParam(name = "matchFilters", value = matchFilterParaExam) @RequestBody Map<String, Object> matchFilters) {
        try {
            return successResult(logDetailService.totalDetail(matchFilters));
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }


    @RequestMapping(value = "/visitCount", method = {RequestMethod.POST})
    @ApiOperation(value = "访问次数", notes = "访问次数")
    public Map<String, Object> getDataByIndex(String index, String type, String filed, boolean distinct) {
        try {
            //return imsSearchService.getDataByIndex(index,type,filed,distinct);
            return successResult(imsSearchService.transferGetData(index,type,filed,distinct));
        } catch (Exception e) {
            return null;
        }
    }

    @RequestMapping(value = "/accessTrend", method = {RequestMethod.POST})
    @ApiOperation(value = "访问趋势", notes = "访问趋势")
    public Map<String, Object> getAccessTrend() {
        try {
            return successResult(imsSearchService.getAccessTrend()) ;
        } catch (Exception e) {
            return null;
        }
    }
    @RequestMapping(value = "/accessErrCount", method = {RequestMethod.POST})
    @ApiOperation(value = "访问失败维度统计", notes = "访问失败维度统计")
    public Map<String, Object> getErrAccessCount() {
        try {
            return successResult(imsSearchService.getErrAccessCount()) ;
        } catch (Exception e) {
            return null;
        }
    }




}