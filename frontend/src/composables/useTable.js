import { ref, reactive } from 'vue'

/**
 * 通用分页表格 composable
 * @param {Function} fetchApi - API 函数，接收 query 参数
 * @param {Object} defaultQuery - 默认查询参数
 */
export function useTable(fetchApi, defaultQuery = {}) {
  const loading = ref(false)
  const tableData = ref([])
  const total = ref(0)
  const query = reactive({
    page: 1,
    size: 20,
    ...defaultQuery
  })

  async function fetchData() {
    loading.value = true
    try {
      const res = await fetchApi(query)
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    query.page = 1
    fetchData()
  }

  function handleReset() {
    Object.assign(query, { page: 1, size: 20, ...defaultQuery })
    fetchData()
  }

  function handleSizeChange(val) {
    if (val != null) query.size = val
    query.page = 1
    fetchData()
  }

  function handleCurrentChange(val) {
    if (val != null) query.page = val
    fetchData()
  }

  return {
    loading,
    tableData,
    total,
    query,
    fetchData,
    handleSearch,
    handleReset,
    handleSizeChange,
    handleCurrentChange
  }
}
