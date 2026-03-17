package org.geodesdk.execution

import com.intellij.openapi.options.SettingsEditor
import com.intellij.util.ui.FormBuilder
import org.jetbrains.annotations.NotNull
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField


class LaunchGameConfigurationSettingsEditor : SettingsEditor<LaunchGameConfiguration>() {

    private var launchRootPanel: JPanel? = null

    init {
        launchRootPanel = FormBuilder.createFormBuilder()
            .panel
    }

    override fun resetEditorFrom(s: LaunchGameConfiguration) {
    }

    override fun applyEditorTo(s: LaunchGameConfiguration) {
    }

    @NotNull
    override fun createEditor(): JComponent {
        return launchRootPanel!!
    }
}
