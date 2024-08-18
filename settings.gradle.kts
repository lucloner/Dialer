rootProject.name = "Dialer"
gradle.beforeProject {
    val javaHome = "/raid/graalvm-jdk-21.0.3+7.1"
    val graalVmHome = "/raid/graalvm-jdk-21.0.3+7.1"

    System.setProperty("java.home", javaHome)
    System.setProperty("GRAALVM_HOME", graalVmHome)

    System.setProperty("org.gradle.java.home", javaHome)
}

gradle.afterProject {
    println("Using Java from: ${System.getProperty("java.home")}")
    println("Using GraalVM from: ${System.getProperty("GRAALVM_HOME")}")
}