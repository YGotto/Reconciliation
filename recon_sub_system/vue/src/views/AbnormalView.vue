<template>
    <div>
        <el-table
            :data="tableData"
            stripe
            style="width: 100%; margin: 0 auto;"
            @sort-change="onSortChange"
            @expand-change="expandChange"
            :row-key='getRowKeys'
            :expand-row-keys="expands">
            <el-table-column type="expand">
                <template slot-scope="scope">
                    <el-table
                        :data="scope.row.Transactions"
                        height="250"
                        stripe 
                        style="width: 100%; margin: 0 auto; margin-left:50px;"
                        @sort-change="onSortChange"
                        @select="selectTransaction"
                        @select-all="selectAllTransactions" 
                        :row-key='getTranRowKeys'
                       >
                        <el-table-column type="selection" width="50" align="center" :reserve-selection="true" />
                        <el-table-column
                            label="事务id"
                            prop="transactionId"
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
                            label="卖方id"
                            prop="sellerId"
                            width="150%"
                            sortable="custom"
                            :sort-orders="['ascending','descending']">
                        </el-table-column>
                        <el-table-column
                            label="金额变化"
                            prop="amount"
                            width="150%"
                            sortable="custom"
                            :sort-orders="['ascending','descending']">
                        </el-table-column>
                        <el-table-column
                            label="交易时间"
                            prop="time"
                            width="200%"
                            sortable="custom"
                            :sort-orders="['ascending','descending']">
                        </el-table-column>
                    </el-table>
                    <el-button type="small" style="margin-left: 5%;margin-top:15px" :disabled="scope.row.state=='行为异常'" @click="rollback()">回滚记录</el-button>
                    <el-button type="small" style="margin-left: 30px" :disabled="scope.row.state=='行为异常'" @click="modify()">校正余额</el-button>
                    <el-button type="small" style="margin-left: 30px" :disabled="scope.row.state=='总账异常'" @click="freeze()">冻结用户</el-button>
                    <el-button type="small" style="margin-left: 30px" :disabled="scope.row.state=='总账异常'" @click="normal()">标记正常</el-button>
                    <label style="margin: 20px">处理信息:</label>
                    <el-input v-model="message" style="width: 40%" placeholder="请输入异常处理的信息"> </el-input>
                </template>
            </el-table-column>
            <el-table-column
                label="用户id"
                prop="user_id"
                width="150%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
            </el-table-column>
            <el-table-column
                label="用户状态"
                prop="state"
                width="150%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
            </el-table-column>
            <el-table-column
                label="用户余额"
                prop="balance"
                width="150%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
            </el-table-column>
            <el-table-column
                label="充值总额"
                prop="charge"
                width="150%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
            </el-table-column>
            <el-table-column
                label="前日余额"
                prop="former_balance"
                width="150%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
            </el-table-column>
            <el-table-column
                label="预期余额"
                prop="expected_balance"
                width="150%"
                sortable="custom">
            </el-table-column>
            <el-table-column
                label="异常日期"
                prop="date"
                width="150%"
                sortable="custom"
                :sort-orders="['ascending','descending']">
        </el-table-column>
        </el-table>
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
            index: undefined,
            selected:[],
            //控制展开只能展开一行
            expands: [],
            form: {
                userId: undefined
            },
            formLabelWidth: '100px',
            time: "",
            message: "",
        }
    },
    created() {
        this.load()
    },
    methods: {
        load() {
            let url = 'http://localhost/recon/api/abnormal';
            axios.get(url).then(res => {
                for (var a in res.data) {
                    if(res.data[a]["state"] == 1)
                        res.data[a]["state"] = "总账异常";
                    else if(res.data[a]["state"] == 2)
                        res.data[a]["state"] = "行为异常";
                };
                    if (res) {
                    //给每行数据强制追加一个数据项
                    res.data.map(item => {
                        item.Transactions = [];
                    });
                    this.tableData = res.data;
                    }

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
        expandChange(row, expandedRows) {
            var that = this
            if (expandedRows.length) {
            that.expands = [];
            this.message = "";
            that.expands.push(row.user_id);
            }
            // 遍历当前页面表
            let url = 'http://localhost/recon/api/abnormal';
            var tran;
            axios.get(url).then(res => {
                for (var a in res.data) {
                    if(res.data[a]["user_id"] == row.user_id)
                        {tran = res.data[a]["transaction"];                        
                        this.tableData.forEach((temp, index) => {
                        // 找到当前点击的行，把动态获取到的数据赋值进去
                        if (temp.user_id === row.user_id) {
                            this.tableData[index].Transactions = tran;
                            this.index = index;
                        }
                        })
                     }
                }; })
        },
        getRowKeys (row) {
            return row.user_id;
        },
        getTranRowKeys (row) {
            return row.transactionId;
        },
        selectTransaction(selection,row) {
            this.selected = [];
            for(var a in selection) {
                this.selected.push(selection[a]["transactionId"]);
            }
        },
        selectAllTransactions(selection) {
            this.selected = [];
            for(var a in selection) {
                this.selected.push(selection[a]["transactionId"]);
            }
        },
        rollback() {
            if(this.selected.length == 0) {
                this.$message({
                        type: 'info',
                        message: '请选择需要回滚的交易流程！'
                        });
                return;
            }
            this.$confirm('请确认是否回滚选定的交易流程?', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
            }).then(() => {
                let url = 'http://localhost/recon/api/rollback?id=';
                let mark = false;
                for(var a in this.selected) {
                    if(mark == false) {
                        url = url + this.selected[a];
                        mark = true;
                    } else {
                        url = url + ',' + this.selected[a];
                    }
                }
                url = url + "&message=" + this.message;
                axios.get(url).then(res => {
                    if(res.data == true) {
                        this.$confirm('异常记录处理完成!', '成功', {
                            confirmButtonText: '确定',
                            type: 'success'
                            }).then(() => {                            
                            window.location.reload([false]);
                            });
                    } else {
                        this.$message({
                        type: 'error',
                        message: '异常记录处理失败，请重试！'
                        });
                    }
                })
                this.message = "";
            }).catch(() => {
            this.$message({
                type: 'info',
                message: '操作已取消'
            });          
            });
        },
        modify() {
            this.$confirm('该操作会将用户的余额直接校正为预期余额且无法撤销，是否确认?', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
            }).then(() => {
                let url = 'http://localhost/recon/api/modifybalance?id=';
                url = url + this.tableData[this.index].user_id + "&date=" + this.tableData[this.index].date
                + "&val=" + (this.tableData[this.index].expected_balance-this.tableData[this.index].balance)
                + "&message=" + this.message;
                axios.get(url).then(res => {
                    if(res.data == true) {
                        this.$confirm('异常记录处理完成!', '成功', {
                            confirmButtonText: '确定',
                            type: 'success'
                            }).then(() => {                            
                            window.location.reload([false]);
                            });
                    } else {
                        this.$message({
                        type: 'error',
                        message: '异常记录处理失败，请重试！'
                        });
                    }
                })
                this.message = "";
            }).catch(() => {
            this.$message({
                type: 'info',
                message: '操作已取消'
            });          
            });
        },
        normal() {
            this.$confirm('是否将该账户标记为正常?', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
            }).then(() => {
                let url = 'http://localhost/recon/api/normal?id=';
                url = url + this.tableData[this.index].user_id + "&date=" + this.tableData[this.index].date
                + "&message=" + this.message;
                axios.get(url).then(res => {
                    if(res.data == true) {
                        this.$confirm('异常记录处理完成!', '成功', {
                            confirmButtonText: '确定',
                            type: 'success'
                            }).then(() => {                            
                            window.location.reload([false]);
                            });
                    } else {
                        this.$message({
                        type: 'error',
                        message: '异常记录处理失败，请重试！'
                        });
                    }
                })
                this.message = "";
            }).catch(() => {
            this.$message({
                type: 'info',
                message: '操作已取消'
            });          
            });
        },
        freeze() {
            this.$confirm('是否冻结该账户?', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
            }).then(() => {
                let url = 'http://localhost/recon/api/freeze?id=';
                url = url + this.tableData[this.index].user_id + "&date=" + this.tableData[this.index].date
                + "&time=" + "365"
                + "&message=" + this.message;
                axios.get(url).then(res => {
                    if(res.data == true) {
                        this.$confirm('异常记录处理完成!', '成功', {
                            confirmButtonText: '确定',
                            type: 'success'
                            }).then(() => {                            
                            window.location.reload([false]);
                            });
                    } else {
                        this.$message({
                        type: 'error',
                        message: '异常记录处理失败，请重试！'
                        });
                    }
                })
                this.message = "";
            }).catch(() => {
            this.$message({
                type: 'info',
                message: '操作已取消'
            });          
            });
        },
    }
}

</script>