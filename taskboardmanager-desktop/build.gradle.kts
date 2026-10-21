plugins {
    application
    id("org.openjfx.javafxplugin")
}

dependencies {
    implementation(project(":taskboardmanager-core"))
    implementation(project(":taskboardmanager-persistence"))

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

javafx {
    version = "21.0.12"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("org.takoyaki.taskboardmanager.desktop.TaskBoardManagerApplication")
}

tasks.test {
    useJUnitPlatform()
}
