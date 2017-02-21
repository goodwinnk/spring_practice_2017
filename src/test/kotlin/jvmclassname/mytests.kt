package jvmclassname

import jvmclassname.builder.*
import org.junit.Test

class ClassNameTest2 : AbstractJvmClassNameTest() {
    override val testData = FILE("test.kt") {
        PACKAGE("org.example") {
            CLASS("Task1") {
                store("simpleClassName", "org/example/Task1")
                CLASS("A") {
                    store("innerClassName", "org/example/Task1\$A")

                    FUNCTION("solution") {
                        store("solution", "org/example/Task1\$A")

                        FUNCTION("local") {
                            store("local", "org/example/Task1\$A\$1")

                            LAMBDA("lambda1") {
                                store("lambda1", "org/example/Task1\$A\$2")

                                FUNCTION("more_local") {
                                    store("more_local", "org/example/Task1\$A\$3")
                                }
                            }

                            LAMBDA("lambda2") {
                                store("lambda2", "org/example/Task1\$A\$4")
                            }
                        }

                    }

                    FUNCTION("util") {
                        store("util", "org/example/Task1\$A")

                        LAMBDA("lambda3") {
                            store("lambda3", "org/example/Task1\$A\$5")
                        }
                    }
                }

                FUNCTION("member") {
                    store("member", "org/example/Task1")

                    FUNCTION("local_in_member") {
                        store("local_in_member", "org/example/Task1\$6")
                    }
                }
            }

            FUNCTION("top_level") {
                store("top_level", "org/example/test_kt")

                LAMBDA("lambda4") {
                    store("lambda4", "org/example/test_kt$1")
                }
            }
        }
    }

    @Test fun innerClassName() = test()
    @Test fun simpleClassName() = test()
    @Test fun solution() = test()
    @Test fun local() = test()
    @Test fun lambda1() = test()
    @Test fun more_local() = test()
    @Test fun lambda2() = test()
    @Test fun util() = test()
    @Test fun lambda3() = test()
    @Test fun member() = test()
    @Test fun local_in_member() = test()
    @Test fun top_level() = test()
    @Test fun lambda4() = test()
}