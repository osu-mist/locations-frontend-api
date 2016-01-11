package edu.oregonstate.mist.locations.frontend.health

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheck.Result
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class ElasticSearchHealthCheck extends HealthCheck {
    private final Map<String, String> locationConfiguration

    ElasticSearchHealthCheck(Map<String, String> locationConfiguration) {
        this.locationConfiguration = locationConfiguration
    }

    /**
     * Verifies that ElasticSearch connection is successful, the health
     * of the cluster and that there are records in the cluster.
     *
     * @return result
     */
    @Override
    protected Result check() {
        try {
            String esUrl = locationConfiguration.get("esUrl")
            String esIndex = locationConfiguration.get("esIndex")
            String clusterHealth = new URL("${esUrl}/_cluster/health/${esIndex}").text

            ObjectMapper mapper = new ObjectMapper()
            JsonNode rootNode = mapper.readTree(clusterHealth)
            String status = rootNode.path("status").textValue()

            if (status != "red") {
                String indexHealth = new URL("${esUrl}/_cat/indices/${esIndex}").text

                def indexPieces = indexHealth?.split(" ")
                String indexStatus = indexPieces[1]
                String indexDocCount = indexPieces[5]
                if (indexHealth && indexStatus == "open" && indexDocCount) {
                    return Result.healthy()
                }

                Result.unhealthy("index status: ${indexStatus}, docCount: ${indexDocCount}")
            } else {
                Result.unhealthy("cluster status: ${status}")
            }
        } catch(Exception e) {
            Result.unhealthy(e.message)
        }
    }
}
