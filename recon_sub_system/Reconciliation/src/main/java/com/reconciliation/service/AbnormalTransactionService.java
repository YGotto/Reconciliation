package com.reconciliation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reconciliation.common.ApiResult;
import com.reconciliation.entity.AbnormalTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AbnormalTransactionService extends IService<AbnormalTransaction> {
    @Transactional(rollbackFor = Exception.class)
    void insertAbnormalTransactions(List<AbnormalTransaction> abnormalTransactions);

    public ApiResult<List<AbnormalTransaction>> selectAllAbnormalTransaction();

    public ApiResult<List<AbnormalTransaction>> selectAbnormalTransactionByUserDate(int userId, String date);

    void deleteAbnormalTransactionByUserDate(int userId, String date);
}
