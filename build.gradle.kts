import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.util.*

plugins {
    java
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.hibernate.orm") version "6.5.2.Final"
    id("org.graalvm.buildtools.native") version "0.10.2"
}

group = "net.vicp.biggee.aot.vpn.expressvpn"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        // vendor.set(JvmVendorSpec.GRAAL_VM)
        // implementation.set(JvmImplementation.J9)
        // vendor.set(JvmVendorSpec.matching("GraalVM"))
        // implementation.set(JvmImplementation.VENDOR_SPECIFIC)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    mavenCentral()
}

extra["springBootAdminVersion"] = "3.3.2"
extra["springCloudVersion"] = "2023.0.3"
extra["springShellVersion"] = "3.3.1"

dependencies {
    // implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//	implementation("org.springframework.boot:spring-boot-starter-data-redis")
//	implementation("org.springframework.boot:spring-boot-starter-jdbc")
    // implementation("org.springframework.boot:spring-boot-starter-mail")
    // implementation("org.springframework.boot:spring-boot-starter-web")
    // implementation("de.codecentric:spring-boot-admin-starter-server")
    // implementation("org.springframework.cloud:spring-cloud-config-server")
//	implementation("org.springframework.cloud:spring-cloud-starter-zookeeper-config")
//	implementation("org.springframework.cloud:spring-cloud-starter-zookeeper-discovery")
    // implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    // implementation("org.springframework.data:spring-data-rest-hal-explorer")
//	implementation("org.springframework.session:spring-session-data-redis")
//	implementation("org.springframework.session:spring-session-jdbc")
    // implementation("org.springframework.shell:spring-shell-starter")
    compileOnly("org.projectlombok:lombok")
//	developmentOnly("org.springframework.boot:spring-boot-devtools")
//	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("com.h2database:h2")
//	runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.shell:spring-shell-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // https://mvnrepository.com/artifact/org.jetbrains.pty4j/pty4j
    implementation("org.jetbrains.pty4j:pty4j:0.12.34")
//    implementation("org.springframework.experimental:spring-native")
//    implementation("org.springframework.experimental:spring-aot")
//    implementation("org.graalvm.nativeimage:svm")
    implementation("org.springframework.boot:spring-boot-starter-webflux") 
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.shell:spring-shell-dependencies:${property("springShellVersion")}")
        mavenBom("de.codecentric:spring-boot-admin-dependencies:${property("springBootAdminVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

hibernate {
    enhancement {
        enableAssociationManagement = true
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
            imageName.set("lucloner/dialer") // 生成的 native image 名称
            buildArgs.add("-H:+UnlockExperimentalVMOptions")
            buildArgs.add("-H:UseFatJar")
            buildArgs.add("-H:+UseClassDataSharing")
            // buildArgs.add("--no-fallback")
            // buildArgs.add("-H:-DebugInfo")
            buildArgs.add("-Djava.util.logging.ConsoleHandler.level=FINE")
            // buildArgs.add("--trace-class-initialization=org.apache.tomcat.util.net.openssl.OpenSSLEngine")
            // buildArgs.add("--initialize-at-build-time="
            // +"org.springframework.core,org.springframework.context,org.springframework.beans,org.springframework.boot,org.springframework.util,org.springframework.web,org.springframework.http,org.springframework.aop,org.springframework.jdbc,org.springframework.orm,org.springframework.transaction,org.springframework.data,org.springframework.cache,org.springframework.security,com.fasterxml.jackson,com.zaxxer.hikari,org.hibernate,javax.servlet,org.apache.tomcat,org.apache.catalina,org.apache.coyote,org.apache.jasper,org.apache.commons.logging,org.slf4j,org.aspectj,org.jasypt,org.thymeleaf,org.h2,"
            // +"org.hibernate.Hibernate,org.hibernate.Session,"
            // +"org.apache.tomcat.util.net.openssl.OpenSSLEngine,"
            // +"org.apache.tomcat.util.net.openssl.OpenSSLContext,"
            // +"ch.qos.logback.core.status.InfoStatus")
            // buildArgs.add("--initialize-at-run-time="
            // +"jakarta.persistence.Entity,"
            // +"org.apache.tomcat.util.net.openssl.OpenSSLEngine,"
            // +"org.apache.tomcat.util.net.openssl.OpenSSLContext,"
            // +"org.springframework.core.io.VfsUtils")
            // buildArgs.add("--initialize-at-run-time=org.apache.catalina.*")   
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

val isArm = System.getProperty("os.arch").lowercase(Locale.getDefault()).contains("aarch64")
tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set("lucloner/dialer")
    builder = "paketobuildpacks/builder:tiny"
    environment.put("BP_CYCLONE_DX_SYFT_URI", "http://10.8.0.6/Downloads/syft_0.84.0_linux_amd64.tar.gz")
    environment.put("BP_JVM_DL_URL","http://10.8.0.6/Downloads/bellsoft-liberica-vm-core-openjdk17.0.7+7-23.0.0+1-linux-amd64.tar.gz")
    environment.put("BP_NATIVE_IMAGE","true")

    if(isArm){
        println("arm build")
        builder = "dashaun/builder-arm:20240403"
        environment.put("BP_CYCLONE_DX_SYFT_URI", "http://10.8.0.6/Downloads/syft_0.105.0_linux_arm64.tar.gz")
        environment.put("BP_JVM_DL_URL","http://10.8.0.6/Downloads/bellsoft-jre17.0.10+13-linux-aarch64.tar.gz")
    }

    environment.keySet().get().forEach {
        println(it+" "+environment.getting(it).get())
    }
}

tasks.named<JavaExec>("bootRun") {
    jvmArgs("-agentlib:native-image-agent=config-output-dir=META-INF/native-image")
}




