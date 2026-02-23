package org.geodesdk

import com.intellij.lang.Language
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ElementColorProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.geodesdk.utils.isCppFile
import java.awt.Color
import java.util.Locale
import kotlin.text.iterator

enum class ColorStructType(
    val keyword: String,
    val componentCount: Int,
    val isFloat: Boolean,
    val openDelimiter: Char,
    val closeDelimiter: Char
) {
    CCC3("ccc3", 3, false, '(', ')'),
    CCC4("ccc4", 4, false, '(', ')'),
    CCCOLOR3B("ccColor3B", 3, false, '{', '}'),
    CCCOLOR4B("ccColor4B", 4, false, '{', '}'),
    CCCOLOR4F("ccColor4F", 4, true, '{', '}');

    companion object {
        fun fromName(name: String): ColorStructType? = entries.find { it.keyword == name }
    }
}

class CCColorProvider : ElementColorProvider {
    override fun getColorFrom(element: PsiElement): Color? {
        if (!element.containingFile.isCppFile()) return null

        val fnName = element.text
        val colorType = ColorStructType.fromName(fnName) ?: return null

        val callRange = findCallRangeInDocument(element, colorType) ?: return null
        val callText = element.containingFile.viewProvider.document?.getText(callRange) ?: return null

        return parseColorFromText(callText, colorType)
    }

    override fun setColorTo(element: PsiElement, color: Color) {
        if (!element.containingFile.isCppFile()) return

        val fnName = element.text
        val colorType = ColorStructType.fromName(fnName) ?: return

        val callRange = findCallRangeInDocument(element, colorType) ?: return
        val file = element.containingFile ?: return
        val doc = file.viewProvider.document ?: return
        val callText = doc.getText(callRange)
        val hex = callText.contains("0x", ignoreCase = true)

        val channels = buildList {
            add(color.red); add(color.green); add(color.blue)
            if (colorType.componentCount == 4) add(color.alpha)
        }

        val formattedChannels = channels.joinToString(", ") {
            formatColorComponent(it, colorType.isFloat, hex)
        }
        val newText = "${fnName}${colorType.openDelimiter}${formattedChannels}${colorType.closeDelimiter}"
        WriteCommandAction.runWriteCommandAction(
            element.project, "Update Cocos Color", null,
            { doc.replaceString(callRange.startOffset, callRange.endOffset, newText) },
            file
        )
    }

    private fun findCallRangeInDocument(callee: PsiElement, colorType: ColorStructType): TextRange? {
        val doc = callee.containingFile?.viewProvider?.document ?: return null
        val docText = doc.charsSequence
        val nameEnd = callee.textRange.endOffset

        if (nameEnd >= docText.length || docText[nameEnd] != colorType.openDelimiter) return null

        var depth = 0
        var i = nameEnd
        while (i < docText.length) {
            when (docText[i]) {
                colorType.openDelimiter -> depth++
                colorType.closeDelimiter -> {
                    depth--; if (depth == 0) return TextRange(callee.textRange.startOffset, i + 1)
                }
            }
            i++
        }
        return null
    }

    @Suppress("UseJBColor")
    private fun parseColorFromText(text: String, colorType: ColorStructType): Color? {
        val open = text.indexOf(colorType.openDelimiter)
        val close = text.lastIndexOf(colorType.closeDelimiter)
        if (open !in 0..<close) return null

        val args = splitArgs(text.substring(open + 1, close), colorType)
        if (args.size != colorType.componentCount) return null

        val components = args.map { parseColorComponent(it.trim(), colorType.isFloat) ?: return null }
        return Color(
            components[0] / 255f,
            components[1] / 255f,
            components[2] / 255f,
            if (colorType.componentCount == 4) components[3] / 255f else 1f
        )
    }

    private fun splitArgs(args: String, colorType: ColorStructType): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()
        for (c in args) {
            when {
                c == colorType.openDelimiter -> {
                    depth++
                    current.append(c)
                }

                c == colorType.closeDelimiter -> {
                    depth--
                    current.append(c)
                }

                c == ',' && depth == 0 -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }

                else -> current.append(c)
            }
        }
        if (current.isNotBlank()) result.add(current.toString())
        return result
    }

    private fun parseColorComponent(str: String, isFloat: Boolean): Int? {
        return if (isFloat) {
            str.toFloatOrNull()?.let { (it.coerceIn(0f..1f) * 255).toInt() }
        } else {
            if (str.startsWith("0x", ignoreCase = true)) {
                str.substring(2).toIntOrNull(16)
            } else {
                str.toIntOrNull()
            }?.takeIf { it in 0..255 }
        }
    }

    private fun formatColorComponent(component: Int, isFloat: Boolean, hex: Boolean): String {
        val value = component.coerceIn(0, 255)
        return when {
            isFloat -> {
                val s = String.format(Locale.US, "%.4f", value / 255f).trimEnd('0')
                "${s}f"
            }

            hex -> "0x%02X".format(value)
            else -> value.toString()
        }
    }
}