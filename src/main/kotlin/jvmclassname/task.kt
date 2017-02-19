package jvmclassname

import com.sun.org.apache.xpath.internal.operations.Bool
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

fun jvmClassName(node: Node): String {
    return when  {
        node.isTopLevelClass    -> node.modifiedPackageName + node.name
        node.isTopLevelFunction -> node.modifiedPackageName + node.modifiedFileName
        node.isClass            -> jvmClassName(node.parent!!) + "$" + node.name!!
        node.isLocalFunction || node.isLambda -> {
            val root = node.firstParentNonlocalFuncOrClass!!
            jvmClassName(root) + "$" + node.num.toString()
        }
        // member:
        node.isFunction -> jvmClassName(node.parent!!)
        else -> throw IllegalArgumentException("JVM class for this type of node should not be created.")
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

val Node.fileName: String
    get() = file.name!!

val Node.file: Node
    get() = parentsWithSelf.first { it.type == FILE }

val Node.modifiedPackageName: String
    get () = if (packageName != null) Regex("\\.").replace(packageName!!, "/") + "/" else ""

val Node.modifiedFileName: String
    get () = Regex("\\.|\\$").replace(fileName, "_")

val Node.parentsWithSelf: Sequence<Node>
    get() = generateSequence(this) { if (it.type == FILE) null else it.parent }

val Node.parents: Sequence<Node>
    get() = parentsWithSelf.drop(1)

val Node.isClass: Boolean
    get() = type == CLASS

val Node.isFile: Boolean
    get() = type == FILE

val Node.isPackage: Boolean
    get() = type == PACKAGE

val Node.isLambda: Boolean
    get() = type == LAMBDA

val Node.isFunction: Boolean
    get() = type == FUNCTION

val Node.isTopLevelClass: Boolean
    get() = isClass && (parent!!.isFile || parent.isPackage)

val Node.isTopLevelFunction: Boolean
    get() = isFunction && (parent!!.isFile || parent.isPackage)

val Node.isLocalFunction: Boolean
    get() = isFunction && !(parent!!.isClass || parent.isFile || parent.isPackage)

val Node.firstParentNonlocalFuncOrClass: Node?
    get() = parentsWithSelf.firstOrNull { it.isClass || (it.isFunction && !it.isLocalFunction) }

val Node.topLevelParent: Node?
    get() = parentsWithSelf.firstOrNull { it.isTopLevelFunction || it.isTopLevelClass }

val Node.num: Int
    get() {
        val root = topLevelParent!!
        return if (root.isClass) enumerate(root, this) else enumerate(root.file, this, Node::isClass)
    }

fun enumerate(root: Node, target: Node, toSkip: (Node) -> Boolean = { false }): Int {
    val (res, isValid) = dfs(root, target, 1,toSkip)
    assert(isValid)
    return res
}

fun dfs(current: Node, target: Node, currNum: Int, toSkip: (Node) -> Boolean): Pair<Int, Boolean> {
    if (current == target) return Pair(currNum, true)
    val currentCounts = current.isLambda || current.isLocalFunction
    var res = Pair(if (currentCounts) currNum + 1 else currNum, false)
    for (child in current.children) {
        if (toSkip(child)) continue
        res = dfs(child, target, res.first, toSkip)
        if (res.second) return res
    }
    return res
}



