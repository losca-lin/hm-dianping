package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIDWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
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

    @Override
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
    }


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
