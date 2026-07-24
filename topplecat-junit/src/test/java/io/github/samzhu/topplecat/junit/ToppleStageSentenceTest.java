package io.github.samzhu.topplecat.junit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToppleStageSentenceTest {
    @Test
    void sharesMethodNameAndAsRulesBetweenRuntimeAndStaticRendering() {
        assertEquals("a cart with subtotal subtotalExpression",
                ToppleStageSentence.staticSentence("a_cart_with_subtotal", null, List.of("subtotalExpression")));
        assertEquals("creates Order", ToppleStageSentence.staticSentence("createsOrder", null, List.of()));
        assertEquals("建立訂單", ToppleStageSentence.staticSentence("createsOrder", "建立訂單", List.of()));
        assertEquals("準備 subtotalExpression 元", ToppleStageSentence.staticSentence("aCart", "準備 {0} 元",
                List.of("subtotalExpression")));
        assertEquals("準備 500 元", ToppleStageSentence.runtime("aCart", "準備 {0} 元", new Object[]{500}, "fixture"));
    }

    @Test
    void preservesSourceExpressionsWhenStaticRenderingCannotFillAllAsPlaceholders() {
        assertEquals("準備 {0} 與 {1} cartExpression",
                ToppleStageSentence.staticSentence("aCart", "準備 {0} 與 {1}", List.of("cartExpression")));
    }
}
