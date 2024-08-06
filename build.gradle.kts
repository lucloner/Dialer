import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

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
        languageVersion = JavaLanguageVersion.of(17)
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
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//	implementation("org.springframework.boot:spring-boot-starter-data-redis")
//	implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("de.codecentric:spring-boot-admin-starter-server")
    implementation("org.springframework.cloud:spring-cloud-config-server")
//	implementation("org.springframework.cloud:spring-cloud-starter-zookeeper-config")
//	implementation("org.springframework.cloud:spring-cloud-starter-zookeeper-discovery")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation("org.springframework.data:spring-data-rest-hal-explorer")
//	implementation("org.springframework.session:spring-session-data-redis")
//	implementation("org.springframework.session:spring-session-jdbc")
    implementation("org.springframework.shell:spring-shell-starter")
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

graalvmNative {
    toolchainDetection.set(true)
    binaries {
//        named("main") {
//            imageName="dialer"
//            buildArgs.addAll(
//                "--initialize-at-build-time=org.bouncycastle.jce.provider.BouncyCastleProvider",
//                "--initialize-at-build-time=org.bouncycastle.jce.provider.BouncyCastleProvider\$JcaCryptoService",
//                "--initialize-at-build-time=org.bouncycastle.jce.provider.BouncyCastleProviderConfiguration",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.xmss.XMSSMTKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.xmss.XMSSKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.sphincsplus.SPHINCSPlusKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.sphincs.Sphincs256KeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.picnic.PicnicKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.ntru.NTRUKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.newhope.NHKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.lms.LMSKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.kyber.KyberKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.hqc.HQCKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.falcon.FalconKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.dilithium.DilithiumKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.cmce.CMCEKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.pqc.jcajce.provider.bike.BIKEKeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.gost.KeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.elgamal.KeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.edec.KeyFactorySpi\$X448",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.edec.KeyFactorySpi\$X25519",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.edec.KeyFactorySpi\$Ed448",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.edec.KeyFactorySpi\$Ed25519",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.ecgost12.KeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.ecgost.KeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi\$ECMQV",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi\$EC",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.dstu.KeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.dsa.KeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.dh.KeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.compositesignatures.KeyFactorySpi",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.EXTERNAL\$ExternalKeyInfoConverter",
//                "--initialize-at-build-time=org.bouncycastle.jcajce.provider.asymmetric.COMPOSITE\$CompositeKeyInfoConverter",
//                "--initialize-at-build-time=org.bouncycastle.asn1.x9.X962Parameters",
//                "--initialize-at-build-time=org.bouncycastle.asn1.x509.AlgorithmIdentifier",
//                "--initialize-at-build-time=org.bouncycastle.asn1.ASN1ObjectIdentifier",
//                "--initialize-at-build-time=org.bouncycastle.asn1.ASN1ObjectIdentifier\$OidHandle",
//                "--initialize-at-build-time=org.bouncycastle.asn1.ASN1ObjectIdentifier\$1",
//                "--initialize-at-build-time=org.bouncycastle.crypto.prng.VMPCRandomGenerator",
//                "--initialize-at-build-time=org.apache.sshd.sftp.client.fs.SftpFileSystemProvider",
//                "--initialize-at-build-time=org.apache.sshd.sftp.client.fs.SftpFileSystemClientSessionInitializer\$1",
//                "--initialize-at-build-time=org.apache.sshd.sftp.client.SftpVersionSelector\$NamedVersionSelector",
//                "--initialize-at-build-time=org.apache.sshd.sftp.client.SftpVersionSelector",
//                "--initialize-at-build-time=org.apache.sshd.sftp.client.SftpErrorDataHandler",
//                "--initialize-at-build-time=org.apache.sshd.common.file.root.RootedFileSystemProvider",
//                "--initialize-at-build-time=org.apache.sshd.common.session.helpers.SessionTimeoutListener",
//                "--initialize-at-build-time=org.apache.sshd.common.util.closeable.AbstractCloseable\$State",
//                "--initialize-at-build-time=org.apache.sshd.common.util.threads.SshdThreadFactory",
//                "--initialize-at-build-time=org.apache.sshd.common.util.threads.NoCloseExecutor",
//                "--initialize-at-build-time=org.apache.sshd.common.util.threads.SshThreadPoolExecutor",
//                "--initialize-at-build-time=org.apache.sshd.common.util.threads.SshThreadPoolExecutor\$DelegateCloseable",
//                "--initialize-at-build-time=org.apache.sshd.common.util.security.bouncycastle.BouncyCastleRandomFactory",
//                "--initialize-at-build-time=org.apache.sshd.common.util.security.bouncycastle.BouncyCastleRandom",
//                "--initialize-at-build-time=org.apache.sshd.common.util.buffer.keys.BufferPublicKeyParser\$2",
//                "--initialize-at-build-time=org.apache.sshd.common.util.buffer.keys.BufferPublicKeyParser\$1",
//                "--initialize-at-build-time=org.apache.sshd.common.util.buffer.keys.RSABufferPublicKeyParser",
//                "--initialize-at-build-time=org.apache.sshd.common.util.buffer.keys.ECBufferPublicKeyParser",
//                "--initialize-at-build-time=org.apache.sshd.common.util.buffer.keys.DSSBufferPublicKeyParser",
//                "--initialize-at-build-time=org.apache.sshd.common.util.buffer.keys.SkECBufferPublicKeyParser",
//                "--initialize-at-build-time=org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory",
//                "--initialize-at-build-time=org.apache.sshd.common.io.nio2.Nio2ServiceFactory",
//                "--initialize-at-build-time=org.apache.sshd.common.io.nio2.Nio2Connector",
//                "--initialize-at-build-time=org.apache.sshd.common.io.DefaultIoServiceFactoryFactory",
//                "--initialize-at-build-time=org.apache.sshd.common.future.DefaultCloseFuture",
//                "--initialize-at-build-time=org.apache.sshd.common.config.keys.FilePasswordProvider",
//                "--initialize-at-build-time=org.apache.sshd.common.config.keys.FilePasswordProviderHolder",
//                "--initialize-at-build-time=org.apache.sshd.common.SyspropsMapWrapper\$1",
//                "--initialize-at-build-time=org.apache.sshd.common.PropertyResolver\$1",
//                "--initialize-at-build-time=org.apache.sshd.common.AttributeRepository\$AttributeKey",
//                "--initialize-at-build-time=org.apache.sshd.common.Factory,java.util.function.Supplier",
//                "--initialize-at-build-time=org.apache.sshd.common.mac.BuiltinMacs\$3",
//                "--initialize-at-build-time=org.apache.sshd.common.mac.BuiltinMacs\$2",
//                "--initialize-at-build-time=org.apache.sshd.common.mac.BuiltinMacs\$1",
//                "--initialize-at-build-time=org.apache.sshd.common.cipher.BuiltinCiphers\$6",
//                "--initialize-at-build-time=org.apache.sshd.common.cipher.BuiltinCiphers\$5",
//                "--initialize-at-build-time=org.apache.sshd.common.cipher.BuiltinCiphers\$4",
//                "--initialize-at-build-time=org.apache.sshd.common.cipher.BuiltinCiphers\$3",
//                "--initialize-at-build-time=org.apache.sshd.common.cipher.BuiltinCiphers\$2",
//                "--initialize-at-build-time=org.apache.sshd.common.cipher.BuiltinCiphers\$1",
//                "--initialize-at-build-time=org.apache.sshd.common.compression.BuiltinCompressions\$1",
//                "--initialize-at-build-time=org.apache.sshd.common.compression.BuiltinCompressions\$2",
//                "--initialize-at-build-time=org.apache.sshd.common.compression.BuiltinCompressions\$3",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$18",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$17",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$16",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$15",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$14",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$13",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$12",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$11",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$10",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$9",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$8",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$7",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$6",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$5",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$4",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$3",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$2",
//                "--initialize-at-build-time=org.apache.sshd.common.signature.BuiltinSignatures\$1",
//                "--initialize-at-build-time=org.apache.sshd.common.random.SingletonRandomFactory",
//                "--initialize-at-build-time=org.apache.sshd.common.session.helpers.DefaultUnknownChannelReferenceHandler",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.extension.DefaultClientKexExtensionHandler",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$15",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$14",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$13",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$12",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$11",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$10",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$9",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$8",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$7",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$6",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$5",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$4",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$3",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$2",
//                "--initialize-at-build-time=org.apache.sshd.common.kex.BuiltinDHFactories\$1",
//                "--initialize-at-build-time=org.apache.sshd.common.forward.DefaultForwarderFactory\$1",
//                "--initialize-at-build-time=org.apache.sshd.common.forward.PortForwardingEventListener,org.apache.sshd.common.util.SshdEventListener,java.util.EventListener",
//                "--initialize-at-build-time=org.apache.sshd.common.forward.PortForwardingEventListener\$1",
//                "--initialize-at-build-time=org.apache.sshd.common.Property\$IntegerProperty",
//                "--initialize-at-build-time=org.apache.sshd.common.Property\$BooleanProperty",
//                "--initialize-at-build-time=org.apache.sshd.common.channel.ChannelListener,org.apache.sshd.common.util.SshdEventListener,java.util.EventListener",
//                "--initialize-at-build-time=org.apache.sshd.client.kex.DHGClient\$1",
//                "--initialize-at-build-time=org.apache.sshd.client.kex.DHGEXClient\$1",
//                "--initialize-at-build-time=org.apache.sshd.client.config.keys.DefaultClientIdentitiesWatcher",
//                "--initialize-at-build-time=org.apache.sshd.client.config.keys.ClientIdentityFileWatcher",
//                "--initialize-at-build-time=org.apache.sshd.client.config.keys.ClientIdentityLoaderHolder",
//                "--initialize-at-build-time=org.apache.sshd.client.config.keys.ClientIdentityLoader\$1",
//                "--initialize-at-build-time=org.apache.sshd.client.config.keys.ClientIdentityLoaderHolder",
//                "--initialize-at-build-time=org.apache.sshd.client.config.hosts.DefaultConfigFileHostEntryResolver",
//                "--initialize-at-build-time=org.apache.sshd.client.config.hosts.HostConfigEntryResolver\$1",
//                "--initialize-at-build-time=org.apache.sshd.client.auth.AuthenticationIdentitiesProvider\$1",
//                "--initialize-at-build-time=org.apache.sshd.client.auth.pubkey.UserAuthPublicKeyFactory\$1",
//                "--initialize-at-build-time=org.apache.sshd.client.auth.keyboard.UserAuthKeyboardInteractiveFactory",
//                "--initialize-at-build-time=org.apache.sshd.client.auth.password.UserAuthPasswordFactory",
//                "--initialize-at-build-time=org.apache.sshd.client.session.SessionFactory",
//                "--initialize-at-build-time=org.apache.sshd.client.session.ClientUserAuthServiceFactory",
//                "--initialize-at-build-time=org.apache.sshd.client.session.ClientConnectionServiceFactory\$1",
//                "--initialize-at-build-time=org.apache.sshd.client.SshClient",
//                "--initialize-at-build-time=org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier",
//                "--initialize-at-build-time=org.apache.sshd.client.global.OpenSshHostKeysHandler",
//                "--initialize-at-build-time=org.apache.sshd.server.forward.RejectAllForwardingFilter",
//                "--initialize-at-build-time=org.apache.sshd.server.forward.ForwardedTcpipFactory",
//                "--initialize-at-build-time=org.apache.sshd.server.forward.TcpForwardingFilter\$Type",
//                "--initialize-at-build-time=org.apache.sshd.sftp.client.SftpVersionSelector",
//                "--initialize-at-build-time=net.i2p.crypto.eddsa.EdDSASecurityProvider",
//                "--initialize-at-build-time=ch.qos.logback.core.status.InfoStatus",
//                "--initialize-at-build-time=ch.qos.logback.core.BasicStatusManager",
//                "--initialize-at-build-time=ch.qos.logback.core.spi.LogbackLock",
//                "--initialize-at-build-time=ch.qos.logback.core.spi.AppenderAttachableImpl",
//                "--initialize-at-build-time=ch.qos.logback.core.spi.FilterAttachableImpl",
//                "--initialize-at-build-time=ch.qos.logback.core.joran.spi.ConsoleTarget",
//                "--initialize-at-build-time=ch.qos.logback.core.joran.spi.ConsoleTarget\$1",
//                "--initialize-at-build-time=ch.qos.logback.core.helpers.CyclicBuffer",
//                "--initialize-at-build-time=ch.qos.logback.core.encoder.LayoutWrappingEncoder",
//                "--initialize-at-build-time=ch.qos.logback.core.util.COWArrayList",
//                "--initialize-at-build-time=ch.qos.logback.core.ConsoleAppender",
//                "--initialize-at-build-time=ch.qos.logback.classic.util.LogbackMDCAdapter",
//                "--initialize-at-build-time=ch.qos.logback.classic.util.ContextInitializer",
//                "--initialize-at-build-time=ch.qos.logback.classic.Logger",
//                "--initialize-at-build-time=ch.qos.logback.classic.LoggerContext",
//                "--initialize-at-build-time=ch.qos.logback.classic.spi.TurboFilterList",
//                "--initialize-at-build-time=ch.qos.logback.classic.spi.LoggerContextVO",
//                "--initialize-at-build-time=ch.qos.logback.classic.Level",
//                "--initialize-at-build-time=ch.qos.logback.classic.BasicConfigurator"
//            )
//        }
    }
}

tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set("lucloner/dialer")
    builder.set("paketobuildpacks/builder-jammy-tiny")
    environment.put("BP_NATIVE_IMAGE", "true")
}

