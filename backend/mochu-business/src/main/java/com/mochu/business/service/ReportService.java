package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.business.entity.*;
import com.mochu.business.mapper.*;
import com.mochu.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表汇总服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final BizProjectMapper projectMapper;
    private final BizContractMapper contractMapper;
    private final BizInventoryMapper inventoryMapper;
    private final BizCostLedgerMapper costLedgerMapper;
    private final BizStatementMapper statementMapper;
    private final BizPaymentApplyMapper paymentMapper;
    private final BizHrEntryMapper hrEntryMapper;
    private final BizHrResignMapper hrResignMapper;
    private final BizSalaryMapper salaryMapper;

    /**
     * 项目汇总：按状态分组计数 + 预算/合同额合计
     */
    public Map<String, Object> getProjectSummary() {
        List<BizProject> projects = projectMapper.selectList(new LambdaQueryWrapper<>());

        // 按状态分组计数
        Map<String, Long> statusCounts = projects.stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.groupingBy(BizProject::getStatus, Collectors.counting()));

        // 预算总额（investLimit）
        BigDecimal totalBudget = projects.stream()
                .map(BizProject::getInvestLimit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 合同总额（amountWithTax）
        BigDecimal totalContract = projects.stream()
                .map(BizProject::getAmountWithTax)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statusCounts", statusCounts);
        result.put("totalBudget", totalBudget);
        result.put("totalContract", totalContract);
        return result;
    }

    /**
     * 财务汇总：按对账单期间分组，汇总产值与回款
     */
    public Map<String, Object> getFinanceSummary() {
        List<BizStatement> statements = statementMapper.selectList(new LambdaQueryWrapper<>());

        // 按 period 分组
        Map<String, List<BizStatement>> grouped = statements.stream()
                .filter(s -> s.getPeriod() != null)
                .collect(Collectors.groupingBy(BizStatement::getPeriod, TreeMap::new, Collectors.toList()));

        List<String> months = new ArrayList<>(grouped.keySet());

        List<BigDecimal> income = new ArrayList<>();
        List<BigDecimal> expense = new ArrayList<>();
        for (String month : months) {
            List<BizStatement> list = grouped.get(month);
            BigDecimal monthIncome = list.stream()
                    .map(BizStatement::getCurrentOutput)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal monthExpense = list.stream()
                    .map(BizStatement::getCurrentCollection)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            income.add(monthIncome);
            expense.add(monthExpense);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", months);
        result.put("income", income);
        result.put("expense", expense);
        return result;
    }

    /**
     * 库存汇总：按物料分组，汇总数量与金额
     */
    public Map<String, Object> getInventorySummary() {
        List<BizInventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<>());

        // 按 materialId 分组汇总
        Map<Integer, List<BizInventory>> grouped = inventories.stream()
                .filter(i -> i.getMaterialId() != null)
                .collect(Collectors.groupingBy(BizInventory::getMaterialId));

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<Integer, List<BizInventory>> entry : grouped.entrySet()) {
            List<BizInventory> list = entry.getValue();
            BigDecimal totalQty = list.stream()
                    .map(BizInventory::getCurrentQuantity)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalAmount = list.stream()
                    .map(BizInventory::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("materialId", entry.getKey());
            // 取第一条的物料名称作为展示
            item.put("materialName", list.get(0).getMaterialName());
            item.put("totalQty", totalQty);
            item.put("totalAmount", totalAmount);
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        return result;
    }

    /**
     * 合同汇总：按合同类型分组计数 + 总金额
     */
    public Map<String, Object> getContractSummary() {
        List<BizContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<>());

        Map<String, Long> typeCounts = contracts.stream()
                .filter(c -> c.getContractType() != null)
                .collect(Collectors.groupingBy(BizContract::getContractType, Collectors.counting()));

        BigDecimal totalAmount = contracts.stream()
                .map(BizContract::getAmountWithTax)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("typeCounts", typeCounts);
        result.put("totalAmount", totalAmount);
        return result;
    }

    /**
     * 成本汇总：按成本类型分组，汇总金额
     */
    public Map<String, Object> getCostSummary() {
        List<BizCostLedger> ledgers = costLedgerMapper.selectList(new LambdaQueryWrapper<>());

        Map<String, List<BizCostLedger>> grouped = ledgers.stream()
                .filter(l -> l.getCostType() != null)
                .collect(Collectors.groupingBy(BizCostLedger::getCostType));

        List<Map<String, Object>> costs = new ArrayList<>();
        for (Map.Entry<String, List<BizCostLedger>> entry : grouped.entrySet()) {
            BigDecimal totalAmount = entry.getValue().stream()
                    .map(BizCostLedger::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("costType", entry.getKey());
            item.put("totalAmount", totalAmount);
            costs.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("costs", costs);
        return result;
    }

    /**
     * 人力资源汇总：入职/离职人数 + 薪资总额
     */
    public Map<String, Object> getHrSummary() {
        long entryCount = hrEntryMapper.selectCount(new LambdaQueryWrapper<>());
        long resignCount = hrResignMapper.selectCount(new LambdaQueryWrapper<>());

        List<BizSalary> salaries = salaryMapper.selectList(new LambdaQueryWrapper<>());
        BigDecimal totalSalary = salaries.stream()
                .map(BizSalary::getNetSalary)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entryCount", entryCount);
        result.put("resignCount", resignCount);
        result.put("totalSalary", totalSalary);
        return result;
    }

    /**
     * 导出报表为CSV — 根据 type 选择对应的汇总数据
     */
    public void exportReport(String type, Integer projectId, HttpServletResponse response) throws IOException {
        if (type == null || type.isBlank()) {
            throw new BusinessException("报表类型不能为空");
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=report_" + type + ".csv");
        response.setCharacterEncoding("UTF-8");

        PrintWriter writer = response.getWriter();
        // UTF-8 BOM for Excel compatibility
        writer.write('\ufeff');

        switch (type) {
            case "project" -> {
                Map<String, Object> data = getProjectSummary();
                writer.println("状态,数量");
                @SuppressWarnings("unchecked")
                Map<String, Long> statusCounts = (Map<String, Long>) data.get("statusCounts");
                if (statusCounts != null) {
                    statusCounts.forEach((status, count) -> writer.println(status + "," + count));
                }
                writer.println();
                writer.println("预算总额," + data.get("totalBudget"));
                writer.println("合同总额," + data.get("totalContract"));
            }
            case "finance" -> {
                Map<String, Object> data = getFinanceSummary();
                @SuppressWarnings("unchecked")
                List<String> months = (List<String>) data.get("months");
                @SuppressWarnings("unchecked")
                List<BigDecimal> income = (List<BigDecimal>) data.get("income");
                @SuppressWarnings("unchecked")
                List<BigDecimal> expense = (List<BigDecimal>) data.get("expense");
                writer.println("月份,产值,回款");
                if (months != null) {
                    for (int i = 0; i < months.size(); i++) {
                        writer.println(months.get(i) + ","
                                + (income != null && i < income.size() ? income.get(i) : 0) + ","
                                + (expense != null && i < expense.size() ? expense.get(i) : 0));
                    }
                }
            }
            case "inventory" -> {
                Map<String, Object> data = getInventorySummary();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
                writer.println("物料ID,物料名称,总数量,总金额");
                if (items != null) {
                    for (Map<String, Object> item : items) {
                        writer.println(item.get("materialId") + ","
                                + item.get("materialName") + ","
                                + item.get("totalQty") + ","
                                + item.get("totalAmount"));
                    }
                }
            }
            case "contract" -> {
                Map<String, Object> data = getContractSummary();
                writer.println("合同类型,数量");
                @SuppressWarnings("unchecked")
                Map<String, Long> typeCounts = (Map<String, Long>) data.get("typeCounts");
                if (typeCounts != null) {
                    typeCounts.forEach((cType, count) -> writer.println(cType + "," + count));
                }
                writer.println();
                writer.println("合同总额," + data.get("totalAmount"));
            }
            case "cost" -> {
                Map<String, Object> data = getCostSummary();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> costs = (List<Map<String, Object>>) data.get("costs");
                writer.println("成本类型,总金额");
                if (costs != null) {
                    for (Map<String, Object> cost : costs) {
                        writer.println(cost.get("costType") + "," + cost.get("totalAmount"));
                    }
                }
            }
            case "hr" -> {
                Map<String, Object> data = getHrSummary();
                writer.println("指标,数值");
                writer.println("入职人数," + data.get("entryCount"));
                writer.println("离职人数," + data.get("resignCount"));
                writer.println("薪资总额," + data.get("totalSalary"));
            }
            default -> throw new BusinessException("不支持的报表类型: " + type);
        }

        writer.flush();
    }
}
