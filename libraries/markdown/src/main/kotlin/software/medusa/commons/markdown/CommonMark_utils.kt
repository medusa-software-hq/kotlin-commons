package software.medusa.commons.markdown

import org.commonmark.node.Block
import org.commonmark.node.Document
import org.commonmark.node.Node

internal object CommonMark_utils {
  val Node.childNodes: Sequence<Node>
    get() = generateSequence(firstChild) { childNode -> childNode.next }

  val Document.childBlocks: Sequence<Block>
    get() = childNodes.map { childNode ->
      childNode as? Block ?: error("Unexpected top-level node type: ${childNode::class.simpleName}")
    }
}
