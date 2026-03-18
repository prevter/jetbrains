package org.geodesdk.execution

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NonNls

class LaunchGameConfigurationFactory(type: LaunchGameConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): @NonNls String {
        return LaunchGameConfigurationType.ID
    }

    override fun createTemplateConfiguration(
        project: Project
    ): RunConfiguration {
        return LaunchGameConfiguration(project, this, "Launch Geometry Dash")
    }

    override fun getOptionsClass(): Class<out BaseState?> {
        return LaunchGameConfigurationOptions::class.java
    }
}
