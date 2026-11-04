package com.agentforge.mapper;

import com.agentforge.model.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper {
    int insert(KnowledgeBase kb);

    KnowledgeBase selectById(String id);

    List<KnowledgeBase> selectAll();

    List<KnowledgeBase> selectByIdBatch(@Param("ids") List<String> ids);

    int deleteById(String id);

    int updateById(KnowledgeBase kb);
}
