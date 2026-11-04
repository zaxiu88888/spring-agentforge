package com.agentforge.mapper;

import com.agentforge.model.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentMapper {
    int insert(Document document);

    Document selectById(String id);

    List<Document> selectByKbId(@Param("kbId") String kbId);

    int deleteById(String id);

    int updateById(Document document);
}
