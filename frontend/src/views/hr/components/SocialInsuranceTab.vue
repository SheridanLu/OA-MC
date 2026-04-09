<template>
  <div v-permission="'hr:social-insurance'" v-loading="loading">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 700px">
      <el-form-item label="员工ID" prop="userId">
        <el-input-number v-model="form.userId" :min="1" controls-position="right" style="width: 100%" />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="养老保险基数" prop="pensionBase">
            <money-input v-model="form.pensionBase" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="医疗保险基数" prop="medicalBase">
            <money-input v-model="form.medicalBase" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="失业保险基数" prop="unemploymentBase">
            <money-input v-model="form.unemploymentBase" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="工伤保险基数" prop="injuryBase">
            <money-input v-model="form.injuryBase" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="生育保险基数" prop="maternityBase">
            <money-input v-model="form.maternityBase" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="公积金基数" prop="housingBase">
            <money-input v-model="form.housingBase" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="onSave">保存配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSocialInsuranceConfig, updateSocialInsuranceConfig } from '@/api/hr'

const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  userId: null,
  pensionBase: null,
  medicalBase: null,
  unemploymentBase: null,
  injuryBase: null,
  maternityBase: null,
  housingBase: null
})

const rules = {
  userId: [{ required: true, message: '请输入员工ID', trigger: 'blur' }],
  pensionBase: [{ required: true, message: '请输入养老保险基数', trigger: 'blur' }],
  medicalBase: [{ required: true, message: '请输入医疗保险基数', trigger: 'blur' }],
  unemploymentBase: [{ required: true, message: '请输入失业保险基数', trigger: 'blur' }],
  injuryBase: [{ required: true, message: '请输入工伤保险基数', trigger: 'blur' }],
  maternityBase: [{ required: true, message: '请输入生育保险基数', trigger: 'blur' }],
  housingBase: [{ required: true, message: '请输入公积金基数', trigger: 'blur' }]
}

async function loadConfig() {
  loading.value = true
  try {
    const res = await getSocialInsuranceConfig()
    if (res.data) {
      const d = res.data
      form.userId = d.user_id ?? null
      form.pensionBase = d.pension_base ?? null
      form.medicalBase = d.medical_base ?? null
      form.unemploymentBase = d.unemployment_base ?? null
      form.injuryBase = d.injury_base ?? null
      form.maternityBase = d.maternity_base ?? null
      form.housingBase = d.housing_base ?? null
    }
  } catch { /* config may not exist yet */ }
  loading.value = false
}

async function onSave() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await updateSocialInsuranceConfig(form)
    ElMessage.success('保存成功')
    loadConfig()
  } finally {
    submitting.value = false
  }
}

onMounted(() => loadConfig())
</script>
