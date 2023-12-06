package com.reconciliation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.reconciliation.entity.*;
import com.reconciliation.mapper.*;
import com.reconciliation.service.DataPreprocessService;
import com.reconciliation.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataPreprocessServiceImpl implements DataPreprocessService {
    @Autowired
    private UserMapper user_mapper;
    @Autowired
    private TransactionFlowMapper trans_mapper;
    @Autowired
    private RechargeMapper recharge_mapper;
    @Autowired
    private UserOrderMapper user_order_mapper;
    @Autowired
    private UserAccountService user_account_service;
    @Autowired
    private UserAccountMapper user_account_mapper;
    private List<User> loadBalance()
    {
        List<User> user;
        user = user_mapper.selectList(null);
        return user;
    }

    @Override
    public HashMap<Integer, Double> loadRechargeRecords(String date)
    {
        HashMap<Integer,Double> map = new HashMap<>();
        QueryWrapper<Recharge> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("user_id,SUM(amount) as amount").groupBy("user_id").eq("DATE(date)",date);
        List<Recharge> recharges = recharge_mapper.selectList(queryWrapper);
        for(Recharge recharge: recharges) {
            map.put(recharge.getUserId(),recharge.getAmount());
        }
        return map;
    }

    private HashMap<Integer,UserOrder> loadOrderState()
    {
        HashMap<Integer, UserOrder> map = new HashMap<>();
        List<UserOrder> userOrders;
        QueryWrapper<UserOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("order_id,buyer_id,seller_id,total_price,item_id,order_state");
        userOrders = user_order_mapper.selectList(queryWrapper);
        for (UserOrder t : userOrders) {
            map.put(t.getOrderId(), t);
        }
        return map;
    }

    private HashMap<Integer,UserAccount> loadUserAccountBackup(String date) {
        HashMap<Integer,UserAccount> balance_map = new HashMap<>();
        QueryWrapper<UserAccount> queryWrapper1 = new QueryWrapper<>();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(sf.parse(date));
        } catch (Exception e) {e.printStackTrace();return null;}
        c.add(Calendar.DATE, -1);
        queryWrapper1.eq("date",sf.format(c.getTime()));
        List<UserAccount> balance_backups = user_account_mapper.selectList(queryWrapper1);
        for(UserAccount userAccount: balance_backups) {
            balance_map.put(userAccount.getUserId(),userAccount);
        }
        return balance_map;
    }

    //    @Scheduled(cron ="*/5 * * * * ?")
    @Scheduled(cron ="59 59 23 * * ?")
    private void autoProcess() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        System.out.println(dateFormat.format(calendar.getTime()));
        if(calendar.get(Calendar.HOUR) == 0)
            saveUserBalances(dateFormat.format(calendar.getTime()));
        else {
            calendar.add(Calendar.DATE,1);
            saveUserBalances(dateFormat.format(calendar.getTime()));
        }
    }
    public void saveUserBalances(String date)
        /* 日期格式： yyyy-MM-dd */
    {
        List<User> users = loadBalance();
        HashMap<Integer, Double> recharges = loadRechargeRecords(date);

        HashMap<Integer,Double> cutoff_map = new HashMap<>();
        QueryWrapper<Recharge> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("user_id,SUM(amount) as amount").groupBy("user_id").eq("DATE(date)",date).between("TIME(date)","00:00:00","00:59:59");
        List<Recharge> cutoff_recharge = recharge_mapper.selectList(queryWrapper);
        for(Recharge recharge: cutoff_recharge) {
            cutoff_map.put(recharge.getUserId(),recharge.getAmount());
        }

        HashMap<Integer,UserAccount> balance_map = loadUserAccountBackup(date);
        if(balance_map == null) balance_map = new HashMap<>();

        List<UserAccount> userAccounts = new ArrayList<>();
        for (User value : users) {
            UserAccount user = new UserAccount();
            user.setDate(date);
            user.setBalance(value.getBalance());
            user.setUserId(value.getId());
            user.setCutoff(null);
            user.setState(0);
            if(!recharges.containsKey(value.getId())) //今日无充值记录
                user.setCharge(0);
            else if(!balance_map.containsKey(value.getId())) //前一日无记录
                user.setCharge(recharges.get(value.getId()));
            else if(balance_map.get(value.getId()).getCutoff() == null) //前日无日切
                user.setCharge(recharges.get(value.getId()));
            else if(!cutoff_map.containsKey(value.getId())) //前日有日切但无日切期间充值记录
                user.setCharge(recharges.get(value.getId()));
            else //前日有日切且有日切期间充值记录
                user.setCharge(recharges.get(value.getId()) - cutoff_map.get(value.getId()));
            userAccounts.add(user);
        }
        user_account_service.insertAccountBackup(userAccounts);
    }

    @Override
    public List<UserAccount> getUserBalances(String date) {
        QueryWrapper<UserAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("date",date);
        return user_account_mapper.selectList(queryWrapper);
    }

    /*付款成功（3）：仅需放入一次
     * 交易失败（0）：需要检查该记录是否已经申请退款（从全Transaction进行检查），如果有，说明他是一次退款，需要按照退款进行处理
     * */
    @Override
    public List<AbnormalTransaction> fetchTransaction(String date)
    {
        QueryWrapper<TransactionFlow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DATE(create_time)",date).in("order_state",3,0);
        List<TransactionFlow> trans = trans_mapper.selectList(queryWrapper); /* 订单状态 */
        HashMap<Integer, UserOrder> user_ord = loadOrderState(); /* 订单信息 */
        List<AbnormalTransaction> abtrans = new ArrayList<>();
        for (TransactionFlow tran : trans) {
            AbnormalTransaction ab = new AbnormalTransaction();
            ab.setTransactionId(tran.getTransactionId());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); /* 获取完整的时间 */
            String format_time = tran.getCreateTime().format(formatter);
            ab.setTime(format_time);
            if (tran.getOrderState() == 3) /* 付款成功 */ {
                UserOrder userOrder = user_ord.get(tran.getOrderId());
                ab.setOrderId(userOrder.getOrderId());
                ab.setUserId(userOrder.getBuyerId());
                ab.setSellerId(userOrder.getSellerId());
                ab.setAmount(-1 * userOrder.getTotalPrice().doubleValue());
                abtrans.add(ab);
            } else if (tran.getOrderState() == 0) /* 订单失败 */ {
                QueryWrapper<TransactionFlow> queryWrapper1 = new QueryWrapper<>();
                queryWrapper1.le("DATE(create_time)", date).eq("order_state", 10).eq("order_id", tran.getOrderId());
                long rst = trans_mapper.selectCount(queryWrapper1);
                if (rst != 0) {
                    UserOrder userOrder = user_ord.get(tran.getOrderId());
                    ab.setOrderId(userOrder.getOrderId());
                    ab.setUserId(userOrder.getBuyerId());
                    ab.setSellerId(userOrder.getSellerId());
                    ab.setAmount(userOrder.getTotalPrice().doubleValue());
                    abtrans.add(ab);
                }
            }
        }
        return abtrans;
    }

    @Override
    public List<AbnormalTransaction> getTransaction(String date) {
        List<AbnormalTransaction> abnormalTransactions = fetchTransaction(date);
        HashMap<Integer,UserAccount> balance_map = loadUserAccountBackup(date);
        if(balance_map == null) balance_map = new HashMap<>();
        for(int i = 0; i < abnormalTransactions.size(); i++) {
            int user_id = abnormalTransactions.get(i).getUserId();
            if(balance_map.containsKey(user_id)) {
                if(balance_map.get(user_id).getCutoff() != null && abnormalTransactions.get(i).getTime().split(" ")[1].compareTo(balance_map.get(user_id).getCutoff())<0) {
                    abnormalTransactions.remove(i);
                    i--;
                }
            }
        }
        return abnormalTransactions;
    }
}
