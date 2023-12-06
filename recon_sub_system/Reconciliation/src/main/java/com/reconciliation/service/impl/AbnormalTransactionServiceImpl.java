package com.reconciliation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reconciliation.common.ApiResult;
import com.reconciliation.entity.AbnormalTransaction;
import com.reconciliation.mapper.AbnormalTransactionMapper;
import com.reconciliation.service.AbnormalTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;

@Service
public class AbnormalTransactionServiceImpl extends ServiceImpl<AbnormalTransactionMapper, AbnormalTransaction> implements AbnormalTransactionService {
    @Autowired
    AbnormalTransactionMapper abnormalTransactionMapper;
    @Override
    public ApiResult<List<AbnormalTransaction>> selectAllAbnormalTransaction() {
        List<AbnormalTransaction> abnormalTransactions;
        abnormalTransactions = abnormalTransactionMapper.selectList(null);
        return ApiResult.success(abnormalTransactions);
    }

    @Override
    public ApiResult<List<AbnormalTransaction>> selectAbnormalTransactionByUserDate(int userId, String date) {
        List<AbnormalTransaction> userAccounts;
        QueryWrapper<AbnormalTransaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DATE(time)",date).eq("user_id",userId);
        userAccounts = abnormalTransactionMapper.selectList(queryWrapper);
        return ApiResult.success(userAccounts);
    }

    @Override
    public void deleteAbnormalTransactionByUserDate(int userId, String date) {
        QueryWrapper<AbnormalTransaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DATE(time)",date).eq("user_id",userId);
        abnormalTransactionMapper.delete(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertAbnormalTransactions(List<AbnormalTransaction> abnormalTransactions) {
        try {
            for(AbnormalTransaction abnormalTransaction : abnormalTransactions) {
                abnormalTransactionMapper.insert(abnormalTransaction);
            }
        } catch (Exception e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }
}
