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

class Test2 : AbstractJvmClassNameTest() {
    override val testData = FILE("test.kt") {
        PACKAGE("org.example") {
            CLASS("Task1") {
                store("T1", "org/example/Task1")
                CLASS("A") {
                    store("T2", "org/example/Task1\$A")
                    FUNCTION("solution") {
                        store("T3", "org/example/Task1\$A")
                        FUNCTION("local") {
                            store("T4", "org/example/Task1\$A$1")
                            LAMBDA {
                                store("T5", "org/example/Task1\$A$2")
                                FUNCTION("more_local") {
                                    store("T6", "org/example/Task1\$A$3")
                                }
                            }
                            LAMBDA {
                                store("T7", "org/example/Task1\$A$4")
                            }
                        }
                    }

                    FUNCTION("util") {
                        LAMBDA {
                            store("T8", "org/example/Task1\$A$5")
                        }
                    }
                }

                FUNCTION("member") {
                    FUNCTION("local_in_member") {
                        store("T9", "org/example/Task1$6")
                    }
                }
            }

            FUNCTION("top_level") {
                store("T10", "org/example/test_kt")
                LAMBDA {
                    store("T11", "org/example/test_kt$1")
                }
            }
        }
    }

    @Test fun T1() = test()
    @Test fun T2() = test()
    @Test fun T3() = test()
    @Test fun T4() = test()
    @Test fun T5() = test()
    @Test fun T6() = test()
    @Test fun T7() = test()
    @Test fun T8() = test()
    @Test fun T9() = test()
    @Test fun T10() = test()
    @Test fun T11() = test()
}

class TestNoPackage: AbstractJvmClassNameTest() {
    override val testData = FILE("test.kt") {
        CLASS("C1") {
            store("T1", "C1")
            FUNCTION("F2") {
                store("T2", "C1")
                LAMBDA {
                    store("T3", "C1$1")
                }
                FUNCTION("F3") {
                    store("T4", "C1$2")
                }
            }
        }

        FUNCTION("F1") {
            store("T5", "test_kt")
            LAMBDA {
                store("T6", "test_kt$1")
                LAMBDA {
                    store("T7", "test_kt$2")
                    LAMBDA {
                        store("T8", "test_kt$3")
                    }
                }
                LAMBDA {
                    store("T9", "test_kt$4")
                }
            }
        }
    }
    @Test fun T1() = test()
    @Test fun T2() = test()
    @Test fun T3() = test()
    @Test fun T4() = test()
    @Test fun T5() = test()
    @Test fun T6() = test()
    @Test fun T7() = test()
    @Test fun T8() = test()
    @Test fun T9() = test()
}
