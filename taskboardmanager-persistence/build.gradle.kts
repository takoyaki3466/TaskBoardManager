plugins {
    id("java-library")
}

dependencies {
    api(project(":taskboardmanager-model"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.10")
}
