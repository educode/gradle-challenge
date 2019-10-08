package io.github.krakowski.challenge


import io.github.krakowski.challenge.task.GenerateChallenge
import io.github.krakowski.challenge.task.PreviewChallenge
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.idea.IdeaPlugin

class ChallengePlugin implements Plugin<Project> {

    private static final String JSON_DIR = 'json'

    void apply(Project project) {
        project.pluginManager.apply(IdeaPlugin)
        project.pluginManager.apply(EclipsePlugin)
        project.pluginManager.apply(JavaPlugin)

        project.tasks.create('generateChallenge', GenerateChallenge)
        project.tasks.create('previewChallenge', PreviewChallenge)

        project.tasks.previewChallenge.dependsOn('generateChallenge')
    }
}
