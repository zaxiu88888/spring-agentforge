package com.agentforge;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** 上下文加载冒烟测试：验证依赖装配（双模型 starter / 数据源 / MyBatis）无冲突 */
@Tag("smoke")
@SpringBootTest
class AgentForgeApplicationTests {

    @Test
    void contextLoads() {
    }
}
