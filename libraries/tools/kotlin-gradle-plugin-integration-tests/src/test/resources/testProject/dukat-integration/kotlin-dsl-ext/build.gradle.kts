plugins {
    kotlin("js")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        useCommonJs()
        nodejs()
    }

    sourceSets {
        val main by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(npm("left-pad", "1.3.0"))
                implementation(npm("decamelize", "4.0.0", true))
            }
        }
    }
}