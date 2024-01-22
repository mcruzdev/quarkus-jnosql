package io.quarkiverse.jnosql.document.deployment;

import java.util.*;

import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;

public class DocumentDevServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDevServices.class);

    @BuildStep(onlyIfNot = { IsNormal.class }, onlyIf = { GlobalDevServicesConfig.Enabled.class })
    public void build(BuildProducer<DevServicesResultBuildItem> devServicesProducer) {

        Set<String> allowedDatabases = Set.of("arangodb", "couchdb");
        String database = ConfigProvider.getConfig()
                .getOptionalValue("jnosql.document.database", String.class)
                .orElse("");
        if (!allowedDatabases.contains(database)) {
            LOGGER.warn("This extensions does provide support only for [arangodb, couchdb] document databases");
            return;
        }

        LOGGER.info("jnosql.document.database === {}", database);

        if (database.equals("couchdb")) {
            DockerConfiguration dockerConfig = new DockerConfiguration(
                    ConfigProvider.getConfig().getValue("jnosql.couchdb.port", Integer.class),
                    ConfigProvider.getConfig().getValue("jnosql.couchdb.password", String.class),
                    ConfigProvider.getConfig().getValue("jnosql.couchdb.username", String.class));

            CouchDBContainer couchDBContainer = new CouchDBContainer()
                    .withEnv(
                            Map.of("COUCHDB_PASSWORD", dockerConfig.password,
                                    "COUCHDB_USER", dockerConfig.username))
                    .withExposedPorts(CouchDBContainer.COUCHDB_DEFAULT_PORT);

            couchDBContainer.setPortBindings(
                    List.of(String.format("0.0.0.0:%d:%d", CouchDBContainer.COUCHDB_DEFAULT_PORT, dockerConfig.port)));

            couchDBContainer.start();

            LOGGER.info(couchDBContainer.getLogs());

            devServicesProducer.produce(new DevServicesResultBuildItem.RunningDevService(
                    "jnosql-document",
                    couchDBContainer.getContainerId(),
                    couchDBContainer::close,
                    Map.of()).toBuildItem());
        } else {
            String host = ConfigProvider.getConfig().getValue("jnosql.arangodb.host", String.class);

            String[] hostPort = host.split(":");
            int PORT = 1;
            String portAsSting = hostPort[PORT];

            DockerConfiguration dockerConfig = new DockerConfiguration(
                    Integer.valueOf(portAsSting),
                    ConfigProvider.getConfig()
                            .getValue("jnosql.arangodb.password", String.class),
                    null);

            ArangoDBContainer arangoDBContainer = new ArangoDBContainer()
                    .withExposedPorts(ArangoDBContainer.ARANGO_DB_DEFAULT_PORT)
                    .withEnv("ARANGO_ROOT_PASSWORD", dockerConfig.password);

            arangoDBContainer.setPortBindings(
                    List.of(String.format("0.0.0.0:%d:%d", ArangoDBContainer.ARANGO_DB_DEFAULT_PORT, dockerConfig.port)));

            arangoDBContainer.start();

            LOGGER.info(arangoDBContainer.getLogs());

            devServicesProducer.produce(new DevServicesResultBuildItem.RunningDevService(
                    "jnosql-document",
                    arangoDBContainer.getContainerId(),
                    arangoDBContainer::close,
                    Map.of()).toBuildItem());
        }
    }

    static class ArangoDBContainer extends GenericContainer<ArangoDBContainer> {
        public static final Integer ARANGO_DB_DEFAULT_PORT = 8529;

        public ArangoDBContainer() {
            super(DockerImageName.parse("arangodb:latest"));
        }
    }

    static class CouchDBContainer extends GenericContainer<CouchDBContainer> {
        public static final Integer COUCHDB_DEFAULT_PORT = 5984;

        public CouchDBContainer() {
            super(DockerImageName.parse("couchdb:latest"));
        }
    }

    public static class DockerConfiguration {
        public Integer port;
        public String password;
        public String username;

        public DockerConfiguration(Integer port, String password, String username) {
            this.port = port;
            this.password = password;
            this.username = username;
        }
    }
}
