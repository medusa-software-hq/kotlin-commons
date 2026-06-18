package software.medusa.commons.markdown.ext.cc

import org.commonmark.node.CustomNode

internal class CcInlineCode(var literal: String = "") : CustomNode()
