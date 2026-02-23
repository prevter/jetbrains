package org.geodesdk.lint

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.geodesdk.GeodeModJsonService
import org.geodesdk.InspectionsMessageBundle
import org.geodesdk.utils.isCppFile

class GeodeDynamicCastInspection : LocalInspectionTool() {
    override fun getGroupDisplayName() = InspectionsMessageBundle.message("inspection.geode.dynamic_cast.group")
    override fun getDisplayName() = InspectionsMessageBundle.message("inspection.geode.dynamic_cast.display.name")
    override fun getShortName() = "GeodeDynamicCast"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) : PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (!GeodeModJsonService.getInstance(element.project).isGeodeMod()) return
                if (element.firstChild != null) return
                if (!element.containingFile.isCppFile()) return
                if (element.text != "dynamic_cast") return

                holder.registerProblem(
                    element,
                    InspectionsMessageBundle.message("inspection.geode.dynamic_cast.problem.description"),
                    ProblemHighlightType.GENERIC_ERROR,
                    ReplaceWithTypeinfocast()
                )
            }
        }

    private class ReplaceWithTypeinfocast : LocalQuickFix {
        override fun getFamilyName() = "Replace with typeinfo_cast"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val file = element.containingFile
            val document = file.viewProvider.document ?: return

            WriteCommandAction.runWriteCommandAction(project, "Replace With typeinfo_cast", null, {
                document.replaceString(
                    element.textRange.startOffset,
                    element.textRange.endOffset,
                    "typeinfo_cast"
                )
            }, file)
        }
    }
}