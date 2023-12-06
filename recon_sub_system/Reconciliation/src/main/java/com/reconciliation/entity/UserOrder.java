package com.reconciliation.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@TableName("user_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrder {
    private Integer orderId;
    private Integer buyerId;
    private Integer sellerId;
    private BigDecimal totalPrice;
    private Integer itemId;
    private Integer orderState;
    private String complaintRecord;

    public UserOrder(Integer buyerId, Integer sellerId, BigDecimal totalPrice, Integer itemId, Integer orderState) {
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.totalPrice = totalPrice;
        this.itemId = itemId;
        this.orderState = orderState;

    }
}
