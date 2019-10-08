package io.github.krakowski.challenge.task

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Paths

class PublishChallenge extends DefaultTask {

    private static final String LOGIN_URL = "https://educode.cs.hhu.de/api/auth/v1/authenticate"
    private static final String PUBLISH_URL = "https://educode.cs.hhu.de/api/catalogue/v1/challenges"
    private static final String OUTPUT_FILENAME = "json/challenge.json"

    @TaskAction
    def action() {
        def log = project.logger

        log.lifecycle("Pleas provide your educode username: ")
        def user = System.in.newReader().readLine()

        log.lifecycle("Please provide your educode password: ")
        def password = System.in.newReader().readLine()

        def token = login(user, password)
        publish(token)
    }

    def login(username, password) {
        def post = new URL(LOGIN_URL).openConnection();
        def message = "{\"username\":\"${username}\",\"password\":\"${password}\"}"
        post.setRequestMethod("POST")
        post.setDoOutput(true)
        post.setRequestProperty("Content-Type", "application/json")
        post.getOutputStream().write(message.getBytes("UTF-8"));
        def postRC = post.getResponseCode();
        if(postRC.equals(200)) {
            return new JsonSlurper().parse(post.getInputStream())['token'];
        } else {
            throw new RuntimeException("Login failed")
        }
    }

    def publish(token) {
        def challengePath = Paths.get("${project.buildDir}/${OUTPUT_FILENAME}")
        def challenge = Files.readString(challengePath)
        def post = new URL(PUBLISH_URL).openConnection();
        post.setRequestMethod("POST")
        post.setDoOutput(true)
        post.setRequestProperty("Content-Type", "application/json")
        post.setRequestProperty("Authorization", "Bearer ${token}")
        post.getOutputStream().write(challenge.getBytes("UTF-8"));
        def postRC = post.getResponseCode();
        project.logger.lifecycle(String.valueOf(postRC))
        if(postRC.equals(200)) {
            return
        } else if (postRC.equals(409)) {
            throw new RuntimeException("Challenge already exists")
        } else if (postRC.equals(403)) {
            throw new RuntimeException("Permission denied")
        } else {
            throw new RuntimeException("Publishing failed")
        }
    }
}
