package com.nypaas.search.enty;

/**
 * 查询条件实体
 */
public class ConditionEnty{
    public static final String like="like";//模糊查询
    public static final String equal="equal";//精确查询
    public static final String between="between";//区间查询
    private String field;
    private String condition;

    private String value;
    private String betweenFrom;
    private String betweenTo;

    public String[] getValues() {
        return values;
    }

    private String[] values;

    public String getBetweenFrom() {
        return betweenFrom;
    }

    public String getBetweenTo() {
        return betweenTo;
    }

    public void setValues(String[] values) {
        this.values = values;

    }
    public void setField(String field) {
        this.field = field;
    }

    public void setValue(String value) {
        this.value = value;
    }
    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getField() {
        return field;
    }

    public String getCondition() {
        return condition;
    }

    public String getValue() {
        return value;
    }

    public void setBetweenFrom(String betweenFrom) {

        this.betweenFrom = betweenFrom;
    }

    public void setBetweenTo(String betweenTo) {
        this.betweenTo = betweenTo;
    }

    ConditionEnty like(String field, String value){
    this.setField(field);
    this.setCondition(like);
    this.setValue(value);
    return this;
    }
    ConditionEnty equal(String field, String value){
        this.setField(field);
        this.setCondition(equal);
        this.setValue(value);
        return this;
    }
    ConditionEnty equal(String field, String []value){
        this.setField(field);
        this.setCondition(equal);
        this.setValues(value);
        return this;
    }
    ConditionEnty between(String field, String fromValue,String toValue){
        this.setField(field);
        this.setCondition(between);
        this.setBetweenFrom(fromValue);
        this.setBetweenTo(toValue);
        return this;
    }

}