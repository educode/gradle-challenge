package io.github.krakowski.challenge.task


import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.visitor.VoidVisitor
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter
import groovy.json.JsonOutput
import io.github.krakowski.challenge.Difficulty
import io.github.krakowski.challenge.Remove
import io.github.krakowski.challenge.RemoveBody
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.yaml.snakeyaml.Yaml

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class GenerateChallenge extends DefaultTask {

    private static final String META_FILENAME = "challenge.yml"
    private static final String OUTPUT_FILENAME = "json/challenge.json"
    private static final String DESCRIPTION_FILENAME = "README.md"

    @TaskAction
    def action() {
        def idSplit = project.name.split("-")
        if (idSplit.length != 2) {
            throw new IllegalStateException("Invalid project name provided")
        }

        def id = idSplit[1];
        def metaPath = Paths.get("${project.projectDir}/${META_FILENAME}")
        if (Files.notExists(metaPath)) {
            throw new IllegalStateException("Metadata file is missing within project")
        }

        def descriptionPath = Paths.get("${project.projectDir}/${DESCRIPTION_FILENAME}")
        if (Files.notExists(descriptionPath)) {
            throw new IllegalStateException("Description file is missing within project")
        }

        def metaData = new Yaml().loadAs(Files.readString(metaPath), MetaData.class)
        def description = Files.readString(descriptionPath)

        def implementationPath = "${project.projectDir}/src/main/java/${metaData.implementationClass.replace(".", "/").concat(".java")}"
        def testPath = "${project.projectDir}/src/test/java/${metaData.testClass.replace(".", "/").concat(".java")}"

        def parserConfig = new ParserConfiguration()
        parserConfig.characterEncoding = StandardCharsets.UTF_8
        parserConfig.languageLevel = ParserConfiguration.LanguageLevel.JAVA_11
        StaticJavaParser.setConfiguration(parserConfig)

        def implementationProcessor = new SourceProcessor()
        def implementation = parseSource(Paths.get(implementationPath), implementationProcessor)
        implementationProcessor.postProcess()

        def testProcessor = new SourceProcessor()
        def test = parseSource(Paths.get(testPath), testProcessor)
        testProcessor.postProcess()

        def challenge = [
                id: id,
                title: metaData.title,
                description: description,
                thumbnail: metaData.thumbnailUrl,
                startTime: metaData.startTime,
                endTime: metaData.endTime,
                plagCheck: metaData.plagCheck,
                locked: metaData.locked,
                unlockDeadline: metaData.unlockDeadline,
                skeleton: [
                        className: metaData.implementationClass,
                        content: LexicalPreservingPrinter.print(implementation)
                ],
                test: [
                        className: metaData.testClass,
                        content: LexicalPreservingPrinter.print(test)
                ],
                rewards: testProcessor.rewards
        ]

        def json = JsonOutput.toJson(challenge)
        def path = Paths.get("${project.buildDir}/${OUTPUT_FILENAME}")
        Files.createDirectories(path.parent)
        Files.writeString(path, json)
    }

    private static def parseSource(Path path, VoidVisitor<Void>... visitors) {
        def compilationUnit = StaticJavaParser.parse(path)
        compilationUnit = LexicalPreservingPrinter.setup(compilationUnit)
        visitors.each { compilationUnit.accept(it, null) }
        return compilationUnit
    }

    private static def isPluginClass(String name) {
        return name.startsWith('io.github.krakowski.challenge')
    }

    private static class MetaData {
        public String title
        public String implementationClass
        public String testClass
        public String thumbnailUrl
        public String startTime
        public String endTime
        public Boolean plagCheck
        public Boolean locked
        public String unlockDeadline;
    }

    private static class SourceProcessor extends VoidVisitorAdapter<Void> {

        List<Node> toRemove = new ArrayList<>()
        Set<Reward> rewards = new HashSet<>()

        @Override
        void visit(MethodDeclaration node, Void arg) {
            if (node.isAnnotationPresent(Difficulty)) {
                def annotation = node.getAnnotationByClass(Difficulty).get().asSingleMemberAnnotationExpr()
                def value = annotation.getMemberValue().asNameExpr().name
                def level = Difficulty.Level.valueOf(value.toString())
                rewards.add(new Reward(node.getNameAsString(), level.points))
                toRemove.add(annotation)
            }

            if (node.isAnnotationPresent(RemoveBody)) {
                node.setBody(new BlockStmt())
                toRemove.add(node.getAnnotationByClass(RemoveBody).get())
            }

            if (node.isAnnotationPresent(Remove)) {
                toRemove.add(node)
                toRemove.add(node.getAnnotationByClass(Remove).get())
            }

            super.visit(node, arg)
        }

        @Override
        void visit(ImportDeclaration node, Void arg) {
            if (isPluginClass(node.getNameAsString())) {
                toRemove.add(node)
            }

            super.visit(node, arg)
        }

        void postProcess() {
            toRemove.forEach { it.remove() }
        }
    }

    private static class Reward {
        String methodName;
        int points;

        Reward(String methodName, int points) {
            this.methodName = methodName
            this.points = points
        }

        boolean equals(o) {
            if (this.is(o)) return true
            if (getClass() != o.class) return false

            Reward reward = (Reward) o

            if (points != reward.points) return false
            if (methodName != reward.methodName) return false

            return true
        }

        int hashCode() {
            int result
            result = methodName.hashCode()
            result = 31 * result + points
            return result
        }
    }
}
