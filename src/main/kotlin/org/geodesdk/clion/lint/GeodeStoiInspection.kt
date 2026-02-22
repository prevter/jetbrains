package org.geodesdk.clion.lint

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.geodesdk.clion.GeodeModJsonService
import org.geodesdk.clion.InspectionsMessageBundle
import org.geodesdk.clion.utils.isCppFile

private val STO_NAMES = listOf("stoi", "stol", "stoll", "stoul", "stoull", "stof", "stod", "stold")

class GeodeStoiInspection : LocalInspectionTool() {
    override fun getGroupDisplayName() = InspectionsMessageBundle.message("inspection.geode.stoi.group")
    override fun getDisplayName() = InspectionsMessageBundle.message("inspection.geode.stoi.display.name")
    override fun getShortName() = "GeodeStoi"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) : PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: com.intellij.psi.PsiElement) {
                if (!GeodeModJsonService.getInstance(element.project).isGeodeMod()) return
                if (element.firstChild != null) return
                if (!element.containingFile.isCppFile()) return
                if (element.text !in STO_NAMES) return

                holder.registerProblem(
                    element,
                    InspectionsMessageBundle.message("inspection.geode.stoi.problem.description"),
                    com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR
                )
            }
        }
}

