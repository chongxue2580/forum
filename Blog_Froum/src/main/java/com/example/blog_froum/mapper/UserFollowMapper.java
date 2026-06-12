package com.example.blog_froum.mapper;

import com.example.blog_froum.entity.User;
import com.example.blog_froum.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户关注Mapper接口
 */
@Mapper
public interface UserFollowMapper {

    /**
     * 添加关注关系
     */
    int insert(UserFollow userFollow);

    /**
     * 删除关注关系
     */
    int delete(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    /**
     * 查询关注关系是否存在
     */
    boolean exists(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    /**
     * 获取用户的关注者（粉丝）
     */
    List<User> findFollowers(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 获取用户关注的人
     */
    List<User> findFollowing(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计用户的关注者数量（粉丝数）
     */
    long countFollowers(@Param("userId") Long userId);

    /**
     * 统计用户关注的人数
     */
    long countFollowing(@Param("userId") Long userId);
} 