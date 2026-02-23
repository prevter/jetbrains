@file:Suppress("UseJBColor")

package org.geodesdk

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.awt.Color
import java.awt.Font

private val GD_TAG_COLORS: Map<String, Color> = mapOf(
    "cb" to Color(74, 82, 225),
    "cg" to Color(64, 227, 72),
    "cl" to Color(96, 171, 239),
    "cj" to Color(50, 200, 255),
    "cy" to Color(255, 255, 0),
    "co" to Color(255, 165, 75),
    "cr" to Color(255, 90, 90),
    "cp" to Color(255, 0, 255),
    "ca" to Color(150, 50, 255),
    "cd" to Color(255, 150, 255),
    "cc" to Color(255, 255, 150),
    "cf" to Color(150, 255, 255),
    "cs" to Color(255, 220, 65),
)

private val GD_TAG_ERROR_COLOR = Color(255, 0, 0)

private fun Color.dimmed(): Color = Color(
    (red * 0.5).toInt(),
    (green * 0.5).toInt(),
    (blue * 0.5).toInt(),
    alpha
)

private val COLOR_TAG_REGEX = Regex(
    """<(c[a-z]+)>(.*?)</?c>""",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
)

class GDColorTagAnnotation : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val raw = element.text
        if (raw.isEmpty() || raw.length < 6) return

        if (element.language.id == "C++" || element.language.id == "ObjectiveC") {
            if (!raw.startsWith('"') && !raw.startsWith("R\"") &&
                !raw.startsWith("u\"") && !raw.startsWith("U\"") &&
                !raw.startsWith("L\"")
            ) return
        }

        val elementStart = element.textRange.startOffset
        for (match in COLOR_TAG_REGEX.findAll(raw)) {
            val tag = match.groupValues[1].lowercase()
            val color = GD_TAG_COLORS[tag] ?: GD_TAG_ERROR_COLOR

            val fullRange = match.range
            val contentRange = match.groups[2]!!.range
            val openEnd = fullRange.first + 1 + tag.length + 1 // < + tag + >

            annotateRange(
                holder,
                elementStart + fullRange.first,
                elementStart + openEnd,
                color.dimmed(),
                bold = false
            )

            annotateRange(
                holder,
                elementStart + contentRange.first,
                elementStart + contentRange.last + 1,
                color,
                bold = true
            )

            annotateRange(
                holder,
                elementStart + contentRange.last + 1,
                elementStart + fullRange.last + 1,
                color.dimmed(),
                bold = false
            )
        }
    }

    private fun annotateRange(
        holder: AnnotationHolder,
        startOff: Int,
        endOff: Int,
        color: Color,
        bold: Boolean
    ) {
        if (startOff >= endOff) return
        val range = TextRange(startOff, endOff)
        val attrs = TextAttributes(
            color,
            null,
            null,
            null,
            if (bold) Font.BOLD else Font.PLAIN
        )

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .enforcedTextAttributes(attrs)
            .create()
    }
}