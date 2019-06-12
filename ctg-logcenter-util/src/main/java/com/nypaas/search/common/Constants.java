package com.nypaas.search.common;

public final class Constants {
    /**DevOps平台 租户ID*/
    public static final String TENANT_ID_DEV = "c87e2267-1001-4c70-bb2a-ab41f3b81aa3";

    /**DevOps平台 租户ID*/
    public static final String TENANT_ID_TEST = "c87e2267-1001-4c70-bb2a-ab41f3b81aa3";

    /**DevOps平台 租户ID*/
    public static final String TENANT_ID_STAGE = "c87e2267-1001-4c70-bb2a-ab41f3b81aa3";

    /**DevOps平台 租户ID*/
    public static final String TENANT_ID_ONLINE = "c87e2267-1001-4c70-bb2a-ab41f3b81aa3";

    /**
     * 服务名
     */
    public static final String SEARCH_SERVICE = "ctg-logcenter-service-provider";
    /**
     * Remote服务名
     */
    public static final String REMOTECALL_SEARCH_SERVICE = SEARCH_SERVICE + "@" + TENANT_ID_TEST;



    /**
     * ES索引信息
     */
    public static final String ES_INDEX_IDP = "login-idp-";//PC
    public static final String ES_INDEX_MOBILE = "login-mobile-";//移动
    public static final String ES_INDEX =ES_INDEX_IDP+"*,"+ES_INDEX_MOBILE+"*";//登陆日志存储
    public static final String ES_TYPE = "doc";
    /**
     * 认证日志中关键字段信息
     */
    public static final String FIELD_DATE_SORT = "date_sort";//日期排序字段
    public static final String FIELD_DATE = "authTime";//日期字段
    public static final String FIELD_USER_ID = "userId";//用户字段
    public static final String FIELD_USER_NAME = "userName";//用户姓名
    public static final String FIELD_IP = "terminalIp";//ip字段
    public static final String FIELD_STATUS = "authStatus";//认证状态
    public static final String FIELD_BROWSER = "vistorBrowser";//浏览器信息
    public static final String FIELD_AUTHTYPE= "authentype";
    public static final String FIELD_AUTHDETAIL= "authDetail";
    public static final String FIELD_APPID= "appId";
    public static final String FIELD_APPNAME= "appName";
    public static final String FIELD_ORGID= "orgId";
    public static final String FIELD_ORGNAME= "orgName";

    public static final String FIELD_TERMINALTYPE= "terminalType";//终端类型
    public static final String FIELD_BROWSERTYPE= "browserType";//浏览器类型

    public static final String[] ES_SHOWFIELDS = new String[]{ FIELD_DATE_SORT,FIELD_DATE,FIELD_USER_ID, FIELD_USER_NAME, FIELD_IP, FIELD_STATUS,FIELD_BROWSER,FIELD_AUTHTYPE,FIELD_TERMINALTYPE
    ,FIELD_BROWSERTYPE,FIELD_APPID,FIELD_APPNAME,FIELD_ORGID,FIELD_ORGNAME,FIELD_AUTHDETAIL};

    public static final String DETAILTABNAME= "detailTab";
    public static final String []DETAILTAB= new String[]{"tab_fail","tab_terminal","tab_browser","tab_system","tab_org"};//详情页页签

}




