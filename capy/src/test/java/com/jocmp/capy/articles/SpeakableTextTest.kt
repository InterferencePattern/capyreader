package com.jocmp.capy.articles

import kotlin.test.Test
import kotlin.test.assertEquals

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
