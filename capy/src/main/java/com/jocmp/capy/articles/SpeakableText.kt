package com.jocmp.capy.articles

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.BreakIterator
import java.util.Locale

private const val BLOCK_SEPARATOR = "\n\n"

// Comfortably under OpenAI's 4,096-character ceiling; roughly four minutes of speech.
const val MAX_PASSAGE_LENGTH = 4_000

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
        .joinToString(BLOCK_SEPARATOR)
}

/**
 * Splits Speakable Text into Passages small enough for a Speech Provider to accept in one
 * request. Blocks are packed whole, so a Passage boundary always falls where the text already
 * had a pause. A block too long to stand alone is broken at sentence boundaries instead.
 */
fun passages(text: String, maxLength: Int = MAX_PASSAGE_LENGTH): List<String> {
    val blocks = text.split(BLOCK_SEPARATOR)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .flatMap { splitOversizedBlock(it, maxLength) }

    return pack(blocks, BLOCK_SEPARATOR, maxLength)
}

private fun splitOversizedBlock(block: String, maxLength: Int): List<String> {
    if (block.length <= maxLength) {
        return listOf(block)
    }

    // ponytail: a single sentence over the limit is cut mid-sentence -- it takes a 4,000
    // character sentence to get here, and the alternative is a request the provider rejects.
    return pack(
        chunks = sentences(block).flatMap { it.chunked(maxLength) },
        separator = " ",
        maxLength = maxLength,
    )
}

private fun sentences(text: String): List<String> {
    val iterator = BreakIterator.getSentenceInstance(Locale.getDefault())
    iterator.setText(text)

    val boundaries = generateSequence(iterator.first()) {
        iterator.next().takeIf { it != BreakIterator.DONE }
    }

    return boundaries.zipWithNext { start, end -> text.substring(start, end).trim() }
        .filter { it.isNotEmpty() }
        .toList()
}

/** Greedily joins chunks into the fewest strings of at most [maxLength] characters. */
private fun pack(chunks: List<String>, separator: String, maxLength: Int): List<String> =
    chunks.fold(mutableListOf()) { packed, chunk ->
        val last = packed.lastOrNull()

        if (last != null && last.length + separator.length + chunk.length <= maxLength) {
            packed[packed.lastIndex] = last + separator + chunk
        } else {
            packed.add(chunk)
        }

        packed
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
