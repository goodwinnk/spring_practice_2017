package jvmclassname

import jvmclassname.NodeType.*

/**
 * Класс jvmclassname.Node задает узел синтаксического дерева для некоторого языка, который
 * компилируется в JVM-bytecode.
 *
 * Есть пять типов узлов (jvmclassname.NodeType):
 *   FILE, PACKAGE, CLASS, FUNCTION, LAMBDA
 *
 * Корректные для данного языка деревья описываются правилами:
 *   FILE -> PACKAGE | declarations
 *   PACKAGE -> declarations
 *   declarations -> (CLASS | FUNCTION)*
 *   CLASS -> (CLASS | FUNCTION)*
 *   FUNCTION -> (FUNCTION | LAMBDA)*
 *   LAMBDA -> (FUNCTION | LAMBDA)*
 *
 * Определения:
 *   Локальная функция - узел типа FUNCTION, у которого среди всех узлов на пути к корню есть другой
 *   узел типа FUNCTION.
 *
 *   Класс верхнего уровня - узел типа CLASS, предок которого либо типа FILE, либо типа PACKAGE.
 *
 *   Функция верхнего уровня - узел типа FUNCTION, предок которого узел либо типа FILE, либо типа PACKAGE.
 *
 * Каждый узел типа CLASS, FUNCTION, LAMBDA при компиляции попадает в какой-то JVM-класс.
 * Имя JVM-класса генерируется по следующим правилам:
 *   + Для класса верхнего уровня: <модифицированное имя пакета, если есть><'/', если есть имя пакета><имя класса>
 *       Модифицированное имя пакета - это имя пакета, в котором '.' заменены на '/'.
 *   + Для функции верхнего уровня:
 *       <модифицированное имя пакета, если есть><'/', если есть имя пакета><модифицированное имя файла>
 *       Модифицированное имя файла - это имя файла, в котором '.' и '$' заменены на '_'.
 *   + Для остальных классов:
 *       <имя JVM-класса родительского узла><'$'><имя текущего узла>
 *   + Для локальных функций и LAMBDA-узлов:
 *       <имя JVM-класса для ближайшего предка, соответствующего нелокальной функции или классу><'$'><номер анонимного класса>
 *       Номер анонимного класса равен номеру данного узла при обходе синтаксического дерева в глубину. Нумерация начинается с 1.
 *       (https://ru.wikipedia.org/wiki/%D0%9F%D0%BE%D0%B8%D1%81%D0%BA_%D0%B2_%D0%B3%D0%BB%D1%83%D0%B1%D0%B8%D0%BD%D1%83).
 *       При этом для узлов, являющихся потомками функции верхнего уровня, поиск запускается
 *       с ближайшего предка типа FILE, и должен пропускать классы верхнего уровня в данном файле.
 *       Для потомков классов верхнего уровня, поиск запускается на этом классе.
 *   + Для нелокальных функций, лежащих в классе, новый класс не создается.
 *
 * Пример:
 *
 * FILE(test.kt) {
 *   PACKAGE(org.example) {
 *     CLASS(Task1) { // имя JVM-класса этого узла: org/example/Task1
 *       CLASS(A) { // org/example/Task1$A
 *         FUNCTION(solution) { // org/example/Task1$A
 *           FUNCTION(local) { // org/example/Task1$A$1
 *             LAMBDA() { // org/example/Task1$A$2
 *               FUNCTION(more_local) // org/example/Task1$A$3
 *             }
 *             LAMBDA() // org/example/Task1$A$4
 *           }
 *         }
 *
 *         FUNCTION(util) {
 *           LAMBDA() // org/example/Task1$A$5
 *         }
 *       }
 *
 *       FUNCTION(member) {
 *         FUNCTION(local_in_member) { // org/example/Task1$6
 *         }
 *       }
 *     }
 *
 *     FUNCTION(top_level) { // org/example/test_kt
 *       LAMBDA() // org/example/test_kt$1
 *     }
 *   }
 * }
 *
 * Задача:
 *   По данному узлу типа CLASS, FUNCTION или LAMBDA построить имя JVM-класса в функции jvmclassname.jvmClassName().
 */

/*
* Каждый узел типа CLASS, FUNCTION, LAMBDA при компиляции попадает в какой-то JVM-класс.
 * Имя JVM-класса генерируется по следующим правилам:
  *   + Для локальных функций и LAMBDA-узлов:
 *       <имя JVM-класса для ближайшего предка, соответствующего нелокальной функции или классу><'$'><номер анонимного класса>
 *       Номер анонимного класса равен номеру данного узла при обходе синтаксического дерева в глубину. Нумерация начинается с 1.
 *       (https://ru.wikipedia.org/wiki/%D0%9F%D0%BE%D0%B8%D1%81%D0%BA_%D0%B2_%D0%B3%D0%BB%D1%83%D0%B1%D0%B8%D0%BD%D1%83).
 *       При этом для узлов, являющихся потомками функции верхнего уровня, поиск запускается
 *       с ближайшего предка типа FILE, и должен пропускать классы верхнего уровня в данном файле.
 *       Для потомков классов верхнего уровня, поиск запускается на этом классе.
  */

fun jvmClassName(node: Node): String {
    val builder = JvmClassNameBuilder()
    builder.buildImpl(node)
    return builder.getResult()
}

private class JvmClassNameBuilder {

    private val result = StringBuilder()

    fun getResult() = result.toString()

    fun buildImpl(node: Node) {

        if (node.parent == null) {
            throw IllegalStateException("`${node.type}` without parent: `${node.name}`")
        }

        when (node.type) {

            CLASS -> {
                when (node.parent.type) {
                    // Nested class
                    CLASS -> {
                        buildImpl(node.parent)
                        result.append('$')
                    }

                    // Top level class
                    PACKAGE -> result.append(node.packageModifiedName)
                    FILE -> {}

                    else -> {
                        throw IllegalStateException("`${node.parent.type}` can't be a parent of `$CLASS`")
                    }
                }
                result.append(node.name)
            }

            FUNCTION, LAMBDA -> {
                when (node.parent.type) {
                    // Top level function
                    PACKAGE -> {
                        result.append(node.packageModifiedName)
                        result.append(node.fileModifiedName)
                    }
                    FILE -> {
                        result.append(node.fileModifiedName)
                    }
                    // Nested function
                    FUNCTION, LAMBDA -> {
                        buildAnonymous(node)
                    }
                    // Common function
                    CLASS -> {
                        buildImpl(node.parent)
                    }
                }
            }

            else -> {
                throw IllegalArgumentException("Parameter type `${node.type}` is not allowed")
            }
        }
    }

    private fun buildAnonymous(node: Node) {
        var lastFunction = node
        val root = run {
            var current = node
            while (current.type != CLASS && current.type != FILE) {
                if (current.type == FUNCTION) {
                    lastFunction = current
                }
                current = current.parent
                        ?: throw IllegalStateException("`${node.type}` without parent: `${node.name}`")
            }
            current
        }

        buildImpl(if (root.type == CLASS) root else lastFunction)

        var count = 0
        fun dfs(current: Node, insideFunction: Boolean): Boolean {
            if (current === node) {
                return true
            }
            for (next in current.children) {
                if (next.type == FUNCTION || next.type == LAMBDA) {
                    if (insideFunction) count += 1
                    if (dfs(next, true)) return true
                }
                if (next.type == PACKAGE || (root.type == CLASS && next.type == CLASS)) {
                    if (dfs(next, false)) return true
                }
            }
            return false
        }

        dfs(root, false)
        result.append('$')
        result.append(count)
    }
}


enum class NodeType {
    FILE,
    PACKAGE,
    CLASS,
    FUNCTION,
    LAMBDA
}

class Node(
        val type: NodeType,
        val name: String?,
        val parent: Node?,
        var children: List<Node> = emptyList()
) {
    override fun toString() = "$type $name"
}

// == Sample utils ==
val Node.packageName: String?
    get() = parents.firstOrNull { it.type == PACKAGE }?.name

val Node.parentsWithSelf: Sequence<Node>
    get() = generateSequence(this) { if (it.type == FILE) null else it.parent }

val Node.parents: Sequence<Node>
    get() = parentsWithSelf.drop(1)

val Node.packageModifiedName: String?
    get() = packageName?.replace('.', '/') + '/'

val Node.file: Node
    get() = when {
        (type == FILE) -> this
        parent == null -> throw IllegalStateException("Node without file $name")
        else -> parent.file
    }

val Node.fileModifiedName: String
    get() = file.name?.replace(Regex("[.${'$'}]"), "_") ?: throw IllegalStateException("File without name")
