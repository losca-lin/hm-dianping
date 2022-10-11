package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIDWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private IVoucherService voucherService;
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIDWorker redisIDWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);
    private static final ExecutorService SECKILL_ORDER_HANDLER = Executors.newSingleThreadExecutor();

    //该类一初始化就执行
    @PostConstruct
    private void init(){
        SECKILL_ORDER_HANDLER.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while (true) {
                try {
                    VoucherOrder voucherOrder = orderTasks.take();
                    createVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常",e);
                }
            }
        }
    }

    private void createVoucherOrder(VoucherOrder voucherOrder) {
        Long voucherId = voucherOrder.getVoucherId();
        //一人一单
        Long userId = voucherOrder.getUserId();
        //创建锁对象
        //SimpleRedisLock redisLock = new SimpleRedisLock("order", stringRedisTemplate);
        //尝试获取锁
        //boolean isLock = redisLock.tryLock(1200);
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
        //默认 不等待 超过30秒自动释放
        boolean isLock = redisLock.tryLock();
        //获取锁失败
        if (!isLock) {
            log.error("不允许重复下单");
            return;
        }
        try {
            Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                log.error("每个用户仅限一单");
                return;
            }
            //扣减库存
            boolean success = seckillVoucherService.update().setSql("stock = stock -1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)
                    .update();
            if (!success) {
                log.error("库存不足");
                return;
            }
            this.save(voucherOrder);
        } finally {
            redisLock.unlock();
        }
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        //1 执行lua脚本
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(),
                voucherId.toString(), userId.toString());
        int r = result.intValue();
        //2 判断结果是否为0
        if (r!=0){
            //2.1 不为0代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        //3 返回订单id
        long orderId = redisIDWorker.nextId("order");
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(userId);
        orderTasks.add(voucherOrder);
        return Result.ok(orderId);
    }
   /* @Override
    public Result seckillVoucher(Long voucherId) {
        //查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        //判断秒杀是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        //判断秒杀是否结束
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束了");
        }
        //判断库存是否充足
        Integer stock = seckillVoucher.getStock();
        if (stock < 1) {
            return Result.fail("库存不足");
        }
        return createOrder(voucherId);
    }*/


    @Transactional
    public Result createOrder(Long voucherId) {
        //一人一单
        Long userId = UserHolder.getUser().getId();
        //创建锁对象
        //SimpleRedisLock redisLock = new SimpleRedisLock("order", stringRedisTemplate);
        //尝试获取锁
        //boolean isLock = redisLock.tryLock(1200);
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
        //默认 不等待 超过30秒自动释放
        boolean isLock = redisLock.tryLock();
        //获取锁失败
        if (!isLock) {
            return Result.fail("不允许重复下单");
        }
        long orderId;
        try {
            Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                return Result.fail("每个用户仅限一单");
            }
            //扣减库存
            boolean success = seckillVoucherService.update().setSql("stock = stock -1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)
                    .update();
            if (!success) {
                return Result.fail("库存不足");
            }
            //创建订单
            orderId = redisIDWorker.nextId("order");
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setId(orderId);
            voucherOrder.setVoucherId(voucherId);
            voucherOrder.setUserId(userId);
            this.save(voucherOrder);
            //返回订单号
            return Result.ok(orderId);
        } finally {
            redisLock.unlock();
        }

    }
  /*  @Transactional
    public Result createOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        //注意 synchronized里面锁的是对象，不能是单纯的变量 intern () 这个方法是从常量池中拿到数据，如果我们直接使用 userId.toString () 他拿到的对象实际上是不同的对象，new 出来的对象，我们使用锁必须保证锁必须是同一把，所以我们需要使用 intern () 方法
        synchronized (userId.toString().intern()) {
            //一人一单
            Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                return Result.fail("每个用户仅限一单");
            }
            //扣减库存
            boolean success = seckillVoucherService.update().setSql("stock = stock -1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)
                    .update();
            if (!success) {
                return Result.fail("库存不足");
            }
            //创建订单
            long orderId = redisIDWorker.nextId("order");
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setId(orderId);
            voucherOrder.setVoucherId(voucherId);
            voucherOrder.setUserId(userId);
            this.save(voucherOrder);
            //返回订单号
            return Result.ok(orderId);
        }
    }*/
}
