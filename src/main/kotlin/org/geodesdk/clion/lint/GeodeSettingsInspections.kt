package org.geodesdk.clion.lint

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.geodesdk.clion.GeodeModJsonService
import org.geodesdk.clion.InspectionsMessageBundle
import org.geodesdk.clion.utils.isCppFile

private val SETTING_FN_NAMES = setOf("getSettingValue", "setSettingValue")

private val ACCEPTED_CPP_TYPES: Map<String, Set<String>> = mapOf(
    "bool" to setOf("bool"),
    "int" to setOf(
        "int64_t", "int", "long", "long long",
        "uint64_t", "unsigned", "unsigned int",
        "unsigned long", "unsigned long long",
        "long long int", "unsigned long long int",
        "long int", "unsigned long int",
        "std::size_t", "size_t",
        "int32_t", "uint32_t"
    ),
    "float" to setOf("double", "float", "long double"),
    "string" to setOf("std::string", "string"),
    "file" to setOf("std::filesystem::path", "filesystem::path", "fs::path"),
    "folder" to setOf("std::filesystem::path", "filesystem::path", "fs::path"),
    "color" to setOf("cocos2d::ccColor3B", "ccColor3B"),
    "rgb" to setOf("cocos2d::ccColor3B", "ccColor3B"),
    "rgba" to setOf("cocos2d::ccColor4B", "ccColor4B"),
    "keybind" to setOf("keybinds::Keybind", "Keybind"),
)

private data class SettingCall(
    val nameElement: PsiElement,
    val nameValue: String,
    val typeElement: PsiElement,
    val typeValue: String,
)

class GeodeUnknownSettingInspection : LocalInspectionTool() {
    override fun getGroupDisplayName() = InspectionsMessageBundle.message("inspection.geode.unknown_setting.group")
    override fun getDisplayName() = InspectionsMessageBundle.message("inspection.geode.unknown_setting.display.name")
    override fun getShortName() = "GeodeUnknownSetting"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.firstChild != null) return
                if (!element.containingFile.isCppFile()) return
                if (element.text !in SETTING_FN_NAMES) return

                val call = extractSettingCall(element) ?: return
                val modJson = GeodeModJsonService.getInstance(element.project)
                    .getModJson(element.containingFile) ?: return
                val setting = modJson.settings[call.nameValue]

                when {
                    setting == null ->
                        holder.registerProblem(
                            call.nameElement,
                            "Unknown setting '${call.nameValue}' - not found in mod.json",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        )

                    setting.type == "title" ->
                        holder.registerProblem(
                            call.nameElement,
                            "Setting '${call.nameValue}' is a title - titles cannot be used as setting values",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        )
                }
            }
        }
}

private fun extractSettingCall(identifierLeaf: PsiElement): SettingCall? {
    val siblings = identifierLeaf.parent?.children
        ?.dropWhile { it !== identifierLeaf }
        ?.drop(1)
        ?.filter { it.node?.elementType?.toString()?.contains("WHITE_SPACE", ignoreCase = true) == false }
        ?: return null

    val iter = siblings.iterator()
    fun next(): PsiElement? = if (iter.hasNext()) iter.next() else null

    if (next()?.text != "<") return null

    val typeTokens = mutableListOf<PsiElement>()
    var tok = next() ?: return null
    while (tok.text != ">") {
        typeTokens += tok
        tok = next() ?: return null
    }

    if (typeTokens.isEmpty()) return null
    if (next()?.text != "(") return null

    val stringLiteral = next() ?: return null
    val raw = stringLiteral.text
    if (!raw.startsWith('"') || !raw.endsWith('"') || raw.length < 2) return null

    val nameValue = raw.removeSurrounding("\"")
    val typeValue = typeTokens.joinToString(" ") { it.text }.trim()

    return SettingCall(
        nameElement = stringLiteral,
        nameValue = nameValue,
        typeElement = typeTokens.first(),
        typeValue = typeValue,
    )
}

class GeodeSettingTypeMismatchInspection : LocalInspectionTool() {
    override fun getGroupDisplayName() =
        InspectionsMessageBundle.message("inspection.geode.setting_type_mismatch.group")

    override fun getDisplayName() =
        InspectionsMessageBundle.message("inspection.geode.setting_type_mismatch.display.name")

    override fun getShortName() = "GeodeSettingTypeMismatch"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.firstChild != null) return
                if (!element.containingFile.isCppFile()) return
                if (element.text !in SETTING_FN_NAMES) return

                val call = extractSettingCall(element) ?: return
                val modJson = GeodeModJsonService.getInstance(element.project)
                    .getModJson(element.containingFile) ?: return
                val setting = modJson.settings[call.nameValue] ?: return

                if (setting.type == "title" || setting.type.startsWith("custom:")) return

                val accepted = ACCEPTED_CPP_TYPES[setting.type] ?: return
                val written = call.typeValue

                val matches = accepted.any { accepted ->
                    written == accepted ||
                            written.endsWith("::$accepted") ||
                            accepted.endsWith("::$written")
                }

                if (!matches) {
                    val canonical = accepted.first()
                    holder.registerProblem(
                        call.typeElement,
                        "Setting '${call.nameValue}' has type '${setting.type}' - expected $canonical, got $written",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    )
                }
            }
        }
}