package jvmclassname.builder

import jvmclassname.*

class NodeBuilder(
        val type: NodeType,
        val name: String?) {

    private val childrenBuilders: MutableList<NodeBuilder> = java.util.ArrayList<NodeBuilder>()
    private var key: String? = null

    val mapping: MutableMap<String, Node> = java.util.HashMap<String, Node>()

    fun node(type: NodeType, name: String?, f: NodeBuilder.() -> Unit) {
        val builder = NodeBuilder(type, name)
        childrenBuilders.add(builder)
        builder.f()
    }

    fun build(parent: Node?): Node {
        val resultNode = Node(type, name, parent, emptyList())
        resultNode.children = childrenBuilders.map { it.build(resultNode) }

        if (key != null) {
            mapping[key!!] = resultNode
        }

        childrenBuilders.forEach { it.mapping.forEach { mapping[it.key] = it.value } }

        return resultNode
    }

    fun store(key: String) {
        this.key = key
    }
}

fun PACKAGE(name: String, f: NodeBuilder.() -> Unit) = builder(NodeType.PACKAGE, name, f)
fun FILE(name: String, f: NodeBuilder.() -> Unit) = builder(NodeType.CLASS, name, f)
fun NodeBuilder.PACKAGE(name: String, f: NodeBuilder.() -> Unit) = node(NodeType.PACKAGE, name, f)
fun NodeBuilder.FILE(name: String, f: NodeBuilder.() -> Unit) = node(NodeType.FILE, name, f)
fun NodeBuilder.CLASS(name: String, f: NodeBuilder.() -> Unit) = node(NodeType.CLASS, name, f)
fun NodeBuilder.LAMBDA(name: String, f: NodeBuilder.() -> Unit) = node(NodeType.LAMBDA, name, f)
fun NodeBuilder.FUNCTION(name: String, f: NodeBuilder.() -> Unit) = node(NodeType.FUNCTION, name, f)

fun builder(type: NodeType, name: String?, f: NodeBuilder.() -> Unit): NodeBuilder {
    val builder = NodeBuilder(type, name)
    builder.f()
    builder.build(null)
    return builder
}