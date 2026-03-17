package org.geodesdk.execution

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NotNullFactory
import com.intellij.openapi.util.NotNullLazyValue
import org.geodesdk.icons.GeodeIcons

class LaunchGameConfigurationType internal constructor() : ConfigurationTypeBase (
    ID, "Launch Geometry Dash", "Launches Geometry Dash using the project's current profile.",
    NotNullLazyValue.createValue(NotNullFactory { GeodeIcons.GeometryDash() }),
) {
    init {
        addFactory(LaunchGameConfigurationFactory(this))
    }

    companion object {
        const val ID: String = "LaunchGameRunConfiguration"
    }
}
