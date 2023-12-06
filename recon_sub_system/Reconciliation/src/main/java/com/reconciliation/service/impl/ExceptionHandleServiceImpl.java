package com.reconciliation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.reconciliation.entity.*;
import com.reconciliation.mapper.*;
import com.reconciliation.service.AbnormalTransactionService;
import com.reconciliation.service.ExceptionHandleService;
import com.reconciliation.service.ReconciliationService;
import com.reconciliation.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;

@Service
public class ExceptionHandleServiceImpl implements ExceptionHandleService {
    @Autowired
    private AbnormalTransactionMapper abnormalTransactionMapper;
    @Autowired
    private AbnormalTransactionService abnormalTransactionService;
    @Autowired
    private UserAccountMapper userAccountMapper;
    @Autowired
    private UserOrderMapper userOrderMapper;
    @Autowired
    private TransactionFlowMapper transactionFlowMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private ReconciliationService reconciliationService;
//    @Autowired
//    private ArbitrationService arbitrationService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editAccountBackupBalance(int user_id, String date, double val) {
        QueryWrapper<UserAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",user_id).ge("date",date);
        List<UserAccount> userAccounts = userAccountMapper.selectList(queryWrapper);
        if(userAccounts.isEmpty()) return false;
        for(UserAccount userAccount: userAccounts) {
            QueryWrapper<UserAccount> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("user_id",user_id).eq("date",userAccount.getDate());
            userAccount.setBalance(userAccount.getBalance()+val);
            userAccountMapper.update(userAccount,queryWrapper1);
        }
        if(!reconciliationForAccount(user_id,date)) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        } else {
            if(!confirmNormal(user_id,date)) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return false;
            }
            QueryWrapper<User> queryWrapper3 = new QueryWrapper<>();
            queryWrapper3.eq("id",user_id);
            User user = userMapper.selectOne(queryWrapper3);
            user.setBalance(user.getBalance() + val);
            userMapper.update(user,queryWrapper3);
            return true;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean freezeAccount(int user_id, String date, int time) {
        QueryWrapper<UserAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",user_id).ge("date",date);
        List<UserAccount> userAccounts = userAccountMapper.selectList(queryWrapper);
        if(userAccounts.isEmpty()) return false;
        for(UserAccount userAccount: userAccounts) {
            QueryWrapper<UserAccount> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("user_id",user_id).eq("date",userAccount.getDate());
            userAccount.setState(4);
            userAccountMapper.update(userAccount,queryWrapper1);
        }
//        ArbitrationInput arbitrationInput = new ArbitrationInput();
//        arbitrationInput.setUserid(user_id);
//        arbitrationInput.setArbitrationtime(time * 24 * 60 * 60 * 1000);
//        arbitrationService.AddArbitration(arbitrationInput);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmNormal(int user_id, String date) {
        try {
            abnormalTransactionService.deleteAbnormalTransactionByUserDate(user_id,date);
            QueryWrapper<UserAccount> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("user_id",user_id).eq("date",date);
            UserAccount userAccount = userAccountMapper.selectOne(queryWrapper2);
            userAccount.setState(3);
            userAccountMapper.update(userAccount,queryWrapper2);
            return true;
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reconciliationForAccount(int user_id, String date) {
        QueryWrapper<UserAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",user_id).eq("date",date);
        UserAccount userAccount = userAccountMapper.selectOne(queryWrapper);
        if(userAccount == null) return false;
        QueryWrapper<AbnormalTransaction> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("user_id",user_id).eq("DATE(time)",date);
        List<AbnormalTransaction> abnormalTransactions = abnormalTransactionMapper.selectList(queryWrapper1);
        double former_balance = userAccountService.selectFormerBalanceByUserDate(user_id,date);
        if(former_balance < 0) return true; //前日无数据
        double expected_balance = reconciliationService.getExpectedBalance(former_balance,user_id,abnormalTransactions);
        return userAccount.getBalance() == (expected_balance+userAccount.getCharge());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackTransactionFlow(List<AbnormalTransaction> abnormalTransactions) {
        int user_id = abnormalTransactions.get(0).getUserId();
        String[] temp = abnormalTransactions.get(0).getTime().split(" ");
        String date = temp[0];
        for(AbnormalTransaction abnormalTransaction: abnormalTransactions) {
            abnormalTransactionMapper.deleteById(abnormalTransaction);
        }
        if(!reconciliationForAccount(user_id,date)) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        } else {  //Exception is solved
            if(!confirmNormal(user_id,date)) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return false;
            }
            for(AbnormalTransaction abnormalTransaction: abnormalTransactions) {
                QueryWrapper<UserOrder> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("order_id",abnormalTransaction.getOrderId());
                UserOrder userOrder = userOrderMapper.selectOne(queryWrapper);
                if(abnormalTransaction.getAmount() < 0) userOrder.setOrderState(2); //付款->未付款
                else userOrder.setOrderState(10); //失败->申请退款
                userOrderMapper.update(userOrder,queryWrapper);

                QueryWrapper<TransactionFlow> queryWrapper1 = new QueryWrapper<>();
                queryWrapper1.eq("order_id",abnormalTransaction.getOrderId()).ge("create_time",abnormalTransaction.getTime());
                transactionFlowMapper.delete(queryWrapper1);
            }
            return true;
        }
    }



}
