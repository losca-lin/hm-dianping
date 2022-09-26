package com.hmdp.utils;

/**
 * @author Losca
 * @date 2022/9/26 12:51
 */
public interface ILock {
    boolean tryLock(long time);

    void unLock();
}
