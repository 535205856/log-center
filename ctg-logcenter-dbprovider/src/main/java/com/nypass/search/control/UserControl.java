package com.nypass.search.control;

import com.nypass.search.enty.OrgEnty;
import com.nypass.search.mapper.OrgMapper;
import com.nypass.search.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class UserControl {
    @Autowired
    UserMapper userMapper;
    @Autowired
    OrgMapper orgMapper;

    @RequestMapping(value = "/user")
    @ResponseBody
    public Object query(){
        Map map=new HashMap();
        map.put("usernames",new String[]{"aa","测试"});
        List result=userMapper.findUserByNames(map);
        return "result:"+result;
    }
    @RequestMapping(value = "/org")
    @ResponseBody
    public Object findUser(){
        List<OrgEnty> result=orgMapper.findOrg();

List<Map> list=new ArrayList<>();
        for (OrgEnty enty:result){
            Map map1=new HashMap();
            Map<String,String> map=new HashMap<>();
        map.put("id",enty.getOrgid());
        map.put("pid",enty.getPartendid());
        map.put("title",enty.getOrgname());
        map1.put("status","nrm");
        map1.put("data",map);
            list.add(map1);
        }

        return list;
    }


}
