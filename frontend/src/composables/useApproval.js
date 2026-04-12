import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveInstance, rejectInstance, withdrawInstance, transferInstance, addCosigner } from '@/api/approval'

/**
 * 审批操作 composable
 */
export function useApproval() {
  const approving = ref(false)

  async function approve(instanceId, comment = '') {
    approving.value = true
    try {
      await approveInstance(instanceId, comment)
      ElMessage.success('审批通过')
    } finally {
      approving.value = false
    }
  }

  async function reject(instanceId, comment = '') {
    approving.value = true
    try {
      await rejectInstance(instanceId, comment)
      ElMessage.success('已驳回')
    } finally {
      approving.value = false
    }
  }

  async function withdraw(instanceId) {
    await ElMessageBox.confirm('确认撤回？')
    await withdrawInstance(instanceId)
    ElMessage.success('已撤回')
  }

  async function transfer(instanceId, targetUserId, comment = '') {
    approving.value = true
    try {
      await transferInstance(instanceId, { target_user_id: targetUserId, comment })
      ElMessage.success('已转办')
    } finally {
      approving.value = false
    }
  }

  async function cosign(instanceId, userIds, comment = '') {
    approving.value = true
    try {
      await addCosigner(instanceId, { user_ids: userIds, comment })
      ElMessage.success('已加签')
    } finally {
      approving.value = false
    }
  }

  return { approving, approve, reject, withdraw, transfer, cosign }
}
