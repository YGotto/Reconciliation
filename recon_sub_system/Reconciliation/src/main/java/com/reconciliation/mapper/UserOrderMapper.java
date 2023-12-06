package com.reconciliation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reconciliation.entity.TransactionFlow;
import com.reconciliation.entity.UserOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserOrderMapper extends BaseMapper<UserOrder> {
    @Select("select order_id,buyer_id,seller_id,total_price,item_id,order_id,complaint_record,order_state \n" +
            "from user_order where buyer_id = #{id} or seller_id = #{id};")
    List<UserOrder> getAllById(Integer id);
    @Select("select * from transaction_flow where order_id = #{orderId}")
    List<TransactionFlow> getFlowsByOrderId(Integer orderId);
}
