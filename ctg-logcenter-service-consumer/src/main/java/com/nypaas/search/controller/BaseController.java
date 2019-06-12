package com.nypaas.search.controller;

import java.util.HashMap;
import java.util.Map;
public class BaseController{

public Map<String,Object> successResult(Object obj){
    Map<String,Object> map=new HashMap();
    map.put("status","success");
    map.put("content",obj);
    return map;
}
    public Map<String,Object> errorResult(String msg){
        Map<String,Object> map=new HashMap();
        map.put("status","error");
        map.put("message",msg);
        return map;
    }
}