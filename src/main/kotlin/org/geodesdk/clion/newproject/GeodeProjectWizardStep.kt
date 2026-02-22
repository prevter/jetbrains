package org.geodesdk.clion.newproject

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.platform.DirectoryProjectGenerator

open class GeodeProjectWizardStep(generator: DirectoryProjectGenerator<GeodeProjectSettings>) :
    ProjectSettingsStepBase<GeodeProjectSettings>(generator, AbstractNewProjectStep.AbstractCallback())
