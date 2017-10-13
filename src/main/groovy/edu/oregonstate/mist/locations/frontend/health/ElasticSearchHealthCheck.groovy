package edu.oregonstate.mist.locations.frontend.health

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheck.Result
import groovy.transform.TypeChecked
import org.elasticsearch.client.Client
import org.elasticsearch.cluster.health.ClusterHealthStatus
import org.elasticsearch.indices.IndexClosedException

@TypeChecked
class ElasticSearchHealthCheck extends HealthCheck {
    private final Client esClient
    private final String esIndex
    private final String esIndexService

    ElasticSearchHealthCheck(Client esClient, Map<String,String> locationConfiguration) {
        this.esClient = esClient
        this.esIndex = locationConfiguration.get("esIndex")
        this.esIndexService = locationConfiguration.get("esIndexService")
    }

    /**
     * Verifies that ElasticSearch connection is successful, the health
     * of the cluster and that there are records in the cluster.
     *
     * @return result
     */
    @Override
    protected Result check() throws Exception {
        def req = esClient.admin().cluster().prepareHealth(esIndex, esIndexService)
        def resp = req.get()
        def status = resp.getStatus()

        //println(resp.toString())

        if (status == ClusterHealthStatus.RED) {
            return Result.unhealthy("cluster status: ${status.name()}")
        }

        // It is surprisingly tricky to check the open/closed status of an index
        // We can't use the _cat/indices/ endpoint because _cat seems to be limited to the REST API
        // We could use _cluster/state/metadata and see if metadata.indices.foo.state is set
        // But it's simpler just to try and get the index stats and catch the ClosedIndexException
        // We want to get the doc count anyway, so this saves a request

        // https://stackoverflow.com/questions/27780036/check-if-elasticsearch-index-is-open-or-closed

        def statsReq = esClient.admin().indices().prepareStats(esIndex, esIndexService)
        try {
            def statsResp = statsReq.get()

            for (index in [esIndex, esIndexService]) {
                def docCount = statsResp.getIndex(index).total.docs.count
                if (docCount <= 0) {
                    return Result.unhealthy("index ${index}: doc count = ${docCount}")
                }
            }
        } catch (IndexClosedException exc) {
            return Result.unhealthy("index ${exc.index}: closed")
        }

        Result.healthy()
    }
}
