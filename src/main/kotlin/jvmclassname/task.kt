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

fun jvmClassName(node: Node): String =
        (sequenceOf(node.segmentName)
                + node.parents.filter(Node::isNamedClass).map(Node::segmentName)
                + sequenceOf(node.packageName)
                )
                .filterNotNull()
                .toList()
                .reversed()
                .joinToString(separator = "")

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

val Node.segmentName: String
    get() = when (type) {
        PACKAGE -> checkNotNull(name).replace(".", "/")
        FILE -> checkNotNull(name).replace(".", "_").replace("$", "_")
        LAMBDA -> {
            if (parents.any { it.type == CLASS })
                "$" + numberInClass.toString()
            else
                "$" + numberInClass
        }
        FUNCTION -> {
            if (parent!!.type == FILE)
                parent.segmentName
            else if (parent.type == PACKAGE)
                "/" + parents.first { it.type == FILE }.segmentName
            else if (parents.any { it.type == FUNCTION })
                "$" + numberInClass
            else
                ""
        }
        CLASS -> {
            if (parent!!.type == PACKAGE)
                "/$name"
            else if (parent.type == FILE)
                name!!
            else
                "$$name"
        }
    }

val Node.numberInClass: Int
    get() = rootClass.DFSSequence.filter(Node::needWrapper).takeWhile { it != this }.count() + 1

val Node.rootClass: Node get() = (parents.first {
    (it.type == CLASS && (it.parent!!.type == FILE || it.parent.type == PACKAGE))
            || it.type == FILE })

val Node.needWrapper: Boolean get() = when (type) {
    FUNCTION -> parents.any { it.type == FUNCTION }
    LAMBDA -> true
    else -> false
}

val Node.DFSSequence: Sequence<Node> get() = object : Sequence<Node> {
    val stack: MutableList<Node> = mutableListOf(this@DFSSequence)
    override fun iterator(): Iterator<Node> = object : Iterator<Node> {
        override fun hasNext(): Boolean = !stack.isEmpty()

        override fun next(): Node {
            val res = stack.removeAt(stack.size - 1)
            if (type != PACKAGE && type != FILE)
                stack.addAll(res.children.reversed())
            else
                stack.addAll(res.children.reversed().filter { it.type != CLASS })
            return res
        }
    }
}

// == Sample utils ==
val Node.packageName: String?
    get() = parents.firstOrNull { it.type == PACKAGE }?.segmentName

val Node.parentsWithSelf: Sequence<Node>
    get() = generateSequence(this) { if (it.type == FILE) null else it.parent }

val Node.parents: Sequence<Node>
    get() = parentsWithSelf.drop(1)

val Node.isNamedClass: Boolean
    get() = type == CLASS || (type == FUNCTION && (parent!!.type == PACKAGE || parent.type == FILE))

