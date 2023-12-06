package com.reconciliation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@TableName("user")
@Data
public class User {
    private Integer id;
    private String userName;
    private String userPassword;
    private Integer type;
    private String name;
    private Integer vipValue;
    private String realName;
    private String idCard;
    private String phoneNumber;
    private String email;
    private Integer verificationStatus;
    private String paymentPassword;
    private double balance;
}
