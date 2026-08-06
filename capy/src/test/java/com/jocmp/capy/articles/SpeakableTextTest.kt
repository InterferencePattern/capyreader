package com.jocmp.capy.articles

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeakableTextTest {
    @Test
    fun `returns empty string for blank markup`() {
        assertEquals("", speakableText(""))
        assertEquals("", speakableText("   "))
    }

    @Test
    fun `preserves block boundaries as pauses`() {
        val html = """
            <h1>Title</h1>
            <p>First paragraph.</p>
            <p>Second paragraph.</p>
        """.trimIndent()

        assertEquals(
            "Title\n\nFirst paragraph.\n\nSecond paragraph.",
            speakableText(html)
        )
    }

    @Test
    fun `includes list items as separate blocks`() {
        val html = "<ul><li>One</li><li>Two</li></ul>"

        assertEquals("One\n\nTwo", speakableText(html))
    }

    @Test
    fun `drops code blocks`() {
        val html = """
            <p>Before</p>
            <pre><code>fun foo() = Unit</code></pre>
            <p>After</p>
        """.trimIndent()

        assertEquals("Before\n\nAfter", speakableText(html))
    }

    @Test
    fun `keeps inline code inside a sentence`() {
        val html = "<p>Call <code>foo()</code> first.</p>"

        assertEquals("Call foo() first.", speakableText(html))
    }

    @Test
    fun `drops tables`() {
        val html = """
            <p>Before</p>
            <table>
              <tr><td>Cell one</td><td>Cell two</td></tr>
            </table>
            <p>After</p>
        """.trimIndent()

        assertEquals("Before\n\nAfter", speakableText(html))
    }

    @Test
    fun `drops image captions`() {
        val html = """
            <figure>
              <img src="photo.jpg" />
              <figcaption>A caption nobody will hear</figcaption>
            </figure>
            <p>Body text</p>
        """.trimIndent()

        assertEquals("Body text", speakableText(html))
    }

    @Test
    fun `unwraps blocks nested inside container elements`() {
        val html = """
            <div class="content">
              <section>
                <p>Nested paragraph.</p>
              </section>
            </div>
        """.trimIndent()

        assertEquals("Nested paragraph.", speakableText(html))
    }

    @Test
    fun `skips blank blocks`() {
        val html = "<p>Real content.</p><p>   </p><p></p>"

        assertEquals("Real content.", speakableText(html))
    }
}

class PassagesTest {
    @Test
    fun `returns nothing for blank text`() {
        assertEquals(emptyList(), passages(""))
        assertEquals(emptyList(), passages("   \n\n  "))
    }

    @Test
    fun `keeps short text as a single passage`() {
        val text = "One block.\n\nAnother block."

        assertEquals(listOf(text), passages(text))
    }

    @Test
    fun `packs whole blocks up to the limit`() {
        val text = "aaaa\n\nbbbb\n\ncccc"

        assertEquals(listOf("aaaa\n\nbbbb", "cccc"), passages(text, maxLength = 10))
    }

    @Test
    fun `splits at block boundaries rather than mid-sentence`() {
        val blocks = List(20) { "Block number $it is a whole sentence." }

        val result = passages(blocks.joinToString("\n\n"), maxLength = 100)

        assertTrue(result.size > 1)
        result.flatMap { it.split("\n\n") }.forEach { block ->
            assertTrue(block in blocks, "$block is not an intact block")
        }
    }

    @Test
    fun `splits an oversized block at sentence boundaries`() {
        val block = "First sentence here. Second sentence here. Third sentence here."

        assertEquals(
            listOf("First sentence here. Second sentence here.", "Third sentence here."),
            passages(block, maxLength = 50)
        )
    }

    @Test
    fun `every passage stays within the limit`() {
        val text = List(50) { "Sentence $it padded out with filler words to take up room." }
            .chunked(5)
            .joinToString("\n\n") { it.joinToString(" ") }

        passages(text, maxLength = 200).forEach {
            assertTrue(it.length <= 200, "passage of ${it.length} exceeds the limit")
        }
    }

    @Test
    fun `splits a sentence with no boundary to break on`() {
        val result = passages("a".repeat(25), maxLength = 10)

        assertEquals(listOf("aaaaaaaaaa", "aaaaaaaaaa", "aaaaa"), result)
    }
}
