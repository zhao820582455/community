package com.zbtech.community.controller.api;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.service.UserFollowService;
import com.zbtech.community.service.UsersService;
import com.zbtech.community.vo.UserVO;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端用户模块（对齐原 foxbook api/controller/User）
 * 路由前缀 /api/user
 */
@RestController
@RequestMapping("/api/user")
public class UserApiController extends BaseController {

    private final UsersService usersService;
    private final UserFollowService userFollowService;

    public UserApiController(UsersService usersService, UserFollowService userFollowService) {
        this.usersService = usersService;
        this.userFollowService = userFollowService;
    }

    /** 当前登录用户资料 */
    @GetMapping("/whoami")
    public Result<UserVO> whoami() {
        return Result.success(usersService.getUserInfo(getUserId()));
    }

    /** 兼容前端 getInfo 调用 */
    @GetMapping("/getInfo")
    public Result<UserVO> getInfo() {
        return whoami();
    }

    /** 单字段更新（field 白名单） */
    @PostMapping("/updateProfile")
    public Result<?> updateProfile(@RequestParam String field, @RequestParam String value) {
        usersService.updateProfile(getUserId(), field, value);
        return Result.success();
    }

    /** 批量更新资料 */
    @PostMapping("/saveInfo")
    public Result<?> saveInfo(@RequestParam(required = false) String nickname,
                              @RequestParam(required = false) String avatar,
                              @RequestParam(required = false) String introduction,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String email) {
        usersService.saveInfo(getUserId(), nickname, avatar, introduction, phone, email);
        return Result.success();
    }

    @PostMapping("/follow")
    public Result<?> follow(@RequestParam Integer userId) {
        userFollowService.follow(getUserId(), userId);
        return Result.success();
    }

    @PostMapping("/unfollow")
    public Result<?> unfollow(@RequestParam Integer userId) {
        userFollowService.unfollow(getUserId(), userId);
        return Result.success();
    }

    /** 关注状态（未登录也可查，返回是否关注/互关） */
    @GetMapping("/checkFollowStatus")
    public Result<Map<String, Object>> checkFollowStatus(@RequestParam Integer userId) {
        Integer me = getUserIdOptional();
        Map<String, Object> m = new HashMap<>();
        boolean following = me != null && userFollowService.isFollowing(me, userId);
        boolean mutual = me != null && userFollowService.isMutualFollow(me, userId);
        m.put("is_following", following);
        m.put("is_mutual_follow", mutual);
        return Result.success(m);
    }

    /** 他人主页 */
    @GetMapping("/getUserById")
    public Result<UserVO> getUserById(@RequestParam Integer userId) {
        Integer me = getUserIdOptional();
        return Result.success(usersService.getUserById(me, userId));
    }

    /** 热门用户 */
    @GetMapping("/getHotUsers")
    public Result<List<UserVO>> getHotUsers() {
        return Result.success(usersService.getHotUsers());
    }
}
