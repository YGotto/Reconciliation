package com.reconciliation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reconciliation.common.ApiResult;
import com.reconciliation.entity.UserAccount;
import com.reconciliation.mapper.UserAccountMapper;
import com.reconciliation.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

@Service
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper,UserAccount> implements UserAccountService {
    @Autowired
    UserAccountMapper userAccountMapper;

    @Override
    public ApiResult<List<UserAccount>> selectAllAccountBackup() {
        List<UserAccount> userAccounts;
        userAccounts = userAccountMapper.selectList(null);
        return ApiResult.success(userAccounts);
    }

    @Override
    public ApiResult<List<UserAccount>> selectAccountBackupByDate(String date) {
        List<UserAccount> userAccounts;
        QueryWrapper<UserAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("date",date);
        userAccounts = userAccountMapper.selectList(queryWrapper);
        return ApiResult.success(userAccounts);
    }

    @Override
    public double selectFormerBalanceByUserDate(int userId, String date) {
        QueryWrapper<UserAccount> queryWrapper = new QueryWrapper<>();
        try {
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar c = Calendar.getInstance();
            c.setTime(sf.parse(date));
            c.add(Calendar.DATE, -1);
            queryWrapper.eq("user_id",userId).eq("date",sf.format(c.getTime()));
            return userAccountMapper.selectOne(queryWrapper).getBalance();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public long getBackupDateNumber() {
        QueryWrapper<UserAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("distinct date");
        return userAccountMapper.selectCount(queryWrapper);
    }

    @Override
    public String getEarliestBackupDate() {
        QueryWrapper<UserAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("date");
        return userAccountMapper.selectList(queryWrapper).get(0).getDate();
    }

    @Override
    public String getSecondEarliestBackupDate() {
        try {
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar c = Calendar.getInstance();
            c.setTime(sf.parse(getEarliestBackupDate()));
            c.add(Calendar.DATE, 1);
            return sf.format(c.getTime());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean deleteEarliestBackup() {
        if(getBackupDateNumber() < 2) return false;
        QueryWrapper<UserAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("date",getSecondEarliestBackupDate()).between("state",1,2);
        long AbnormalUserNum = userAccountMapper.selectCount(queryWrapper);
        if(AbnormalUserNum != 0) return false;
        else {
            QueryWrapper<UserAccount> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("date",getEarliestBackupDate());
            userAccountMapper.delete(queryWrapper1);
            return true;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertAccountBackup(List<UserAccount> userAccounts) {
        if(getBackupDateNumber() == MaxBackupDate) {
            if(!deleteEarliestBackup()) return false;
        }
        try {
            for(UserAccount userAccount : userAccounts) {
                QueryWrapper<UserAccount> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("date",userAccount.getDate()).eq("user_id",userAccount.getUserId());
                userAccountMapper.delete(queryWrapper);
                userAccountMapper.insert(userAccount);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    @Override
    public ApiResult<List<UserAccount>> getAbnormalUser() {
        QueryWrapper<UserAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("state",1,2).orderByAsc("user_id","date");
        List<UserAccount> userAccounts = userAccountMapper.selectList(queryWrapper);
        int temp = -1;
        for(int i = 0; i < userAccounts.size(); i++) {
            if(userAccounts.get(i).getUserId() != temp)
                temp = userAccounts.get(i).getUserId();
            else {
                userAccounts.remove(i);
                i--;
            }
        }
        userAccounts.sort(new Comparator<>() {
            @Override
            public int compare(UserAccount o1, UserAccount o2) {
                if (o1.getDate().equals(o2.getDate())) {
                    return Integer.compare(o1.getUserId(), o2.getUserId());
                }
                return o1.getDate().compareTo(o2.getDate());
            }
        });
        return ApiResult.success(userAccounts);
    }

}


