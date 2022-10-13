package com.hmdp.test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Losca
 * @date 2022/10/7 12:29
 */
public class DebugTest {
    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 7,6, 8);
        list.stream()
                .filter(x -> x > 5)
                .collect(Collectors.toList());

    }

    private static int print(String msg) {
        System.out.println(msg);
        System.out.println("数据库操作");
        return 0;
    }

    static class user{
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
