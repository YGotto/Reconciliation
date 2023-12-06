package com.reconciliation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reconciliation.common.ApiResult;
import com.reconciliation.entity.UserAccount;

import java.util.List;

public interface UserAccountService extends IService<UserAccount> {
    public ApiResult<List<UserAccount>> selectAllAccountBackup();

    public ApiResult<List<UserAccount>> selectAccountBackupByDate(String date);

    double selectFormerBalanceByUserDate(int userId, String date);

    public long getBackupDateNumber();

    public String getEarliestBackupDate();

    String getSecondEarliestBackupDate();

    boolean deleteEarliestBackup();

    boolean insertAccountBackup(List<UserAccount> userAccounts);

    ApiResult<List<UserAccount>> getAbnormalUser();

    static long MaxBackupDate = 7;
}
