import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.util.*

plugins {
    java
    id("org.springframework.boot") version "3.0.+"
    id("io.spring.dependency-management") version "+"
    id("org.graalvm.buildtools.native") version "+"
}

group = "net.vicp.biggee.aot.vpn.expressvpn"
version = "1.0.0-rc3"

java {
    toolchain {
//        languageVersion = JavaLanguageVersion.of(21)
    }
}

val isArm = System.getProperty("os.arch").lowercase(Locale.getDefault()).contains("aarch64")

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/spring") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

extra["springCloudVersion"] = "2022.+"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("com.h2database:h2")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.shell:spring-shell-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly("io.micronaut.sql:micronaut-jdbc-hikari:+")

    implementation("org.springframework.cloud:spring-cloud-starter-zookeeper-discovery")
//    implementation("org.springframework.cloud:spring-cloud-starter-zookeeper-config")
}


dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
    options.forkOptions.jvmArgs?.add("-XX:ParallelGCThreads=4")
    options.encoding = "UTF-8"
    options.release.set(17)
}

graalvmNative {
    toolchainDetection.set(true)
    binaries {
        named("main") {
            imageName.set("dialer")
            buildArgs.add("-H:+UnlockExperimentalVMOptions")
            if (isArm) {
                buildArgs.add("-march=native")
            } else {
                buildArgs.add("-march=x86-64-v1")
            }
//             buildArgs.add("--no-fallback")
//             buildArgs.add("-H:-DebugInfo")
            buildArgs.add("-Djava.util.logging.ConsoleHandler.level=FINE")
            buildArgs.add("--trace-class-initialization=org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl")
//             buildArgs.add("--initialize-at-build-time="
//             +"ch.qos.logback.core.status.InfoStatus")
            buildArgs.add(
                "--initialize-at-run-time="
                        + "org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl"
            )
            buildArgs.add("--report-unsupported-elements-at-runtime")
            buildArgs.add("-H:ReflectionConfigurationFiles=${project.rootDir}/META-INF/native-image/reflect-config.json")
            buildArgs.add("-H:ResourceConfigurationFiles=${project.rootDir}/META-INF/native-image/resource-config.json")
            buildArgs.add("-H:JNIConfigurationFiles=${project.rootDir}/META-INF/native-image/jni-config.json")
            buildArgs.add("-H:DynamicProxyConfigurationFiles=${project.rootDir}/META-INF/native-image/proxy-config.json")
            buildArgs.add("--verbose")
            buildArgs.add("-H:TraceClassInitialization=ALL")
            // buildArgs.add("-H:+PrintAnalysisCallTree")
            // buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:+DeadlockWatchdogExitOnTimeout")
            buildArgs.add("-H:DeadlockWatchdogInterval=30000")
        }
    }
}

tasks.register<JavaExec>("runNative") {
    group = "application"
    description = "Run the native image"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.vicp.biggee.aot.vpn.expressvpn.Dialer.DialerApplication")
}

tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set("lucloner/dialer")
    builder = "paketobuildpacks/builder:tiny"

    if (isArm) {
        println("arm build")
        builder = "dashaun/builder-arm:20240403"
    } else {
        environment.put("BP_CYCLONE_DX_SYFT_URI", "http://10.8.0.6/Downloads/syft_0.84.0_linux_amd64.tar.gz")
        environment.put(
            "BP_JVM_DL_URL",
            "http://10.8.0.6/Downloads/bellsoft-liberica-vm-core-openjdk17.0.7+7-23.0.0+1-linux-amd64.tar.gz"
        )
        environment.put("BP_NATIVE_IMAGE", "true")
    }

    environment.keySet().get().forEach {
        println(it + " " + environment.getting(it).get())
    }
}

tasks.named<JavaExec>("bootRun") {
    jvmArgs("-agentlib:native-image-agent=config-output-dir=META-INF/native-image")
}


