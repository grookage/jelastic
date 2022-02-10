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
package com.grookage.jelastic.core.repository;

import com.google.common.collect.Lists;
import com.grookage.jelastic.core.elastic.ElasticClient;
import com.grookage.jelastic.core.exception.InvalidQueryException;
import com.grookage.jelastic.core.exception.JelasticException;
import com.grookage.jelastic.core.exception.JsonMappingException;
import com.grookage.jelastic.core.managers.QueryManager;
import com.grookage.jelastic.core.models.mapping.CreateMappingRequest;
import com.grookage.jelastic.core.models.query.Query;
import com.grookage.jelastic.core.models.query.paged.PageWindow;
import com.grookage.jelastic.core.models.search.IdSearchRequest;
import com.grookage.jelastic.core.models.search.SearchRequest;
import com.grookage.jelastic.core.models.search.SearchResponse;
import com.grookage.jelastic.core.models.source.*;
import com.grookage.jelastic.core.models.template.CreateTemplateRequest;
import com.grookage.jelastic.core.utils.ElasticUtils;
import com.grookage.jelastic.core.utils.MapperUtils;
import com.grookage.jelastic.core.utils.ValidationUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import javax.inject.Singleton;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by koushikr
 */
@SuppressWarnings("unused")
@Slf4j
@Singleton
@AllArgsConstructor
@Getter
public class ElasticRepository implements Closeable {

    private final ElasticClient elasticClient;
    private final QueryManager queryManager;

    public IndexTemplateMetaData getTemplate(String templateName) {
        final var getRequest = new GetIndexTemplatesRequest(templateName);
        try {
            final var getIndexTemplatesResponse = elasticClient.getClient().
                    indices().getIndexTemplate(getRequest, RequestOptions.DEFAULT);
            return getIndexTemplatesResponse.getIndexTemplates().isEmpty() ?
                    null : getIndexTemplatesResponse.getIndexTemplates().get(0);
        } catch (IOException e) {
            throw new JelasticException("Unable to get Template", e);
        }
    }

    /**
     * Runs with the {@link ValidationUtil} when runWithValidator is true.
     * Works with the request validation of jaxrs!
     * @param request Any java object
     */
    public <T> void validate(T request){
        if(elasticClient.getJElasticConfiguration().isRunWithValidator()){
            ValidationUtil.validateRequest(request);
        }
    }

    public void createMapping(CreateMappingRequest mappingRequest) {
        validate(mappingRequest);
        final var request = new PutMappingRequest(mappingRequest.getIndexName())
                .source(mappingRequest.getMappingSource());
        try {
            elasticClient.getClient()
                    .indices()
                    .putMapping(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new JelasticException("Error Creating mapping", e);
        }

    }

    public void createTemplate(CreateTemplateRequest createTemplateRequest) {
        validate(createTemplateRequest);
        final var mapping = new PutIndexTemplateRequest(createTemplateRequest.getTemplateName())
                .patterns(
                        Lists.newArrayList(
                                "*" + createTemplateRequest.getIndexPattern() + "*"
                        )
                )
                .settings(ElasticUtils.getSettings(createTemplateRequest))
                .mapping(createTemplateRequest.getMappingSource()); // removed mapping type as that support is not provided anymore.
        try {
            elasticClient.getClient().indices().putTemplate(mapping, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new JelasticException("Could not put template", e);
        }
    }

    public void createIndex(String indexName) {
        if (!isExistsIndex(indexName)) {
            try {
                elasticClient.getClient()
                        .indices()
                        .create(new CreateIndexRequest(indexName),RequestOptions.DEFAULT);

                elasticClient.getClient()
                        .cluster()
                        .health(new ClusterHealthRequest(indexName)
                                    .waitForStatus(ClusterHealthStatus.GREEN),
                                RequestOptions.DEFAULT);
            } catch (IOException e) {
                throw new JelasticException("Create Index Failed", e);
            }
        }
    }

    /**
     * Method to retrieve document based on referenceId.
     *
     * @param getSourceRequest getSourceRequest
     * @param <T> class to be retrieved
     * @throws JsonMappingException when there is IOException, JsonParseException & JsonMappingException
     * @return Optional<T>
     */
    public <T> Optional<T> get(GetSourceRequest<T> getSourceRequest) {
        validate(getSourceRequest);
        GetResponse getResponse;
        try {
            getResponse = elasticClient.getClient()
                    .get(ElasticUtils.getRequest(getSourceRequest), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new JelasticException("Error Getting document",e);
        }
        try{
            return Optional.ofNullable(
                    getResponse.isExists() ?
                    MapperUtils.mapper().readValue(
                        getResponse.getSourceAsString(), getSourceRequest.getKlass()
                    ) : null
            );
        } catch (IOException io) {
            throw new JsonMappingException("Exception while mapping response to class ", io);
        }
    }

    /**
     * Method to persist entity into ElasticSearch.
     *
     * @param entitySaveRequest entitySaveRequest
     * @throws IndexNotFoundException when index is not created.
     */
    public void save(EntitySaveRequest entitySaveRequest) {
        validate(entitySaveRequest);
        var exists = isExistsIndex(entitySaveRequest.getIndexName());
        if (!exists) {
            throw new IndexNotFoundException("Index, " + entitySaveRequest.getIndexName() + " doesn't exist.");
        }

        final var request = new IndexRequest(entitySaveRequest.getIndexName())
                .id(entitySaveRequest.getReferenceId())
                .source(entitySaveRequest.getValue(), XContentType.JSON)
                .routing(entitySaveRequest.getRoutingKey())
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        try {
            elasticClient.getClient()
                    .index(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new JelasticException("Failed to save data", e);
        }
    }

    private boolean isExistsIndex(String indexName) {
        try {
            return elasticClient.getClient()
                    .indices()
                    .exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new JelasticException("Failed to check if Index exists", e);
        }
    }

    public void update(UpdateEntityRequest updateEntityRequest) {
        validate(updateEntityRequest);
        final var updateRequest = new UpdateRequest(updateEntityRequest.getIndexName(), updateEntityRequest.getReferenceId())
                .doc(updateEntityRequest.getValue(), XContentType.JSON)
                .routing(updateEntityRequest.getRoutingKey())
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        try {
            elasticClient.getClient()
                    .update(updateRequest,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new JelasticException("Error updating the request", e);
        }
    }

    public void updateField(UpdateFieldRequest updateFieldRequest) {
        validate(updateFieldRequest);
        final var updateRequest = new UpdateRequest(
                updateFieldRequest.getIndexName(),
                updateFieldRequest.getReferenceId()
        ).retryOnConflict(updateFieldRequest.getRetryCount())
                .routing(updateFieldRequest.getRoutingKey())
            .doc(
                MapperUtils.writeValueAsString(updateFieldRequest.getFieldValueMap()),
                XContentType.JSON
            );
        try {
            elasticClient.getClient().update(updateRequest,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new JelasticException("Error Updating Field.",e);
        }
    }

    public void reAlias(String newIndex, String aliasName) {
        GetAliasesResponse getAliasesResponse;
        try {
            getAliasesResponse = elasticClient.getClient().indices()
                    .getAlias(new GetAliasesRequest(aliasName), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new JelasticException("Error Getting alias", e);
        }
        final var aliases = getAliasesResponse.getAliases();
        final var request = new IndicesAliasesRequest();
        if (aliases.isEmpty()) {
            request.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD).alias(aliasName).index(newIndex));
        }

        final var oldIndex = aliases.keySet().iterator().next();
        if (oldIndex.equalsIgnoreCase(newIndex)) return;

        request.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD).alias(aliasName).index(newIndex));
        request.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE).alias(aliasName).index(oldIndex));
        try {
        elasticClient.getClient()
                .indices()
                .updateAliases(request, RequestOptions.DEFAULT);

            elasticClient.getClient()
                    .indices()
                    .delete(new DeleteIndexRequest(oldIndex), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new JelasticException("Error Performing request", e);
        }
    }

    /**
     * Search elasticSearch based on the search query passed.
     *
     * @param searchRequest searchRequest
     * @param <T> Response class Type
     * @throws InvalidQueryException when query is not built correctly.
     * @return SearchResponse<T> list of objects that meet searchCriteria
     */
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    public <T> SearchResponse<T> enumeratedSearch(SearchRequest<T> searchRequest) {
        validate(searchRequest);
        final var query = searchRequest.getQuery();
        final var queryBuilder = queryManager.getQueryBuilder(query);

        final var request = new org.elasticsearch.action.search.SearchRequest(searchRequest.getIndex());
        final var searchSourceBuilder = new SearchSourceBuilder().query(queryBuilder);

        if (!Objects.isNull(searchRequest.getRoutingKeys()) && !searchRequest.getRoutingKeys().isEmpty()) {
            request.routing(searchRequest.getRoutingKeys()
                            .toArray(new String[(searchRequest.getRoutingKeys().size())]));
        }
        if (!Objects.isNull(query.getSorters()) && !query.getSorters().isEmpty()) {
            queryManager.getSortBuilders(query).forEach(searchSourceBuilder::sort);
        }
        searchSourceBuilder
                .from(query.getPageWindow().getPageNumber() * query.getPageWindow().getPageSize())
                .size(query.getPageWindow().getPageSize());

        request.source(searchSourceBuilder);
        org.elasticsearch.action.search.SearchResponse searchResponse;
        try {
            searchResponse = elasticClient.getClient().search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new JelasticException("Error Searching the results", e);
        }
        return ElasticUtils.getSearchResponse(searchResponse, searchRequest.getKlass());
    }


    public <T> SearchResponse<T> search(String index, QueryBuilder queryBuilder,
                                        PageWindow pageWindow, Class<T> klass) {

        org.elasticsearch.action.search.SearchRequest request = new org.elasticsearch.action.search.SearchRequest(index);
        final var searchSourceBuilder = new SearchSourceBuilder().query(queryBuilder);
        searchSourceBuilder
                .from(pageWindow.getPageNumber() * pageWindow.getPageSize())
                .size(pageWindow.getPageSize());
        request.source(searchSourceBuilder);
        org.elasticsearch.action.search.SearchResponse searchResponse;
        try {
            searchResponse = elasticClient.getClient().search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new JelasticException("Error Searching the results", e);
        }
        return ElasticUtils.getSearchResponse(searchResponse, klass);
    }

    public <T> List<T> searchByIds(IdSearchRequest<T> idSearchRequest) {
        validate(idSearchRequest);
        final var request = new MultiGetRequest();
        idSearchRequest.getIds().forEach(id -> request.add(new MultiGetRequest.Item(idSearchRequest.getIndex(), id)));
        MultiGetResponse multiGetItemResponses;
        try {
            multiGetItemResponses = elasticClient.getClient().mget(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new JelasticException("Error performing MGET operation", e);
        }

        return ElasticUtils.getResponse(multiGetItemResponses, idSearchRequest.getKlass());
    }

    /**
     * Load data of entire index based on batchSize using ES scroll API
     * During first request to ES we send scroll ttl and the response return result along with scroll Id. Then for all
     * subsequent we send this scroll id as request param. ES knows how much data is returned in previous calls. We fetch
     * the data in batches using batchSize which should be < 10k for better performance
     *
     * @param <T> Response class Type
     * @param index Index to be loaded
     * @param query jelastic query object
     * @param batchSize Will tell ElasticClient the size of each fetch, should be <= 10000 for better performance
     * @return List<T> list of all objects in that index
     */
    public <T> List<T> loadAll(String index, Query query, int batchSize, Class<T> klass) {
        final var maxResultSize = elasticClient.getJElasticConfiguration().getMaxResultSize();

        if(batchSize > maxResultSize){
            log.error("Result size exceeds configured limit of {}, Please try changing it.", maxResultSize);
            throw new JelasticException(String.format("Result size exceeds configured limit : %d", maxResultSize));
        }

        final var scroll = new Scroll(TimeValue.timeValueMinutes(1L));
        final var queryBuilder = queryManager.getQueryBuilder(query);
        final var request = new org.elasticsearch.action.search.SearchRequest(index);
        final var searchSourceBuilder = new SearchSourceBuilder().query(queryBuilder);
        searchSourceBuilder.size(batchSize);

        request.source(searchSourceBuilder)
                .scroll(scroll);

        try {
            var searchResponse = elasticClient.getClient().search(request, RequestOptions.DEFAULT);
            var batchedResult = ElasticUtils.getResponse(searchResponse, klass);
            final var scrollId = searchResponse.getScrollId();
            final var totalResult = new ArrayList<>(batchedResult);
            while (batchedResult.size() == batchSize) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(scroll);
                searchResponse = elasticClient
                        .getClient()
                        .scroll(scrollRequest, RequestOptions.DEFAULT);
                batchedResult = ElasticUtils.getResponse(searchResponse, klass);
                totalResult.addAll(batchedResult);
            }
            return totalResult;
        } catch (IOException e){
            throw new JelasticException("Error processing request!!", e);
        }

    }

  /**
   * Method to delete a document based on reference id
   * @param deleteEntityRequest deleteEntityRequest
   */
  public void delete(DeleteEntityRequest deleteEntityRequest) {
        validate(deleteEntityRequest);
        final var request = new DeleteRequest(deleteEntityRequest.getIndexName(),deleteEntityRequest.getReferenceId());
        request.routing(deleteEntityRequest.getRoutingKey());
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

      try {
          elasticClient.getClient().delete(request,RequestOptions.DEFAULT);
      } catch (IOException e) {
          throw new JelasticException("Error Deleting entity", e);
      }
  }

    @Override
    public void close() {
        elasticClient.stop();
    }
}
