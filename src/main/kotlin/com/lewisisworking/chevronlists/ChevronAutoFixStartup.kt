/**
 * ChevronAutoFixStartup.kt
 * Runs on project open and ensures the application-level
 * ChevronAutoFixListener service is constructed. Without this, the listener
 * would only be created when something else first calls service<...>().
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ChevronAutoFixStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        ChevronAutoFixListener.ensureInitialised()
    }
}