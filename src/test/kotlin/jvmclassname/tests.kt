package jvmclassname

import jvmclassname.builder.CLASS
import jvmclassname.builder.FILE
import jvmclassname.builder.PACKAGE
import org.junit.Test

class ClassNameTest : AbstractJvmClassNameTest() {
    override val testData = FILE("test.kt") {
        PACKAGE("test.unit") {
            CLASS("TestCase") {
                store("simpleClassName", "test/unit/TestCase")
                CLASS("Assert") {
                    store("innerClassName", "test/unit/TestCase\$Assert")
                }
            }

            CLASS("Other") {}
        }
    }

    @Test fun innerClassName() = test()
    @Test fun simpleClassName() = test()
}