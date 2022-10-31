package com.hmdp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisIDWorker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
@RunWith(SpringRunner.class)
public class HmDianPingApplicationTests {
    @Resource
    RedisIDWorker redisIDWorker;
    @Resource
    private IShopService shopService;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    long id = redisIDWorker.nextId("order");
                    System.out.println(id);
                }
                latch.countDown();
            }
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time=" + (end - begin));
    }

    @Test
    public void loadShopData() {
        //查询店铺信息
        List<Shop> shopList = shopService.list();
        //把店铺分组，按照typeId分组
        Map<Long, List<Shop>> map = shopList.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        //分批完成写入redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            for (Shop shop : value) {
                //stringRedisTemplate.opsForGeo().add(key,)
                locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString()
                         , new Point(shop.getX(), shop.getY())));

            }

            stringRedisTemplate.opsForGeo().add(key, locations);
        }

    }
    
    @Test
    public void testHyperLogLog(){
        String[] values = new String[1000];
        int j = 0;
        for (int i = 0; i < 100_0000; i++) {
            j = i%1000;
            values[j] = "user_"+i;
            if (j == 999){
                stringRedisTemplate.opsForHyperLogLog().add("h12",values);
            }
        }
    //   统计数量
        Long count = stringRedisTemplate.opsForHyperLogLog().size("h12");
        System.out.println("count="+count);
    }

}
