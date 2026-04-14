package com.mochu.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mochu.business.entity.BizApprovalInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BizApprovalInstanceMapper extends BaseMapper<BizApprovalInstance> {

    /**
     * P6 §4.13: SELECT FOR UPDATE — 防并发撤回
     */
    @Select("SELECT * FROM biz_approval_instance WHERE id = #{id} FOR UPDATE")
    BizApprovalInstance selectForUpdate(@Param("id") Integer id);
}
