package jvmclassname

import jvmclassname.builder.CLASS
import jvmclassname.builder.FILE
import jvmclassname.builder.PACKAGE
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.util.*

val testData = FILE("test.kt") {
    PACKAGE("test.unit") {
        CLASS("TestCase") {
            store("simpleClassName")
            CLASS("Assert") {
                store("innerClassName")
            }
        }

        CLASS("Other") {}
    }
}.mapping

class ClassNameTest {
    @JvmField @Rule val name = TestName()

    private val testNodes = HashSet<Node>()

    @Test fun innerClassName() {
        test("test/unit/TestCase\$Assert")
    }

    @Test fun simpleClassName() {
        test("test/unit/TestCase")
    }

    private fun test(expected: String?) {
        val node = testData[name.methodName]!!
        Assert.assertTrue(testNodes.add(node))
        Assert.assertEquals(expected, jvmClassName(node))
    }
}