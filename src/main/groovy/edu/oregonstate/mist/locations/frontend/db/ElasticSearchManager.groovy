package edu.oregonstate.mist.locations.frontend.db

import groovy.transform.TypeChecked
import io.dropwizard.lifecycle.Managed
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

@TypeChecked
class ElasticSearchManager implements Managed {
    TransportClient esClient
    URL esUrl

    /**
     * Constructs an Elasticsearch Client
     * @param esUrl     url with the ES server's host and port
     */
    ElasticSearchManager(String esUrl) {
        this.esUrl = new URL(esUrl)
        this.esClient = TransportClient.builder().build()
        this.esClient.addTransportAddress(
                new InetSocketTransportAddress(
                        new InetSocketAddress(this.esUrl.host, 9300)))
        // TODO: don't hardcode port
    }

    Client getClient() {
        esClient
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
        this.esClient.close()
    }
}
