<template>
    <div>
        <div class="search" style="margin-left: 10px">
            <el-dropdown @command="handleCommand">
              <el-button type="primary">查询订单类型
                  <i class="el-icon-arrow-down el-icon--right"></i></el-button>
                <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item command="0">全部</el-dropdown-item>
                    <el-dropdown-item command="-1">付款</el-dropdown-item>
                    <el-dropdown-item command="1">退款</el-dropdown-item>
                </el-dropdown-menu>
            </el-dropdown>
            <el-date-picker @change="select()"
                v-model="time"
                type="date"
                placeholder="选择日期"
                style="margin-left: 20px"
                value-format="yyyy-MM-dd">
            </el-date-picker>
            <label style="margin: 15px">用户id:</label>
            <el-input-number :controls="false"
            v-model="form.userId"
            :min="0"
            :precision="0" style="paddle:1px"
          ></el-input-number>
            <label style="margin: 15px">订单id:</label>
            <el-input-number :controls="false"
            v-model="form.orderId"
            :min="0"
            :precision="0" style="paddle:1px"
          ></el-input-number>
            <el-button type="small" style="margin-left: 15px" @click="select()">查询</el-button>
        </div>
        <el-table
            :data="tableData"
            stripe
            style="width: 100%; margin: 0 auto 0 50px;"
            height="600"
            @change="reCount"
            @sort-change="onSortChange">
            <el-table-column
                label="事务id"
                prop="transactionId"
                width="150%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
            </el-table-column>
            <el-table-column
                label="用户id"
                prop="userId"
                width="150%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
            </el-table-column>
            <el-table-column
                label="卖方id"
                prop="sellerId"
                width="150%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
            </el-table-column>
            <el-table-column
                label="订单id"
                prop="orderId"
                width="150%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
            </el-table-column>
            <el-table-column
                label="余额变动"
                prop="amount"
                width="150%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
            </el-table-column>
            <el-table-column
                label="时间"
                prop="time"
                width="200%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
            </el-table-column>
        </el-table>
        <br>
        <label style="margin-top:20px;margin-left: 350px;">条目数: {{form.recordNum}}</label>
        <label style="margin-left: 50px;">付款总额: {{form.fund}}</label>
        <label style="margin-left: 50px;">退款总额: {{form.refund}}</label>
    </div>
</template>

<style>
.demo-table-expand {
    font-size: 0;
}
.demo-table-expand label {
    width: 90px;
    color: #99a9bf;
}
.demo-table-expand .el-form-item {
    margin-right: 0;
    margin-bottom: 0;
    width: 50%;
}
.el-input-number__{
    margin: 0 !important;
}
</style>

<script>
import axios from "axios";
export default {
    data() {
        return {
            tableData: [],
            form: {
                userId: undefined,
                orderId: undefined,
                recordNum: 0,
                fund: 0.00,
                refund: 0.00,
            },
            formLabelWidth: '100px',
            time: "",
        }
    },
    created() {
        this.load()
    },
    methods: {
        load() {
            let date = new Date();
            let year = date.getFullYear();
            let month = date.getMonth()+1;
            let day = date.getDate();
            this.time = year + '-' + month + '-' + day;
            let url = 'http://localhost/recon/api/reconlist?date=' + this.time;
            axios.get(url).then(res => {
                this.tableData = res.data;
                this.updateData();
            })
        },
        /**
         * 表格排序事件处理函数
         * @param {object} {column,prop,order} 列数据|排序字段|排序方式
         */
        onSortChange({prop, order}) {
            this.tableData.sort(this.compare(prop, order));
        },

        /**
         * 排序比较
         * @param {string} propertyName 排序的属性名
         * @param {string} sort ascending(升序)/descending(降序)
         * @return {function}
         */
        compare(propertyName, sort) {
            return function (obj1, obj2) {
                var value1 = obj1[propertyName]
                var value2 = obj2[propertyName]
                if (typeof value1 === 'string' && typeof value2 === 'string') {
                    const res = value1.localeCompare(value2, 'zh')
                    return sort === 'ascending' ? res : -res
                } else {
                    if (value1 <= value2) {
                        return sort === 'ascending' ? -1 : 1
                    } else if (value1 > value2) {
                        return sort === 'ascending' ? 1 : -1
                    }
                }
            }
        },
        select() {
            // this.loading  = true;
            if (null == this.time) {
            }
            if (0 !== this.time.length) {
                axios.get('http://localhost/recon/api/reconlist?date='+ this.time)
                    .then((res) => {
                        if(this.form.userId == undefined && this.form.orderId == undefined){
                                this.tableData = res.data;
                                this.updateData();
                            }
                        else if(this.form.orderId == undefined){
                            for (var a in res.data) {
                            if(res.data[a]["userId"] != this.form.userId)
                                delete res.data[a];
                            this.tableData = res.data;
                            this.updateData();
                        }
                        }
                        else if(this.form.userId == undefined){
                            for (var a in res.data) {
                            if(res.data[a]["orderId"] != this.form.orderId)
                                delete res.data[a];
                            this.tableData = res.data;
                            this.updateData();
                        }
                        }
                        else {
                            for (var a in res.data) {
                            if(res.data[a]["orderId"] != this.form.orderId || res.data[a]["userId"] != this.form.userId)
                                delete res.data[a];
                            this.tableData = res.data;
                            this.updateData();
                        }
                        }
                    })
                    .catch((error) => {
                        console.log(error);
                        alert('输入格式错误');
                    });
            }
        },
        handleCommand(command) {
            axios.get('http://localhost/recon/api/reconlist?date='+ this.time)
                    .then((res) => {
                        for (var a in res.data) {
                            if(command * res.data[a]["amount"] < 0)
                            {
                                delete res.data[a];
                            }
                        };
                        this.tableData = res.data;
                        this.updateData();
                    })
                    .catch((error) => {
                        console.log(error);
                        alert('输入格式错误');
                    });
        },
        updateData() {
            this.form.fund = 0;
            this.form.refund = 0;
            this.form.recordNum = 0;
                for(var a in this.tableData) {
                    this.form.recordNum++;
                    if(this.tableData[a].amount > 0) this.form.refund += this.tableData[a].amount;
                    else this.form.fund -= this.tableData[a].amount; 
            }
        }
    }
}

</script>