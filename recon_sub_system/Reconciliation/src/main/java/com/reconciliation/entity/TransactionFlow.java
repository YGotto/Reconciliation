package com.reconciliation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("transaction_flow")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class  TransactionFlow {
    private Integer transactionId;
    private Integer orderId;
    private Integer orderState;
    private LocalDateTime createTime;
//    private String orderStateString;

    public TransactionFlow(Integer orderId, Integer orderState, LocalDateTime createTime) {
        this.orderId = orderId;
        this.orderState = orderState;
        this.createTime = createTime;
    }
}
