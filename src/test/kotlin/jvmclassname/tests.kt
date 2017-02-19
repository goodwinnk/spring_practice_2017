package jvmclassname

import jvmclassname.builder.*
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

class ClassNameTestComplex : AbstractJvmClassNameTest() {
    override val testData = FILE("test.kt") {
        PACKAGE("org.example") {
            CLASS("Task1") {
                store("topLevelClass", "org/example/Task1")
                CLASS("A") {
                    store("nested", "org/example/Task1${'$'}A")
                    FUNCTION("solution") {
                        store("member", "org/example/Task1${'$'}A")
                        FUNCTION("local") {
                            store("localInNested", "org/example/Task1${'$'}A$1")
                            LAMBDA {
                                store("lambdaInLocalInNested1", "org/example/Task1${'$'}A$2")
                                FUNCTION("more_local") {
                                    store("localInLambdaInLocalInNested", "org/example/Task1${'$'}A$3")
                                }
                            }
                            LAMBDA {
                                store("lambdaInLocalInNested2", "org/example/Task1${'$'}A$4")
                            }
                        }
                    }
                    FUNCTION("util") {
                        LAMBDA {
                            store("lambdaInMemberInNested", "org/example/Task1${'$'}A$5")
                        }
                    }
                }
                FUNCTION("member") {
                    FUNCTION("local_in_member") {
                        store("localInMember", "org/example/Task1$6")
                    }
                }
            }
            FUNCTION("top_level") {
                store("topLevelFunction", "org/example/test_kt")
                LAMBDA {
                    store("lambdaOut1", "org/example/test_kt$1")
                }
            }
        }
    }

    @Test fun topLevelClass() = test()
    @Test fun nested() = test()
    @Test fun lambdaOut1() = test()
    @Test fun topLevelFunction() = test()
    @Test fun localInMember() = test()
    @Test fun lambdaInMemberInNested() = test()
    @Test fun lambdaInLocalInNested1() = test()
    @Test fun lambdaInLocalInNested2() = test()
    @Test fun localInLambdaInLocalInNested() = test()
    @Test fun localInNested() = test()
}
