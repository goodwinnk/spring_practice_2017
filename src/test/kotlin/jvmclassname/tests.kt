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

class ExampleTest : AbstractJvmClassNameTest() {
    override val testData = FILE("test.kt") {
        PACKAGE("org.example") {
            CLASS("Task1") {
                store("topLevelClassName", "org/example/Task1")
                CLASS("A") {
                    store("innerClassName", "org/example/Task1\$A")
                    FUNCTION("solution") {
                        store("nonLocalFunctionName", "org/example/Task1\$A")
                        FUNCTION("local") {
                            store("localFunctionName1", "org/example/Task1\$A$1")
                            LAMBDA {
                                store("lambdaName1", "org/example/Task1\$A$2")
                                FUNCTION("more_local") {
                                    store("localFunctionName2", "org/example/Task1\$A$3")
                                }
                            }

                            LAMBDA {
                                store("lambdaName2", "org/example/Task1\$A$4")
                            }
                        }
                    }

                    FUNCTION("util") {
                        LAMBDA {
                            store("lambdaName3", "org/example/Task1\$A$5")
                        }
                    }
                }

                FUNCTION("member") {
                    FUNCTION("local_in_member") {
                        store("localFunctionName3", "org/example/Task1$6")
                    }
                }
            }

            FUNCTION("top_level") {
                store("topLevelFunctionName", "org/example/test_kt")
                LAMBDA {
                    store("lambdaName4", "org/example/test_kt$1")
                }
            }
        }
    }

    @Test fun topLevelClassName() = test()
    @Test fun topLevelFunctionName() = test()
    @Test fun innerClassName() = test()
    @Test fun nonLocalFunctionName() = test()
    @Test fun localFunctionName1() = test()
    @Test fun localFunctionName2() = test()
    @Test fun localFunctionName3() = test()
    @Test fun lambdaName1() = test()
    @Test fun lambdaName2() = test()
    @Test fun lambdaName3() = test()
    @Test fun lambdaName4() = test()
}
