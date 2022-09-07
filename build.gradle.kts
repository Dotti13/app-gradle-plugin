/*
 * Copyright 2022 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
import net.researchgate.release.GitAdapter.GitConfig
import java.util.Date
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    id("java")
    id("maven")
    id("java-gradle-plugin")
    id("net.researchgate.release") version "2.6.0"
    id("com.github.sherter.google-java-format") version "0.8"
    id("checkstyle")
    id("jacoco")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

group = "com.google.cloud.tools"

dependencies {
    compile(localGroovy())
    compile(gradleApi())
    compile("com.google.cloud.tools:appengine-plugins-core:0.9.9")

    testCompile("commons-io:commons-io:2.4")
    testCompile("junit:junit:4.12")
    testCompile("org.hamcrest:hamcrest-library:1.3")
    testCompile("org.mockito:mockito-core:2.23.4")
}


tasks.wrapper {
    gradleVersion = "6.9"
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Built-By" to System.getProperty("user.name"),
                "Built-Date" to Date(),
                "Built-JDK" to System.getProperty("java.version"),
                "Built-Gradle" to gradle.gradleVersion
            )
        )
    }
}

// <editor-fold defaultstate="expanded" desc="Testing">
tasks.test {
    testLogging {
        showStandardStreams = true
        exceptionFormat = FULL
    }
}

val integTestSourceSet = sourceSets.create("integTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations {
    "integTestImplementation" {
        extendsFrom(testImplementation.get())
    }
    "integTestRuntime" {
        extendsFrom(testRuntime.get())
    }
}

val integTest by tasks.registering(Test::class) {
    testClassesDirs = integTestSourceSet.output.classesDirs
    classpath = integTestSourceSet.runtimeClasspath
    outputs.upToDateWhen { false }
}
// </editor-fold>

// <editor-fold defaultstate="expanded" desc="Releasing">
tasks.register<Jar>("sourceJar") {
    from(sourceSets.main.get().allJava)
    classifier = "sources"
}

tasks.register<Jar>("javadocJar") {
    dependsOn(tasks.javadoc)
    from(tasks.javadoc.map { it.destinationDir!! })
    classifier = "javadoc"
}

project.afterEvaluate {
    tasks.register("writePom") {
        val outputFile = file("$buildDir/pom/${project.name}-${project.version}.pom")
        outputs.file(outputFile)

        doLast {
            maven {
                pom {
                    project {
                        withGroovyBuilder {
                            "name"("App Engine Gradle Plugin")
                            "description"("This Gradle plugin provides tasks to build and deploy Google App Engine applications.")

                            "url"("https://github.com/GoogleCloudPlatform/app-gradle-plugin")
                            "inceptionYear"("2016")

                            "scm" {
                                "url"("https://github.com/GoogleCloudPlatform/app-gradle-plugin")
                                "connection"("scm:https://github.com/GoogleCloudPlatform/app-gradle-plugin.git")
                                "developerConnection"("scm:git://github.com/GoogleCloudPlatform/app-gradle-plugin.git")
                            }

                            "licenses" {
                                "license" {
                                    "name"("The Apache Software License, Version 2.0")
                                    "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                    "distribution"("repo")
                                }
                            }
                            "developers" {
                                "developer" {
                                    "id"("loosebazooka")
                                    "name"("Appu Goundan")
                                    "email"("appu@google.com")
                                }
                            }
                        }
                    }
                }.writeTo(outputFile)
            }
        }
    }
}


// for kokoro releases
tasks.register<Sync>("prepareRelease") {
    from(tasks.jar)
    from(tasks.named("sourceJar"))
    from(tasks.named("javadocJar"))
    from(tasks.named("writePom"))

    into("${buildDir}/release-artifacts")

    dependsOn(tasks.build)
}

release {
    tagTemplate = "v\$version"
    getProperty("git").apply {
        this as GitConfig
        requireBranch = """^release-v\d+.*$"""  //regex
    }
}
// </editor-fold>

// <editor-fold defaultstate="expanded" desc="Formatting">
googleJavaFormat {
    toolVersion = "1.7"
}


tasks.check {
    dependsOn(tasks.verifyGoogleJavaFormat)
}
// to auto-format run ./gradlew googleJavaFormat

checkstyle {
    toolVersion = "8.18"
    // get the google_checks.xml file from the actual tool we"re invoking)
    config = resources.text.fromArchiveEntry(
        configurations.checkstyle.map { it.first() },
        "google_checks.xml"
    )
    maxErrors = 0
    maxWarnings = 0
}

tasks.checkstyleTest {
    enabled = false
}
// </editor-fold>

// <editor-fold defaultstate="expanded" desc="Test coverage">
jacoco {
    toolVersion = "0.8.6"
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        html.isEnabled = false
    }
}
// </editor-fold>
