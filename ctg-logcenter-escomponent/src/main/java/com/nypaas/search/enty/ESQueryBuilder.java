package com.nypaas.search.enty;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询条件构建
 */
public class ESQueryBuilder {
   private  List<ConditionEnty> includeConditions=new ArrayList<>();
   private  List<ConditionEnty>excludeConditions=new ArrayList<>();

public static ESQueryBuilder newEsBuilder(){
    return new ESQueryBuilder();
}
public  List<ConditionEnty> addInclude(ConditionEnty conditionEnty){
    getIncludeConditions().add(conditionEnty);
    return includeConditions;
}
    public  List<ConditionEnty> addExclude(ConditionEnty conditionEnty){
        excludeConditions.add(conditionEnty);
        return excludeConditions;
    }

    public  List<ConditionEnty> getIncludeConditions() {
        return includeConditions;
    }
    public  List<ConditionEnty> getExcludeConditions() {
        return excludeConditions;
    }
}


