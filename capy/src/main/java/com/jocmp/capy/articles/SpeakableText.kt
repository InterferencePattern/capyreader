package com.jocmp.capy.articles

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Walks an article's markup into blocks -- headings, paragraphs, list items, blockquotes --
 * and joins them with blank lines that a Speech Provider reads as pauses. Content that cannot
 * be listened to (code blocks, tables, image captions) is dropped entirely.
 */
fun speakableText(html: String): String {
    if (html.isBlank()) {
        return ""
    }

    val body = Jsoup.parse(html).body()

    // An inline `<code>` span is left alone -- it is part of the sentence around it. `<pre>` is
    // what makes a code block.
    body.select("pre, table, figcaption").remove()

    return collectBlocks(body)
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
}

private val blockTags = setOf("h1", "h2", "h3", "h4", "h5", "h6", "p", "li", "blockquote")

private fun collectBlocks(element: Element): List<String> =
    element.children().flatMap { child ->
        if (child.tagName() in blockTags) {
            listOf(child.text().trim())
        } else {
            collectBlocks(child)
        }
    }
