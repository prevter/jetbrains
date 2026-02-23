package org.geodesdk.lint

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.geodesdk.GeodeModJsonService
import org.geodesdk.InspectionsMessageBundle
import org.geodesdk.utils.isCppFile

class GeodeCoutInspection : LocalInspectionTool() {
    override fun getGroupDisplayName() = InspectionsMessageBundle.message("inspection.geode.cout.group")
    override fun getDisplayName() = InspectionsMessageBundle.message("inspection.geode.cout.display.name")
    override fun getShortName() = "GeodeCout"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) : PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (!GeodeModJsonService.getInstance(element.project).isGeodeMod()) return
                if (element.firstChild != null) return
                if (!element.containingFile.isCppFile()) return
                if (element.text != "cout") return

                holder.registerProblem(
                    element,
                    InspectionsMessageBundle.message("inspection.geode.cout.problem.description"),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }
}