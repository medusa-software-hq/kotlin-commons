package software.medusa.commons.markdown.ext.cc

import org.commonmark.node.CustomBlock

internal class CcCodeBlock : CustomBlock() {
  var literal: String = ""
}
