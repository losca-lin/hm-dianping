package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOPList_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryTypeList() {
        String key = CACHE_SHOPList_KEY;
        Long size = stringRedisTemplate.opsForList().size(key);
        List<String> range = stringRedisTemplate.opsForList().range(key, 0, size-1);
        List<ShopType> shopTypeList = new ArrayList<>();
        for (String s : range) {
            ShopType shopType = JSONUtil.toBean(s, ShopType.class);
            shopTypeList.add(shopType);
        }
        if (!shopTypeList.isEmpty()){
            return Result.ok(shopTypeList);
        }
        List<ShopType> shopTypeList2 = this.list();
        if (shopTypeList2 == null){
            return Result.fail("没有数据");
        }
        for (ShopType shopType : shopTypeList2) {
            String shopTypeJson = JSONUtil.toJsonStr(shopType);
            stringRedisTemplate.opsForList().leftPush(key, shopTypeJson);
        }
        return Result.ok(shopTypeList2);
    }
}
