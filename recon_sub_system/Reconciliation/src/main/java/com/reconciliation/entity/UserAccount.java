package com.reconciliation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("account_backup")
@Data
public class UserAccount {
    private int userId;
    private String date;
    private double balance;
    private int state;
    private String cutoff;
    private double charge;
}
