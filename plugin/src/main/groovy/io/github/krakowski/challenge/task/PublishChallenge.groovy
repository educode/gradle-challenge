package io.github.krakowski.challenge.task

import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder
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

        def username = ''
        def password = ''
        new SwingBuilder().edt {
            dialog(modal: true,
                    title: 'Enter password',
                    alwaysOnTop: true,
                    resizable: false,
                    locationRelativeTo: null,
                    pack: true,
                    show: true
            ) {
                vbox { // Put everything below each other
                    label(text: "username:")
                    usernameInput = textField()
                    label(text: "password:")
                    passwordInput = passwordField()
                    button(defaultButton: true, text: 'OK', actionPerformed: {
                        username = usernameInput.text
                        password = passwordInput.password
                        dispose()
                    })
                }
            }
        }

        def token = login(username, password)
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
        if(postRC.equals(200)) {
            return
        } else if (postRC.equals(409)) {
            throw new RuntimeException("Challenge already exists")
        } else if (postRC.equals(403)) {
            throw new RuntimeException("Permission denied")
        } else {
            project.logger.error("HTTP ERROR {}", postRC)
            throw new RuntimeException("Publishing failed")
        }
    }
}
