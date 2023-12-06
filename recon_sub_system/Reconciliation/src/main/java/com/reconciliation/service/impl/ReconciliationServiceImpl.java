package com.reconciliation.service.impl;

import com.reconciliation.entity.*;

import com.reconciliation.mapper.*;
import com.reconciliation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ReconciliationServiceImpl implements ReconciliationService
{
    @Autowired
    private UserMapper user_mapper;
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private DataPreprocessService dataPreprocessService;
    @Autowired
    private AbnormalTransactionService abnormalTransactionService;

    /* 传入double型的balance，以及AbnormalTransation的一个List, 以及用户id，需要得到double类型的balance - assumption*/
    @Override
    public double getExpectedBalance(double balance, int userid, List<AbnormalTransaction> list) {
        double new_balance = balance;
        for (AbnormalTransaction abnormalTransaction : list) {
            if (abnormalTransaction.getUserId() == userid) new_balance += abnormalTransaction.getAmount();
        }
        return (double) Math.round(new_balance * 100) / 100;
    }

    @Scheduled(cron ="59 59 0 * * ?")
    private void autoReconciliation() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        System.out.println(dateFormat.format(calendar.getTime()));
        reconciliation(dateFormat.format(calendar));
    }

    @Override
    public void reconciliation(String date) {
        /* 获取前一天,后一天的日期（string）形式 */
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date temp_date = null;
        try {
            temp_date = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        Calendar calendar = Calendar.getInstance();
        if (temp_date != null) {
            calendar.setTime(temp_date);
        }
        calendar.add(Calendar.DATE, -1);
        Date new_date = calendar.getTime();
        calendar.add(Calendar.DATE, 2);
        Date next_date = calendar.getTime();
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        String previous_date = sdf1.format(new_date);
        String after_date = sdf1.format(next_date);

        HashMap<Integer, Double> cutoffRecharge = dataPreprocessService.loadRechargeRecords(after_date);

        /*数据结构*/
        HashMap<Integer, UserAccount> backupToday = new HashMap<>(); /* 今日的备份 */
        List<UserAccount> userAcc = new ArrayList<>(); /* 今日的备份(最终传给数据库的信息) */
        List<AbnormalTransaction> abnormalTransactionToday = new ArrayList<>();/*今日的异常交易记录 */
        HashMap<Integer, Double> accountChange = new HashMap<>();  /* key：用户id   value:用户当日交易额 */
        HashMap<Integer, Double> balanceYesterday = new HashMap<>(); /* key：用户id   value:用户前一日余额 */
        HashMap<Integer, List<AbnormalTransaction>> list = new HashMap<>(); /* 保存每个用户和它的所有交易记录 */
        List<UserAccount> backup_yesterday = userAccountService.selectAccountBackupByDate(previous_date).getData();
        /* 前一日备份 */
        List<UserAccount> todayData = dataPreprocessService.getUserBalances(date);
        /* 今日的数据, 00:00从外部获取，并且保存 */
        HashMap<Integer, UserAccount> today_Data = new HashMap<>();
        for (UserAccount todayDatum : todayData) today_Data.put(todayDatum.getUserId(), todayDatum);
        List<AbnormalTransaction> todayTransaction = dataPreprocessService.getTransaction(date);/* 今日订单 */
        /* 从前一日备份数据中获取所有用户前一日余额，自动忽略异常用户 */

        /* 获取此时的数据，用于日切对账审核 */
        /* 获取此时的用户余额数据（预处理之后） */
        List<User> user = user_mapper.selectList(null);
        HashMap<Integer, User> cutoffUser = new HashMap<>();
        for (User value : user) {
            cutoffUser.put(value.getId(), value);
        }
        /* 获取对账日（含日切）和对账日的下一日的订单数据（预处理之后） */
        List<AbnormalTransaction> cutoffTrans = new ArrayList<>(todayTransaction);
        List<AbnormalTransaction> new_trans = dataPreprocessService.getTransaction(after_date);
        cutoffTrans.addAll(new_trans);

        /* 获取对账日和对账日的下一日的充值数据 根据充值记录*/
        List<UserAccount> user12 = dataPreprocessService.getUserBalances(date);

        for (UserAccount account : user12) {
            if (!cutoffRecharge.containsKey(account.getUserId()))
                cutoffRecharge.put(account.getUserId(), account.getCharge());
            else
                cutoffRecharge.replace(account.getUserId(), account.getCharge() + cutoffRecharge.get(account.getUserId()));
        }

        for (UserAccount userAccount : backup_yesterday) {
            if (userAccount.getState() == 4)
                /* 遇到异常账户时，直接将异常账户的信息放到备份数据库中 */ {
                UserAccount wrong = new UserAccount();
                wrong.setState(userAccount.getState());
                wrong.setDate(date);
                wrong.setUserId(userAccount.getUserId());
                wrong.setCutoff(null);
                wrong.setBalance(today_Data.get(wrong.getUserId()).getBalance());
                wrong.setCharge(today_Data.get(wrong.getUserId()).getCharge());
                userAcc.add(wrong);
                backupToday.put(wrong.getUserId(), wrong);
            } else
                /* 遇到正常账户时，我们将其数据保存在balanceYesterday中 */ {
                balanceYesterday.put(userAccount.getUserId(), userAccount.getBalance());
            }
        }
        /*遍历今日的transaction_flow*/
        for (AbnormalTransaction abnormalTransaction : todayTransaction) {
            if (backupToday.containsKey(abnormalTransaction.getUserId()))
                /* 发现该条记录已经在今日的备份中，可以确定该用户为异常用户，将其今日的transaction放到异常transaction中 */ {
                abnormalTransactionToday.add(abnormalTransaction);
            } else {
                if (accountChange.containsKey(abnormalTransaction.getUserId()))
                    /* 已经探测过该用户的某条账单 */ {
                    accountChange.replace(abnormalTransaction.getUserId(), accountChange.get(abnormalTransaction.getUserId()) + abnormalTransaction.getAmount());
                    List<AbnormalTransaction> li = list.get(abnormalTransaction.getUserId());
                    li.add(abnormalTransaction);
                    list.replace(abnormalTransaction.getUserId(), li);
                } else
                    /*从未探测过该用户的账单*/ {
                    accountChange.put(abnormalTransaction.getUserId(), abnormalTransaction.getAmount());
                    List<AbnormalTransaction> li = new ArrayList<>();
                    li.add(abnormalTransaction);
                    list.put(abnormalTransaction.getUserId(), li);
                }
            }
        }
        for (int i = 0; i < todayData.size(); i++) {
            if (backupToday.containsKey(todayData.get(i).getUserId()))
                /* 该用户为异常用户，已被处理过 */
            {

            } else if (!balanceYesterday.containsKey(todayData.get(i).getUserId()))
                /* 该用户是今天新注册的 */ {
                UserAccount right = new UserAccount();
                right.setState(0);
                right.setDate(date);
                right.setUserId(todayData.get(i).getUserId());
                right.setCutoff(null);
                right.setBalance(today_Data.get(right.getUserId()).getBalance());
                right.setCharge(today_Data.get(right.getUserId()).getCharge());
                userAcc.add(right);
                backupToday.put(right.getUserId(), right);
            }
            /* 该用户是原有的，且正常/异常已处理 */
            else {
                double today_balance = today_Data.get(todayData.get(i).getUserId()).getBalance();
                double recharge = today_Data.get(todayData.get(i).getUserId()).getCharge();
                if ((accountChange.containsKey(todayData.get(i).getUserId())) &&
                        ((double)Math.round((today_balance - recharge)*100)/100 ==
                                (double)Math.round((accountChange.get(todayData.get(i).getUserId()) +
                                        balanceYesterday.get(todayData.get(i).getUserId()))*100)/100))
                {
                    /* 对账通过 */
                    if (list.get(todayData.get(i).getUserId()).size() >= 100 || accountChange.get(todayData.get(i).getUserId()) >= 100000
                            || accountChange.get(todayData.get(i).getUserId()) <= -100000)
                        /* 交易记录过多,或者交易额太大 */ {
                        UserAccount right = new UserAccount();
                        right.setState(2);
                        right.setDate(date);
                        right.setUserId(todayData.get(i).getUserId());
                        right.setCutoff(null);
                        right.setBalance(today_Data.get(right.getUserId()).getBalance());
                        right.setCharge(today_Data.get(right.getUserId()).getCharge());
                        userAcc.add(right);
                        backupToday.put(right.getUserId(), right);
                        abnormalTransactionToday.addAll(list.get(todayData.get(i).getUserId()));
                    } else
                        /* 没有问题 */ {
                        UserAccount right = new UserAccount();
                        right.setState(0);
                        right.setDate(date);
                        right.setUserId(todayData.get(i).getUserId());
                        right.setCutoff(null);
                        right.setBalance(today_Data.get(right.getUserId()).getBalance());
                        right.setCharge(today_Data.get(right.getUserId()).getCharge());
                        userAcc.add(right);
                        backupToday.put(right.getUserId(), right);
                    }
                }
                else if((!accountChange.containsKey(todayData.get(i).getUserId())) &&
                        ((double)Math.round((today_balance - recharge)*100)/100 ==
                                (double)Math.round(balanceYesterday.get(todayData.get(i).getUserId())*100)/100))
                    /* 今天没有交易记录 */
                {
                    UserAccount right = new UserAccount();
                    right.setState(0);
                    right.setDate(date);
                    right.setUserId(todayData.get(i).getUserId());
                    right.setCutoff(null);
                    right.setBalance(today_Data.get(right.getUserId()).getBalance());
                    right.setCharge(today_Data.get(right.getUserId()).getCharge());
                    userAcc.add(right);
                    backupToday.put(right.getUserId(), right);
                }
                else
                    /* 对账不正确 */ {
                    /* 日切处理 */
                    /* 根据前一日的备份数据，再进行一次对账检查 */
                    /* 若该次对账审核通过，则将到1：00的数据进行保存 */
                    double new_charge = cutoffRecharge.get(todayData.get(i).getUserId());
                    double new_consume = 0;
                    for (AbnormalTransaction cutoffTran : cutoffTrans) {
                        if (cutoffTran.getUserId() == todayData.get(i).getUserId()) {
                            new_consume += cutoffTran.getAmount();
                        }
                    }
                    double new_total = cutoffUser.get(todayData.get(i).getUserId()).getBalance();
                    if ((double)Math.round((balanceYesterday.get(todayData.get(i).getUserId()) + new_consume)*100)/100
                            == (double)Math.round(( new_total - new_charge)*100)/100)
                        /*日切对账成功*/
                    {
                        UserAccount right = new UserAccount();
                        right.setState(0);
                        right.setDate(date);
                        right.setUserId(todayData.get(i).getUserId());
                        right.setCutoff(cutoffTime);
                        right.setBalance(new_total);
                        right.setCharge(new_charge);
                        userAcc.add(right);
                        backupToday.put(right.getUserId(), right);
                    }
                    else
                        /* 总账对不上 */
                    {
                        UserAccount wrong = new UserAccount();
                        wrong.setState(1);
                        wrong.setDate(date);
                        wrong.setUserId(todayData.get(i).getUserId());
                        wrong.setCutoff(null);
                        wrong.setBalance(today_Data.get(wrong.getUserId()).getBalance());
                        wrong.setCharge(today_Data.get(wrong.getUserId()).getCharge());
                        userAcc.add(wrong);
                        if(list.containsKey(todayData.get(i).getUserId()))
                            abnormalTransactionToday.addAll(list.get(todayData.get(i).getUserId()));
                    }
                }
            }
        }
        abnormalTransactionService.insertAbnormalTransactions(abnormalTransactionToday);
        userAccountService.insertAccountBackup(userAcc);
    }
}