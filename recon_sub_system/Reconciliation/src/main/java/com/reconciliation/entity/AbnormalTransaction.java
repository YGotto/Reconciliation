package com.reconciliation.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

@TableName("abnormal_transactions")
@Data
public class AbnormalTransaction {
    @TableId
    private int transactionId;
    private int userId;
    private int sellerId;
    private int orderId;
    private double amount;
    private String time;
}
