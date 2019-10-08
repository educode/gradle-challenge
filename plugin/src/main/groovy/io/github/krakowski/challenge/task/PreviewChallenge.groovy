package io.github.krakowski.challenge.task

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Paths

class PreviewChallenge extends DefaultTask {

    private static final String OUTPUT_FILENAME = "json/challenge.json"

    @TaskAction
    def action() {
        def json = new JsonSlurper().parse("${project.buildDir}/${OUTPUT_FILENAME}" as File) as Map<String, Object>

        def implementation = json.get('skeleton') as Map<String, String>
        def test = json.get('test') as Map<String, String>

        println(implementation.get('content'))
        println()
        println(test.get('content'))
    }
}
