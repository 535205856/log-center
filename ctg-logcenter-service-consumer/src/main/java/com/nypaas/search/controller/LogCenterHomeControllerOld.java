package com.nypaas.search.controller;

import com.nypaas.search.service.CtgLogServiceOld;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/log/home")
@Api(value = "/log/home", description = "登录日志-主页数据查询")
public class LogCenterHomeControllerOld extends BaseController {
    @Autowired
    CtgLogServiceOld logService;

   final String matchFilterParaExam="查询参数 如： {\"beginTime\":\"2019-02-27\",\"endTime\":\"2019-02-28\",\"systemName\":\"\",\"orgName\":\"\"}";

    @ApiOperation(value = "数据总览", notes = "数据总览")
    @RequestMapping(value = "/overview", method = {RequestMethod.GET})
    public Map<String, Object> homeOverview() {
        try {
            return successResult(logService.homeOverview());
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    @RequestMapping(value = "/trendsInVisits", method = {RequestMethod.POST})
    @ApiOperation(value = "访问趋势", notes = "访问趋势")
    public Map<String, Object> trendsInVisits(@ApiParam(name = "matchFilters", value = matchFilterParaExam) @RequestBody Map<String, Object> matchFilters) {
        try {
            return successResult(logService.trendsInVisits(matchFilters));
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }
    @RequestMapping(value = "/terminalDistribution", method = {RequestMethod.POST})
    @ApiOperation(value = "终端分布", notes = "终端分布")
    public Map<String, Object> terminalDistribution(@ApiParam(name = "matchFilters", value = matchFilterParaExam) @RequestBody Map<String, Object> matchFilters) {
        try {
            return successResult(logService.terminalDistribution(matchFilters));
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    @RequestMapping(value = "/browserDistribution", method = {RequestMethod.POST})
    @ApiOperation(value = "浏览器分布", notes = "浏览器分布")
    public Map<String, Object> browserDistribution(@ApiParam(name = "matchFilters", value = matchFilterParaExam) @RequestBody Map<String, Object> matchFilters) {
        try {
            return successResult(logService.browserDistribution(matchFilters));
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    @ApiOperation(value = "内容区查询", notes = "从ES中检索数据，带分页")
    @RequestMapping(value = "/query", method = {RequestMethod.POST})
    Map<String, Object> query(@ApiParam(name = "curpage", value = "当前页") @RequestParam("curpage") Integer curpage, @ApiParam(name = "pagesize", value = "每页显示行数") @RequestParam("pagesize") Integer pagesize, @ApiParam(name = "matchFilters", value = matchFilterParaExam) @RequestBody Map<String, Object> matchFilters) {
        try {
            return successResult(logService.query(curpage, pagesize, matchFilters));
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }
}