package org.geodesdk.execution

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.geodesdk.utils.GeodeUtils


class LaunchGameConfiguration(
    project: Project,
    factory: LaunchGameConfigurationFactory?,
    name: String?
) : RunConfigurationBase<LaunchGameConfigurationOptions?>(project, factory, name) {
    override fun getOptions(): LaunchGameConfigurationOptions {
        return super.getOptions() as LaunchGameConfigurationOptions
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> {
        return LaunchGameConfigurationSettingsEditor()
    }

    @Throws(ExecutionException::class)
    override fun getState(
        executor: Executor,
        executionEnvironment: ExecutionEnvironment
    ): RunProfileState {
        return object : CommandLineState(executionEnvironment) {
            @Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                // geode.exe
                val sdkVersion = GeodeUtils.detectGeodeSdkVersion()

                if (sdkVersion.isNullOrEmpty()) throw ExecutionException("Could not find SDK version from 'geode sdk version'! Maybe 'geode' could not be found as an environment variable?")

                // I don't know if this is the best code, but it should work.
                val commandLine = GeneralCommandLine("geode", "run", "--stay")

                val processHandler = ProcessHandlerFactory.getInstance()
                    .createColoredProcessHandler(commandLine)

                ProcessTerminatedListener.attach(processHandler)

                return processHandler
            }
        }
    }
}
