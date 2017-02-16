package jvmclassname

import jvmclassname.builder.NodeBuilder
import org.junit.Assert
import org.junit.Rule
import org.junit.rules.TestName
import java.util.HashSet

abstract class AbstractJvmClassNameTest {
    @JvmField @Rule val name = TestName()

    abstract val testData: NodeBuilder

    protected val testNodes = HashSet<Node>()

    protected fun test() {
        val (node, expected) = testData.mapping[name.methodName]!!
        Assert.assertTrue(testNodes.add(node))
        Assert.assertEquals(expected, jvmClassName(node))
    }
}