# markdown

A Kotlin library for parsing Markdown into a structured, traversable model.

This module is built on top of [commonmark-java](https://github.com/commonmark/commonmark-java) and provides higher-level domain types such as `MdDocument`, `MdChapter`, `MdBlock`, and inline node models. It also includes utilities for walking inline content and a custom `cc` extension for specialized code block / inline code handling.

## What it does

- Parse Markdown source into a structured representation
- Represent headings and nested sections as chapters
- Traverse block and inline content programmatically
- Render the structured model back to Markdown
- Extend CommonMark with custom `cc` syntax support
