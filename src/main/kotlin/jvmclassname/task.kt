package jvmclassname

import jvmclassname.NodeType.*
import java.util.*

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

fun jvmClassName(node: Node): String {
    val parents = node.parents.toMutableList()
    parents.reverse()
    check(parents.first().type == FILE)

    val topLevelObject = parents.find { it.type == CLASS || it.type == FUNCTION } ?: node
    if (topLevelObject.type == CLASS) {
        parents.removeAt(0)
    } else if (parents[1].type == PACKAGE) {
        val tmp = parents[0]
        parents[0] = parents[1]
        parents[1] = tmp
    }

    fun translateNodesToClassName(nodes: MutableList<Node>, self: Node): String {
        if (nodes.removeAll { it.type == FUNCTION || it.type == LAMBDA } ||
                self.type != FUNCTION) {
            nodes.add(self)
        }

        fun getAnonymousClassesNumberTable(node: Node): Map<Node, Int> {
            val anonymousClassesMap = HashMap<Node, Int>()
            val packageOrFile = node.parents.find { it.type == PACKAGE } ?: node.parents.find { it.type == FILE }

            fun <T> enumerate(seq: Sequence<T>): Sequence<Pair<T, Int>> {
                return seq.zip((1..seq.count()).asSequence())
            }

            val topLevelFunctionsDescendants = ArrayList<Node>()
            for (child in checkNotNull(packageOrFile).children) {
                if (child.type == FUNCTION) {
                    topLevelFunctionsDescendants.addAll(child.descendants)
                } else {
                    val classFunctions = child.descendants.filter {
                        it.type == FUNCTION && it.parent!!.type != CLASS || it.type == LAMBDA
                    }
                    anonymousClassesMap.putAll(enumerate(classFunctions.asSequence()))
                }
            }

            topLevelFunctionsDescendants.removeAll { it.type != FUNCTION && it.type != LAMBDA }
            anonymousClassesMap.putAll(enumerate(topLevelFunctionsDescendants.asSequence()))

            return anonymousClassesMap
        }

        val anonymousClassesNumberTable = getAnonymousClassesNumberTable(self)

        val pathStringBuilder = nodes.map {
            when (it.type) {
                FILE -> "${it.modifiedName}"
                PACKAGE -> "${it.modifiedName}/"
                CLASS -> {
                    if (checkNotNull(it.parent).type == CLASS) {
                        "$${it.name}"
                    } else {
                        "${it.name}"
                    }
                }
                else -> "$${anonymousClassesNumberTable[it]}"
            }
        }

        return pathStringBuilder.joinToString("")
    }

    return translateNodesToClassName(parents, node)
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

val Node.modifiedName: String?
    get() = when(type) {
        PACKAGE -> name?.replace('.', '/')
        FILE -> name?.replace(Regex("[.$]"), "_")
        else -> name
    }

val Node.parentsWithSelf: Sequence<Node>
    get() = generateSequence(this) { if (it.type == FILE) null else it.parent }

val Node.parents: Sequence<Node>
    get() = parentsWithSelf.drop(1)

val Node.descendants: Sequence<Node>
    get() {
        val tour = ArrayList<Node>()
        fun dfs(current: Node) {
            for (child in current.children) {
                tour.add(child)
                dfs(child)
            }
        }

        dfs(this)
        return tour.asSequence()
    }
