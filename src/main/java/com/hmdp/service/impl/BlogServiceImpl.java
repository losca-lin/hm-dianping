package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("该用户笔记为空");
        }
        queryBlogUser(blog);
        //查询blog是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if (user == null){
            return;
        }
        //获取登录用户
        Long userId = user.getId();
        String key = BLOG_LIKED_KEY + blog.getId();
        //判断是否点赞
        Double isMember = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike( isMember != null );
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        //查询是否点赞
        records.forEach(blog -> {
                    queryBlogUser(blog);
                    isBlogLiked(blog);

                }
        );

        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {
        //获取登录用户
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY + id;
        //判断是否点赞
        Double isMember = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if ( isMember == null) {
            //如果未点赞，可以点赞
            //数据库点赞数+1
            boolean isSuccess = update().setSql("liked = liked+1").eq("id", id).update();
            if (isSuccess) {
                //保存用户到redis的set集合
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }

        } else {
            boolean isSuccess = update().setSql("liked = liked-1").eq("id", id).update();
            if (isSuccess) {
                //保存用户到redis的set集合
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        //查询点赞前5的用户
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5.isEmpty() || top5 == null) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = top5.stream().map(s -> Long.valueOf(s))
                .collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        List<User> users = userService.query().in("id", ids).last("order by field(id," + idStr + ")").list();
        List<UserDTO> userDTOList = users.stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOList);

    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
