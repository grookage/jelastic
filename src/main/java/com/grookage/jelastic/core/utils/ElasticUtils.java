/*
 * Copyright 2019 Koushik R <rkoushik.14@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.grookage.jelastic.core.utils;

import com.grookage.jelastic.core.models.search.SearchResponse;
import com.grookage.jelastic.core.models.source.GetSourceRequest;
import com.grookage.jelastic.core.models.template.CreateTemplateRequest;
import lombok.experimental.UtilityClass;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author koushik
 */
@SuppressWarnings("rawtypes")
public interface ElasticUtils {

    static SortOrder getSortOrder(com.grookage.jelastic.core.models.query.sorter.SortOrder sortOrder) {
        if (sortOrder == com.grookage.jelastic.core.models.query.sorter.SortOrder.ASC) {
            return SortOrder.ASC;
        }
        return SortOrder.DESC;
    }

    static <T> List<T> getResponse(org.elasticsearch.action.search.SearchResponse response, Class<T> klass) {
        return Arrays.stream(response.getHits().getHits())
                .map(hit -> {
                    final Map<String, Object> result = hit.getSourceAsMap();
                    return MapperUtils.mapper().convertValue(result, klass);
                }).collect(Collectors.toList());
    }

    static <T> SearchResponse<T> getSearchResponse(org.elasticsearch.action.search.SearchResponse response, Class<T> klass) {
        return SearchResponse.<T>builder()
                .count(response.getHits().getTotalHits().value)
                .entities(Arrays.stream(response.getHits().getHits())
                        .map(hit -> {
                            final Map<String, Object> result = hit.getSourceAsMap();
                            return MapperUtils.mapper().convertValue(result, klass);
                        }).collect(Collectors.toList()))
                .build();
    }

    static <T> List<T> getResponse(MultiGetResponse multiGetItemResponses, Class<T> klass) {
        return Arrays.stream(multiGetItemResponses.getResponses())
                .map(hit -> {
                    final Map<String, Object> result = hit.getResponse().getSourceAsMap();
                    return MapperUtils.mapper().convertValue(result, klass);
                }).collect(Collectors.toList());
    }

    static GetRequest getRequest(GetSourceRequest getSourceRequest) {
        return new GetRequest(getSourceRequest.getIndexName()).id(getSourceRequest.getReferenceId());
    }

    static Map<String, Object> getSettings(CreateTemplateRequest createTemplateRequest) {
        final var indexProperties = createTemplateRequest.getIndexProperties();
        final var settings = new HashMap<String, Object>(Map.of(
                ElasticProperties.NO_OF_SHARDS, indexProperties.getNoOfShards(),
                ElasticProperties.NO_OF_REPLICAS, indexProperties.getNoOfReplicas(),
                ElasticProperties.INDEX_REQUEST_CACHE, indexProperties.isEnableRequestCache()
        ));
        if (!Objects.isNull(createTemplateRequest.getAnalysis())) {
            settings.put(ElasticProperties.ANALYSIS, createTemplateRequest.getAnalysis());
        }
        if (!Objects.isNull(createTemplateRequest.getIndexProperties().getNoOfRoutingShards())) {
            settings.put(ElasticProperties.NO_OF_ROUTING_SHARDS,
                    createTemplateRequest.getIndexProperties().getNoOfRoutingShards());
        }
        return settings;
    }

    @UtilityClass
    class ElasticProperties {
        public static final String NO_OF_SHARDS = "number_of_shards";
        public static final String NO_OF_REPLICAS = "number_of_replicas";
        public static final String INDEX_REQUEST_CACHE = "index.requests.cache.enable";
        public static final String ANALYSIS = "analysis";
        public static final String NO_OF_ROUTING_SHARDS = "number_of_routing_shards";
    }
}
