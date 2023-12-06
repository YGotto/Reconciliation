<template>
    <div id="app">
        <!-- head -->
        <div style="height: 60px; line-height: 60px; background-color: white; margin-bottom: 5px">
            <img src="@/assets/logo.png" alt="" style="width: 40px; position: relative; top: 18px; left: 20px">
            <el-link to="/" :underline="false" style="margin-left: 25px; font-size: 24px">对账与审核子系统</el-link>
            <label style="margin-left:950px;" for="testlogout" id="logoutlabel">注销</label>
            <button @click="logout" id="testlogout" style="visibility:hidden">点击</button>
        </div>

        <!-- left and body -->
        <div style="display: flex">
            <div style="width: 200px; min-height:calc(100vh - 100px); overflow: hidden; margin-right: 2px; background-color: white">
                <el-menu
                        default-active="/"
                        router class="el-menu-demo">
                    <el-menu-item :index="'/recon'">
                        <i class="el-icon-house"></i>
                        <span slot="title">对账数据列表</span>
                    </el-menu-item>
                    <el-menu-item :index="'/abnormal'">
                        <i class="el-icon-house"></i>
                        <span slot="title" v-if="abnormal_number != 0" style="color:indianred;font-weight:bold">异常账户信息({{abnormal_number}})</span>
                        <span slot="title" v-if="abnormal_number == 0">异常账户信息</span>
                    </el-menu-item>
                </el-menu>
            </div>
            <div style="flex: 1; background-color: white">
                <router-view/>
            </div>
        </div>
    </div>
</template>

<style>
#logoutlabel:hover {
    color: indianred;
    font-weight: bold;
}
#logoutlabel:active {
    color: lightcoral;
    font-weight: bold;
}
</style>

<script>
import axios from "axios";
export default {
    data() {
        return {
            abnormal_number: 0,
        }
    },
    created() {
        this.load()
    },
    methods: {
        load() {
            let url = 'http://localhost/recon/api/abnormalcount';
            axios.get(url).then(res => {
                console.log(res.data);
                    this.abnormal_number = res.data;
            })
        },
        logout() {
            alert("signout");
        },
    }
}
</script>