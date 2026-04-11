package com.mochu.infra.codegen;

import com.mochu.infra.entity.InfraCodegenColumn;
import com.mochu.infra.entity.InfraCodegenTable;
import com.mochu.infra.vo.DbTableVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 代码生成器 — 数据库元数据读取与推断
 */
@Component
@RequiredArgsConstructor
public class CodegenBuilder {

    private final JdbcTemplate jdbcTemplate;

    /** 自动跳过的 BaseEntity 字段 */
    private static final Set<String> BASE_FIELDS = Set.of(
            "id", "creator_id", "created_at", "updated_at", "deleted"
    );

    /** 查询当前库中尚未导入的表 */
    public List<DbTableVO> listUnimportedTables(String dbName, List<String> importedTableNames) {
        String sql = """
                SELECT table_name, IFNULL(table_comment,'') AS table_comment,
                       DATE_FORMAT(create_time,'%Y-%m-%d %H:%i:%s') AS create_time
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_type = 'BASE TABLE'
                ORDER BY create_time DESC
                """;
        List<DbTableVO> all = jdbcTemplate.query(sql, (rs, i) -> {
            DbTableVO v = new DbTableVO();
            v.setTableName(rs.getString("table_name"));
            v.setTableComment(rs.getString("table_comment"));
            v.setCreateTime(rs.getString("create_time"));
            return v;
        }, dbName);

        if (importedTableNames == null || importedTableNames.isEmpty()) {
            return all;
        }
        return all.stream()
                .filter(t -> !importedTableNames.contains(t.getTableName()))
                .toList();
    }

    /** 从数据库读取表注释 */
    public String getTableComment(String dbName, String tableName) {
        String sql = "SELECT IFNULL(table_comment,'') FROM information_schema.tables " +
                     "WHERE table_schema=? AND table_name=?";
        List<String> result = jdbcTemplate.queryForList(sql, String.class, dbName, tableName);
        return result.isEmpty() ? "" : result.get(0);
    }

    /** 自省表列信息，构建列配置列表 */
    public List<InfraCodegenColumn> buildColumns(String dbName, String tableName, InfraCodegenTable table) {
        String sql = """
                SELECT column_name, column_comment, data_type,
                       column_key, is_nullable, ordinal_position
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ?
                ORDER BY ordinal_position
                """;
        return jdbcTemplate.query(sql, (rs, i) -> {
            String colName = rs.getString("column_name");
            if (BASE_FIELDS.contains(colName)) {
                return null;
            }
            InfraCodegenColumn col = new InfraCodegenColumn();
            col.setTableId(table.getId());
            col.setColumnName(colName);
            col.setColumnComment(rs.getString("column_comment"));
            col.setDataType(rs.getString("data_type"));
            col.setNullableFlag("YES".equals(rs.getString("is_nullable")) ? 1 : 0);
            col.setColumnSort(rs.getInt("ordinal_position"));

            boolean isPk = "PRI".equals(rs.getString("column_key"));
            col.setPkFlag(isPk ? 1 : 0);

            // 推断 Java 类型
            col.setJavaType(inferJavaType(col.getDataType()));
            // 推断 Java 字段名（下划线转驼峰）
            col.setJavaField(toCamelCase(colName));
            // 推断前端组件类型
            col.setHtmlType(inferHtmlType(colName, col.getDataType()));
            // 推断查询条件（主键和特殊字段不作查询）
            col.setQueryOperation(inferQueryOperation(colName) ? 1 : 0);
            col.setQueryCondition(inferQueryCondition(colName, col.getDataType()));
            // 默认 CRUD 可见（主键仅列表可见）
            col.setCreateOperation(isPk ? 0 : 1);
            col.setUpdateOperation(isPk ? 0 : 1);
            col.setListOperation(1);
            return col;
        }, dbName, tableName).stream().filter(c -> c != null).toList();
    }

    // ==================== 推断逻辑 ====================

    private String inferJavaType(String dataType) {
        return switch (dataType.toLowerCase()) {
            case "varchar", "char", "text", "mediumtext", "longtext", "tinytext" -> "String";
            case "tinyint" -> "Integer";
            case "smallint", "int", "mediumint" -> "Integer";
            case "bigint" -> "Long";
            case "decimal", "numeric" -> "java.math.BigDecimal";
            case "double", "float" -> "Double";
            case "date" -> "java.time.LocalDate";
            case "datetime", "timestamp" -> "java.time.LocalDateTime";
            case "time" -> "java.time.LocalTime";
            default -> "String";
        };
    }

    private String inferHtmlType(String colName, String dataType) {
        String lower = colName.toLowerCase();
        if (lower.endsWith("_status") || lower.equals("status")) return "select";
        if (lower.endsWith("_type") || lower.equals("type")) return "select";
        if (lower.endsWith("_id") && !lower.equals("id")) return "select";
        if (lower.contains("content") || lower.contains("remark") || lower.contains("desc")) return "textarea";
        if (lower.contains("image") || lower.contains("avatar") || lower.contains("url")) return "upload";
        if (dataType.contains("datetime") || dataType.contains("date")) return "datetime";
        return "input";
    }

    private boolean inferQueryOperation(String colName) {
        String lower = colName.toLowerCase();
        // 常见查询字段白名单
        return Arrays.asList("name", "status", "type", "code", "title").stream()
                .anyMatch(lower::contains);
    }

    private String inferQueryCondition(String colName, String dataType) {
        String lower = colName.toLowerCase();
        if (lower.contains("name") || lower.contains("title") || lower.contains("remark")) return "LIKE";
        if (dataType.contains("datetime") || dataType.contains("date")) return "BETWEEN";
        return "EQ";
    }

    /** 下划线转驼峰 */
    public static String toCamelCase(String underline) {
        if (underline == null || !underline.contains("_")) return underline;
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : underline.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return sb.toString();
    }

    /** 驼峰转首字母大写 */
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
