plugins {
	kotlin("jvm") version "2.1.10"
}

group = "bread_experts_group"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	testImplementation(kotlin("test"))
	implementation(kotlin("reflect"))
}

tasks.test {
	useJUnitPlatform()
}
kotlin {
	jvmToolchain(21)
}