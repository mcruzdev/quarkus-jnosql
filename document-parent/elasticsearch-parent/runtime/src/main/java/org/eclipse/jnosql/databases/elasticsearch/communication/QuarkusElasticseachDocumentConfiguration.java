package org.eclipse.jnosql.databases.elasticsearch.communication;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.document.DocumentConfiguration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

@Singleton
public class QuarkusElasticseachDocumentConfiguration implements DocumentConfiguration {

    @Inject
    protected ElasticsearchClient client;

    @Override
    public ElasticsearchDocumentManagerFactory apply(Settings settings) throws NullPointerException {
        return new ElasticsearchDocumentManagerFactory(this.client);
    }

}
