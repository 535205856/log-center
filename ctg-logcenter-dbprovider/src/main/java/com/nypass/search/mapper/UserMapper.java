package com.nypass.search.mapper;


import com.nypass.search.enty.UserEnty;

import java.util.List;
import java.util.Map;

public interface UserMapper {

    List<UserEnty> findUserByName(String name);
    List<UserEnty> findUserByNames(Map map);
    List findUser();

}
