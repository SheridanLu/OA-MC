<template>
  <div class="finance-page">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="结算单" name="statement"><statement-tab /></el-tab-pane>
        <el-tab-pane label="付款申请" name="payment"><payment-tab /></el-tab-pane>
        <el-tab-pane label="发票管理" name="invoice"><invoice-tab /></el-tab-pane>
        <el-tab-pane label="报销管理" name="reimburse"><reimburse-tab /></el-tab-pane>
        <el-tab-pane label="收款管理" name="receipt"><receipt-tab /></el-tab-pane>
        <el-tab-pane label="成本台账" name="costLedger"><cost-ledger-tab /></el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import StatementTab from './components/StatementTab.vue'
import PaymentTab from './components/PaymentTab.vue'
import InvoiceTab from './components/InvoiceTab.vue'
import ReimburseTab from './components/ReimburseTab.vue'
import CostLedgerTab from './components/CostLedgerTab.vue'
import ReceiptTab from './components/ReceiptTab.vue'

const activeTab = ref('statement')
</script>

<style scoped>
.finance-page { padding: 0; }
</style>
