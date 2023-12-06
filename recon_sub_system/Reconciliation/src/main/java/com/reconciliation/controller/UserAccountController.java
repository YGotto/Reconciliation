package com.reconciliation.controller;

import com.reconciliation.entity.UserAccount;
import com.reconciliation.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/recon/api")
public class UserAccountController {
    @Autowired
    private UserAccountService userAccountService;

    @RequestMapping("/allaccount")
    public List<UserAccount> getAllAccountBackup(){
        return userAccountService.selectAllAccountBackup().getData();
    }

    @RequestMapping("/account")
    public List<UserAccount> getAccountBackupByDate(@RequestParam("date") String date){
        return userAccountService.selectAccountBackupByDate(date).getData();
    }
    @RequestMapping("/account/count")
    public long getAccountBackupNumber(){
        return userAccountService.getBackupDateNumber();
    }
}
//@RequestParam("date") String date