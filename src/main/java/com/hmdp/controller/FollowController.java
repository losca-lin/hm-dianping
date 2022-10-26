package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    @GetMapping("/or/not/{id}")
    public Result isFollowed(@PathVariable("id") Long followUserId) {
        return followService.isFollowed(followUserId);
    }

    @PutMapping("/{id}/{isFollowed}")
    public Result follow(@PathVariable("id") Long followUserId
            , @PathVariable("isFollowed") Boolean isFollowed
    ) {
        return followService.follow(followUserId, isFollowed);
    }

    @GetMapping("/common/{isFollowedId}")
    public Result commonFriends(@PathVariable("isFollowedId") Long isFollowedId) {
        return followService.commonFriends(isFollowedId);
    }


}
