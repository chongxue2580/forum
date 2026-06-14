package com.example.blog_froum.mapper;

import com.example.blog_froum.entity.User;
import com.example.blog_froum.enums.UserRole;
import com.example.blog_froum.enums.UserStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    /**
     * 根据ID查询用户
     */
    User findById(@Param("id") Long id);

    /**
     * 根据用户名查询用户
     */
    User findByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     */
    User findByEmail(@Param("email") String email);

    /**
     * 根据用户名或邮箱查询用户
     */
    User findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    /**
     * 插入用户
     */
    int insert(User user);

    /**
     * 更新用户信息
     */
    int update(User user);

    /**
     * 更新用户登录信息
     */
    int updateLoginInfo(User user);

    /**
     * 更新用户状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") UserStatus status);

    /**
     * 更新用户角色
     */
    int updateRole(@Param("id") Long id, @Param("role") UserRole role);

    /**
     * 删除用户
     */
    int deleteById(@Param("id") Long id);

    /**
     * 查询所有用户（分页）
     */
    List<User> findAll(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 根据状态查询用户（分页）
     */
    List<User> findByStatus(@Param("status") UserStatus status, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 根据角色查询用户（分页）
     */
    List<User> findByRole(@Param("role") UserRole role, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计用户总数
     */
    long countAll();

    /**
     * 根据状态统计用户数
     */
    long countByStatus(@Param("status") UserStatus status);

    /**
     * 根据角色统计用户数
     */
    long countByRole(@Param("role") UserRole role);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(@Param("email") String email);

    /**
     * 更新用户密码
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /**
     * 更新用户两步验证设置
     */
    int updateTwoFactor(@Param("id") Long id,
                        @Param("enabled") Boolean enabled,
                        @Param("secret") String secret);

    // 管理员功能需要的方法
    /**
     * 根据角色和状态查询用户
     */
    List<User> findByRoleAndStatus(@Param("role") UserRole role, @Param("status") UserStatus status,
                                   @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 根据角色和状态统计用户数
     */
    long countByRoleAndStatus(@Param("role") UserRole role, @Param("status") UserStatus status);

    /**
     * 统计指定时间范围内注册的用户数
     */
    long countCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计指定时间范围内登录过的用户数
     */
    long countLastLoginBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 搜索用户（按用户名、昵称、邮箱）
     */
    List<User> searchUsers(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计搜索结果数量
     */
    long countSearchUsers(@Param("keyword") String keyword);

    /**
     * 获取最近注册的用户
     */
    List<User> findRecentUsers(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 删除用户
     */

}
