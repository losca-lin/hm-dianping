package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    @Override
    public Result isFollowed(Long followUserId) {
        //获取登录用户
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result follow(Long followUserId, Boolean isFollowed) {
        String key = "follows:";
        Long userId = UserHolder.getUser().getId();
        if (isFollowed) {
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (isSuccess){
                stringRedisTemplate.opsForSet().add(key + userId, followUserId.toString());
            }
        }else {
            QueryWrapper<Follow> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId).eq("follow_user_id", followUserId);
            boolean isRemove = remove(wrapper);
            if (isRemove) {
                stringRedisTemplate.opsForSet().remove(key + userId, followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result commonFriends(Long isFollowedId) {
        Long userId = UserHolder.getUser().getId();
        String key = "follows:";
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key + userId, key + isFollowedId);
        if (intersect== null || intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = intersect
                .stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        List<UserDTO> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }

}
