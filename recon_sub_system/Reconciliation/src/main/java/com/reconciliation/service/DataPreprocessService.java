package com.reconciliation.service;

import com.reconciliation.entity.*;

import java.util.HashMap;
import java.util.List;

public interface DataPreprocessService {

       public void saveUserBalances(String date);

        HashMap<Integer, Double> loadRechargeRecords(String date);

    public List<UserAccount> getUserBalances(String date);

    /*付款成功（3）：仅需放入一次
     * 交易失败（0）：需要检查该记录是否已经申请退款（从全Transaction进行检查），如果有，说明他是一次退款，需要按照退款进行处理
     * */
    List<AbnormalTransaction> fetchTransaction(String date);

    List<AbnormalTransaction> getTransaction(String date);
}
