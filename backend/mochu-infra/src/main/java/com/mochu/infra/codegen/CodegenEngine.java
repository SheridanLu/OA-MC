package com.mochu.infra.codegen;

import com.mochu.infra.entity.InfraCodegenColumn;
import com.mochu.infra.entity.InfraCodegenTable;
import jakarta.annotation.PostConstruct;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器 — Velocity 模板渲染引擎
 */
@Component
public class CodegenEngine {

    private VelocityEngine velocityEngine;

    @PostConstruct
    public void init() {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        velocityEngine.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());
        velocityEngine.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        velocityEngine.setProperty(RuntimeConstants.OUTPUT_ENCODING, "UTF-8");
        velocityEngine.init();
    }

    /** 11个模板对应的输出路径模板 */
    private static final Map<String, String> TEMPLATE_PATH_MAP = new LinkedHashMap<>();

    static {
        TEMPLATE_PATH_MAP.put("codegen/entity.java.vm",      "backend/{moduleName}/entity/{ClassName}.java");
        TEMPLATE_PATH_MAP.put("codegen/mapper.java.vm",      "backend/{moduleName}/mapper/{ClassName}Mapper.java");
        TEMPLATE_PATH_MAP.put("codegen/mapper.xml.vm",       "backend/{moduleName}/mapper/xml/{ClassName}Mapper.xml");
        TEMPLATE_PATH_MAP.put("codegen/service.java.vm",     "backend/{moduleName}/service/{ClassName}Service.java");
        TEMPLATE_PATH_MAP.put("codegen/controller.java.vm",  "backend/{moduleName}/controller/{ClassName}Controller.java");
        TEMPLATE_PATH_MAP.put("codegen/dto.java.vm",         "backend/{moduleName}/dto/{ClassName}DTO.java");
        TEMPLATE_PATH_MAP.put("codegen/queryDto.java.vm",    "backend/{moduleName}/dto/{ClassName}QueryDTO.java");
        TEMPLATE_PATH_MAP.put("codegen/vo.java.vm",          "backend/{moduleName}/vo/{ClassName}VO.java");
        TEMPLATE_PATH_MAP.put("codegen/api.js.vm",           "frontend/src/api/{bizName}.js");
        TEMPLATE_PATH_MAP.put("codegen/index.vue.vm",        "frontend/src/views/{moduleName}/{bizName}/index.vue");
        TEMPLATE_PATH_MAP.put("codegen/form.vue.vm",         "frontend/src/views/{moduleName}/{bizName}/form.vue");
    }

    /**
     * 预览所有模板 — 返回 Map<文件路径, 代码内容>
     */
    public Map<String, String> preview(InfraCodegenTable table, List<InfraCodegenColumn> columns) {
        VelocityContext ctx = buildContext(table, columns);
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : TEMPLATE_PATH_MAP.entrySet()) {
            String content = render(entry.getKey(), ctx);
            String path = resolvePath(entry.getValue(), table);
            result.put(path, content);
        }
        return result;
    }

    /**
     * 生成 zip 包字节数组
     */
    public byte[] generateZip(InfraCodegenTable table, List<InfraCodegenColumn> columns) {
        VelocityContext ctx = buildContext(table, columns);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : TEMPLATE_PATH_MAP.entrySet()) {
                String content = render(entry.getKey(), ctx);
                String path = resolvePath(entry.getValue(), table);
                zos.putNextEntry(new ZipEntry(path));
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            zos.finish();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成代码zip失败", e);
        }
    }

    // ==================== 私有方法 ====================

    private VelocityContext buildContext(InfraCodegenTable table, List<InfraCodegenColumn> columns) {
        VelocityContext ctx = new VelocityContext();
        ctx.put("tableName", table.getTableName());
        ctx.put("tableComment", table.getTableComment());
        ctx.put("moduleName", table.getModuleName());
        ctx.put("bizName", table.getBizName());
        ctx.put("ClassName", table.getClassName());
        ctx.put("className", CodegenBuilder.toCamelCase(table.getClassName()));
        // 首字母小写的类名
        String lcFirst = Character.toLowerCase(table.getClassName().charAt(0)) + table.getClassName().substring(1);
        ctx.put("classNameLc", lcFirst);
        ctx.put("author", table.getAuthor());
        ctx.put("columns", columns);
        // 需要导入的 Java 类型
        ctx.put("importTypes", collectImportTypes(columns));
        // 查询列（queryOperation=1）
        ctx.put("queryColumns", columns.stream().filter(c -> c.getQueryOperation() != null && c.getQueryOperation() == 1).toList());
        // 列表列（listOperation=1 且非主键）
        ctx.put("listColumns", columns.stream().filter(c -> c.getListOperation() != null && c.getListOperation() == 1 && (c.getPkFlag() == null || c.getPkFlag() == 0)).toList());
        // 表单列（createOperation=1）
        ctx.put("formColumns", columns.stream().filter(c -> c.getCreateOperation() != null && c.getCreateOperation() == 1).toList());
        return ctx;
    }

    private String render(String templateName, VelocityContext ctx) {
        Template tpl = velocityEngine.getTemplate(templateName, "UTF-8");
        StringWriter writer = new StringWriter();
        tpl.merge(ctx, writer);
        return writer.toString();
    }

    private String resolvePath(String pathTemplate, InfraCodegenTable table) {
        return pathTemplate
                .replace("{moduleName}", table.getModuleName())
                .replace("{bizName}", table.getBizName())
                .replace("{ClassName}", table.getClassName());
    }

    private Set<String> collectImportTypes(List<InfraCodegenColumn> columns) {
        Set<String> imports = new LinkedHashSet<>();
        for (InfraCodegenColumn col : columns) {
            String jt = col.getJavaType();
            if (jt != null && jt.contains(".")) {
                imports.add(jt);
            }
        }
        return imports;
    }
}
