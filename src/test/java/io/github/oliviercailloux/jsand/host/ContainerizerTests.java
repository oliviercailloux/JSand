package io.github.oliviercailloux.jsand.host;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.oliviercailloux.jaris.io.CloseablePathFactory;
import io.github.oliviercailloux.jaris.io.PathUtils;
import io.github.oliviercailloux.jsand.containerized.LoadOneClass;
import io.github.oliviercailloux.jsand.containerized.SendReady;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerizerTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(ContainerizerTests.class);

  @Test
  void testReady(@TempDir Path hostCodeDir) throws Exception {
    JavaSourcer sourcer = JavaSourcer.targetDir(hostCodeDir);
    CloseablePathFactory simple =
        PathUtils.fromUri(ContainerizerTests.class.getResource("../containerized/simple/").toURI());
    sourcer.copyCreateDir(simple, "pom.xml");
    Path root = Path.of("");
    sourcer.copyCreateDir(root, "src/main/java/io/github/oliviercailloux/jsand/common/JSand.java");
    sourcer.copyCreateDir(root,
    "src/main/java/io/github/oliviercailloux/jsand/common/ReadyService.java");
    sourcer.copyCreateDir(root,
    "src/main/java/io/github/oliviercailloux/jsand/common/RemoteLoggerService.java");
    sourcer.copyCreateDir(root, "src/main/java/io/github/oliviercailloux/jsand/containerized/HostRegistry.java");
    sourcer.copyCreateDir(root, "src/main/java/io/github/oliviercailloux/jsand/containerized/logback/RemoteClientAppender.java");
    String sendReadySource = "io/github/oliviercailloux/jsand/containerized/SendReady.java";
    JavaSourcer.copyCreateDirTo(Path.of("src/test/java/").resolve(sendReadySource), hostCodeDir.resolve("src/main/java/").resolve(sendReadySource));
    sourcer.copyLogbackConf();

    Containerizer containerizer =
        Containerizer.usingPaths(hostCodeDir, Path.of("/home/olivier/.m2/repository/"));
    containerizer.createNetworksIfNotExist();

    containerizer.compile();

    Registerer registerer = Registerer.create();
    registerer.setHostIp(containerizer.hostIp());
    registerer.createRegistry();
    ReadyWaiter readyWaiter = registerer.registerReadyWaiter();
    registerer.registerLogger();

    ExecutedContainer ran = containerizer.run(SendReady.class.getName());
    assertTrue(ran.err().length() < 10, ran.err());
    assertTrue(ran.out().contains("BUILD SUCCESS"));
    readyWaiter.latch().await();

    containerizer.removeContainersIfExist();
  }

  @Test
  void testLoadOneClass(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path hostCodeDir) throws Exception {
    JavaSourcer sourcer = JavaSourcer.targetDir(hostCodeDir);
    CloseablePathFactory simple =
        PathUtils.fromUri(ContainerizerTests.class.getResource("../containerized/simple/").toURI());
    sourcer.copyCreateDir(simple, "pom.xml");
    Path root = Path.of("");
    Path destBase = hostCodeDir.resolve("src/main/java/io/github/oliviercailloux/jsand/");
    Files.createDirectories(destBase);
    PathUtils.copyRecursively(root.resolve("src/main/java/io/github/oliviercailloux/jsand/common/"), destBase.resolve("common/"));
    PathUtils.copyRecursively(root.resolve("src/main/java/io/github/oliviercailloux/jsand/containerized/"), destBase.resolve("containerized/"));
    String loadOneClassSource = "io/github/oliviercailloux/jsand/containerized/LoadOneClass.java";
    JavaSourcer.copyCreateDirTo(Path.of("src/test/java/").resolve(loadOneClassSource), hostCodeDir.resolve("src/main/java/").resolve(loadOneClassSource));
    sourcer.copyLogbackConf();

    Containerizer containerizer =
        Containerizer.usingPaths(hostCodeDir, Path.of("/home/olivier/.m2/repository/"));
    containerizer.createNetworksIfNotExist();
    containerizer.removeContainersIfExist();

    containerizer.compile();

    Registerer registerer = Registerer.create();
    registerer.setHostIp(containerizer.hostIp());
    registerer.createRegistry();
    ReadyWaiter readyWaiter = registerer.registerReadyWaiter();
    registerer.registerLogger();
    registerer.registerClassSender(new ClassSenderImpl());

    ExecutedContainer ran = containerizer.run(LoadOneClass.class.getName());
    assertTrue(ran.err().length() < 10, ran.err());
    assertTrue(ran.out().contains("BUILD SUCCESS"));
    readyWaiter.latch().await();

    containerizer.removeContainersIfExist();
  }

  @Test
  void testLogFails(@TempDir Path hostCodeDir) throws Exception {
    JavaSourcer sourcer = JavaSourcer.targetDir(hostCodeDir);
    CloseablePathFactory simple =
        PathUtils.fromUri(ContainerizerTests.class.getResource("../containerized/simple/").toURI());
    sourcer.copyCreateDir(simple, "pom.xml");
    Path root = Path.of("");
    sourcer.copyCreateDir(root, "src/main/java/io/github/oliviercailloux/jsand/common/JSand.java");
    sourcer.copyCreateDir(root,
    "src/main/java/io/github/oliviercailloux/jsand/common/ReadyService.java");
    sourcer.copyCreateDir(root,
    "src/main/java/io/github/oliviercailloux/jsand/common/RemoteLoggerService.java");
    sourcer.copyCreateDir(root, "src/main/java/io/github/oliviercailloux/jsand/containerized/HostRegistry.java");
    sourcer.copyCreateDir(root, "src/main/java/io/github/oliviercailloux/jsand/containerized/logback/RemoteClientAppender.java");
    String sendReadySource = "io/github/oliviercailloux/jsand/containerized/SendReady.java";
    JavaSourcer.copyCreateDirTo(Path.of("src/test/java/").resolve(sendReadySource), hostCodeDir.resolve("src/main/java/").resolve(sendReadySource));
    sourcer.copyLogbackConf();

    Containerizer containerizer =
        Containerizer.usingPaths(hostCodeDir, Path.of("/home/olivier/.m2/repository/"));
    containerizer.createNetworksIfNotExist();

    containerizer.compile();

    Registerer registerer = Registerer.create();
    registerer.setHostIp(containerizer.hostIp());
    registerer.createRegistry();
    ReadyWaiter readyWaiter = registerer.registerReadyWaiter();

    ExecutedContainer ran = containerizer.run(SendReady.class.getName());

    assertTrue(ran.err().contains("Failed to initialize Configurator"));
    assertTrue(ran.err().contains("java.rmi.NotBoundException: Logger"));
    assertTrue(ran.out().contains("BUILD SUCCESS"));
    readyWaiter.latch().await();

    containerizer.removeContainersIfExist();
  }
}
