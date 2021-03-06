plugins {
    kotlin("js")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(npm("decamelize", "4.0.0", true))
}

kotlin {
    js(BOTH) {
        useCommonJs()
        nodejs()
    }
}