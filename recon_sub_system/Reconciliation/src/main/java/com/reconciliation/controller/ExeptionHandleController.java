package com.reconciliation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.reconciliation.entity.AbnormalTransaction;
import com.reconciliation.mapper.AbnormalTransactionMapper;
import com.reconciliation.service.DataPreprocessService;
import com.reconciliation.service.ExceptionHandleService;
import com.reconciliation.service.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = "/recon/api")
public class ExeptionHandleController {

    @Autowired
    private ExceptionHandleService exceptionHandleService;
    @Autowired
    private AbnormalTransactionMapper abnormalTransactionMapper;
    @Autowired
    private DataPreprocessService dataPreprocessService;
    @Autowired
    private ReconciliationService reconciliationService;
    @RequestMapping(value = "/rollback")
    public boolean callBack1(@RequestParam("id") List<Integer> id,@RequestParam("message") String message) {
        List<AbnormalTransaction> abnormalTransactions = new ArrayList<>();
        for(Integer ID: id) {
            QueryWrapper<AbnormalTransaction> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("transaction_id",ID);
            AbnormalTransaction abnormalTransaction = abnormalTransactionMapper.selectOne(queryWrapper);
            if(abnormalTransaction == null) continue;
            abnormalTransactions.add(abnormalTransaction);
        }
        if(abnormalTransactions.isEmpty()) return false;
        return exceptionHandleService.rollbackTransactionFlow(abnormalTransactions);
    }

    @RequestMapping(value = "/modifybalance")
    public boolean callBack2(@RequestParam("id") int id,@RequestParam("date") String date,
                             @RequestParam("val") double val,@RequestParam("message") String message) {
        return exceptionHandleService.editAccountBackupBalance(id,date,val);
    }

    @RequestMapping(value = "/freeze")
    public boolean callBack3(@RequestParam("id") int id,@RequestParam("date") String date,
                             @RequestParam("time") int time,@RequestParam("message") String message) {
        return exceptionHandleService.freezeAccount(id,date,time);
    }

    @RequestMapping(value = "/normal")
    public boolean callBack6(@RequestParam("id") int id,
                             @RequestParam("date") String date,@RequestParam("message") String message) {
        return exceptionHandleService.confirmNormal(id,date);
    }

    @RequestMapping(value = "/test")
    public void callBack4() {
        dataPreprocessService.saveUserBalances("2023-06-01");
    }

    @RequestMapping(value = "/testrecon")
    public void callBack5() {
        reconciliationService.reconciliation("2023-06-01");
    }
}