package org.geodesdk.utils

import com.intellij.psi.PsiFile

fun PsiFile.isCppFile(): Boolean =
    fileElementType?.language?.id == "C++" ||
    fileElementType?.language?.id == "ObjectiveC++" ||
    fileElementType?.language?.id == "ObjectiveC"