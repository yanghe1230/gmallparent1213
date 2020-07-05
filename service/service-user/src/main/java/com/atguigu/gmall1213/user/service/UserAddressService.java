package com.atguigu.gmall1213.user.service;

import com.atguigu.gmall1213.model.user.UserAddress;

import java.util.List;

public interface UserAddressService {

    // 业务接口  select * from user_address where user_id = ?
    List<UserAddress> findUserAddressListByUserId(String userId);
}
