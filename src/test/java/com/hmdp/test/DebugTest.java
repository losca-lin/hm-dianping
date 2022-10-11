package com.hmdp.test;

/**
 * @author Losca
 * @date 2022/10/7 12:29
 */
public class DebugTest {
    public static void main(String[] args) {
        int aaa = print("aaa");
        System.out.println(1);
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
