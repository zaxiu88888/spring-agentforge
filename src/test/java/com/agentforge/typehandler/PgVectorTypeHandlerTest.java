package com.agentforge.typehandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** pgvector 文本协议双向转换（纯单测，不碰数据库） */
class PgVectorTypeHandlerTest {

    @Test
    @DisplayName("float[] → \"[0.1,0.2]\" 字面量")
    void toLiteral() {
        String literal = PgVectorTypeHandler.toVectorLiteral(new float[]{0.1f, 0.2f, -0.3f});
        assertThat(literal).isEqualTo("[0.1,0.2,-0.3]");
    }

    @Test
    @DisplayName("\"[0.1,0.2]\" → float[]（含空串/空格容错）")
    void parse() {
        float[] v = PgVectorTypeHandler.parse("[0.1, 0.2, -0.3]");
        assertThat(v).containsExactly(0.1f, 0.2f, -0.3f);
    }

    @Test
    @DisplayName("空向量 \"[]\" 解析为零长数组；null 返回 null")
    void edgeCases() {
        assertThat(PgVectorTypeHandler.parse("[]")).isEmpty();
        assertThat(PgVectorTypeHandler.parse(null)).isNull();
    }

    @Test
    @DisplayName("往返一致：toLiteral → parse 还原")
    void roundTrip() {
        float[] original = new float[1024];
        for (int i = 0; i < original.length; i++) original[i] = (i % 7) * 0.01f - 0.03f;
        float[] parsed = PgVectorTypeHandler.parse(PgVectorTypeHandler.toVectorLiteral(original));
        assertThat(parsed).containsExactly(original);
    }
}
