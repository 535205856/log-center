package com.nypaas.search.controller;

import com.nypaas.search.es.ESUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Array;
import java.util.*;

//@RunWith(SpringRunner.class)
//@SpringBootTest
//@AutoConfigureMockMvc
public class SearchControllerTest {
//    @Autowired
//    private ESUtil esUtil;
//
//    @Before
//    public void before() {
//    }

    @Test
    public void testHome() throws Exception {
        int [] keyArray=new int[]{50,20,20};
        String[] valueArray=new String[]{"50a","20aa","100a"};
        for (int i = 0;i < keyArray.length - 1; i ++) {
            for (int j = i + 1;j < keyArray.length;j ++) {
                if (keyArray[j] < keyArray[i]) {
                    int temp = keyArray[i];
                    String tempValue = valueArray[i];
                    keyArray[i] = keyArray[j];
                    keyArray[j] = temp;
                    valueArray[i] = valueArray[j];
                    valueArray[j] = tempValue;
                }
            }
        }


        for (int i=0;i<valueArray.length;i++){
            System.out.println(valueArray[i]);
        }



    }


}
