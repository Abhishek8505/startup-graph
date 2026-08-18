package com.startupgraph.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Config.TrustStrategy;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

    @Bean(destroyMethod = "close")
    public Driver neo4jDriver(DbProperties props) {
        if (!props.configured()) {
            return null;
        }
        Config config = Config.builder()
                .withMaxConnectionPoolSize(10)
                .withConnectionAcquisitionTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .withTrustStrategy(TrustStrategy.TRUST_ALL_CERTIFICATES)
                .build();
        return GraphDatabase.driver(props.uri(), AuthTokens.basic(props.user(), props.password()), config);
    }
}
