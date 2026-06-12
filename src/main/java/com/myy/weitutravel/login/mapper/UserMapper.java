package com.myy.weitutravel.login.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myy.weitutravel.login.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE mobile = #{mobile}")
    User selectByMobile(@Param("mobile") String mobile);
}
