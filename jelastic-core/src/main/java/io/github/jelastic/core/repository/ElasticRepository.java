package io.github.jelastic.core.repository;

import com.google.common.collect.Lists;
import io.github.jelastic.models.source.*;
import io.github.jelastic.core.elastic.ElasticClient;
import io.github.jelastic.core.managers.QueryManager;
import io.github.jelastic.core.utils.ElasticUtils;
import io.github.jelastic.core.utils.SerDe;
import io.github.jelastic.models.query.Query;
import io.github.jelastic.models.query.paged.PageWindow;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequest;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilders;

import javax.inject.Singleton;
import java.io.Closeable;
import java.util.*;

/**
 * Created by koushikr
 */
@Slf4j
@Singleton
@AllArgsConstructor
public class ElasticRepository implements Closeable{

  private final ElasticClient elasticClient;
  private final QueryManager queryManager;

  public IndexTemplateMetaData getTemplate(String templateName) {
    GetIndexTemplatesRequest getRequest = new GetIndexTemplatesRequest().names(templateName);
    val getIndexTemplatesResponse = elasticClient.getClient().admin()
            .indices().getTemplates(getRequest).actionGet();
    return getIndexTemplatesResponse.getIndexTemplates().isEmpty() ?
            null : getIndexTemplatesResponse.getIndexTemplates().get(0);
  }

  public void createMapping(CreateMappingRequest mappingRequest){
      elasticClient.getClient()
              .admin()
              .indices()
              .preparePutMapping(mappingRequest.getIndexName())
              .setType(mappingRequest.getMappingType())
              .setSource(mappingRequest.getMappingSource())
              .execute()
              .actionGet();
  }

  public void createTemplate(CreateTemplateRequest createTemplateRequest) {
    val mapping = new PutIndexTemplateRequest()
        .name(createTemplateRequest.getTemplateName())
        .patterns(
                Lists.newArrayList(
                        "*" + createTemplateRequest.getIndexPattern() + "*"
                )
        )
        .settings(ElasticUtils.getSettings(createTemplateRequest))
        .mapping(
                createTemplateRequest.getMappingType(),
                createTemplateRequest.getMappingSource()
        );
    elasticClient.getClient().admin().indices().putTemplate(mapping).actionGet();
  }

  public void createIndex(String indexName) throws Exception {
    if (!elasticClient.getClient().admin().indices().prepareExists(indexName).execute().get()
        .isExists()) {
      elasticClient.getClient()
              .admin()
              .indices()
              .prepareCreate(indexName)
              .execute()
              .actionGet();

      elasticClient.getClient()
              .admin()
              .cluster()
              .prepareHealth(indexName)
              .setWaitForGreenStatus()
              .execute()
              .actionGet();
    }
  }

  public <T> Optional<T> get(GetSourceRequest<T> getSourceRequest) throws Exception {
    GetResponse getResponse = elasticClient.getClient()
        .get(ElasticUtils.getRequest(getSourceRequest)).get();
    T entity = getResponse.isExists() ?
            SerDe.mapper().readValue(
                    getResponse.getSourceAsString(), getSourceRequest.getKlass()
            ) :
            null;
    return Optional.ofNullable(entity);
  }

  public void save(EntitySaveRequest entitySaveRequest) throws Exception {
    if (!elasticClient.getClient().admin()
            .indices()
            .prepareExists(entitySaveRequest.getIndexName())
            .execute()
            .get()
            .isExists()
            ) {
      throw new Exception("Index, " + entitySaveRequest.getIndexName() + " doesn't exist.");
    }
    val indexRequestBuilder = elasticClient.getClient()
            .prepareIndex(
                    entitySaveRequest.getIndexName(),
                    entitySaveRequest.getMappingType(),
                    entitySaveRequest.getReferenceId()
            )
            .setSource(entitySaveRequest.getValue());
    indexRequestBuilder.setRefreshPolicy(
            WriteRequest.RefreshPolicy.IMMEDIATE
    ).execute().get();
  }

  public void update(UpdateEntityRequest updateEntityRequest)
      throws Exception {
    UpdateRequestBuilder updateRequestBuilder = elasticClient.getClient()
        .prepareUpdate(
                updateEntityRequest.getIndexName(),
                updateEntityRequest.getMappingType(),
                updateEntityRequest.getReferenceId()
        )
        .setDoc(updateEntityRequest.getValue());
    updateRequestBuilder.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE).execute().get();
  }

  public void updateField(UpdateFieldRequest updateFieldRequest) throws Exception {
    UpdateRequest updateRequest = new UpdateRequest(
            updateFieldRequest.getIndexName(),
            updateFieldRequest.getMappingType(),
            updateFieldRequest.getReferenceId()
    ).retryOnConflict(updateFieldRequest.getRetryCount())
            .doc(updateFieldRequest.getField(), updateFieldRequest.getValue());
    elasticClient.getClient().update(updateRequest).get();
  }

  public void reAlias(String newIndex, String aliasName) {
    GetAliasesResponse var = elasticClient.getClient().admin().indices()
        .getAliases(new GetAliasesRequest(aliasName)).actionGet();
    ImmutableOpenMap<String, List<AliasMetaData>> aliases = var.getAliases();

    if (aliases.isEmpty()) {
      elasticClient.getClient()
              .admin()
              .indices()
              .prepareAliases()
              .addAlias(newIndex, aliasName)
              .execute()
              .actionGet();
    }

    String oldIndex = aliases.keysIt().next();
    if (oldIndex.equalsIgnoreCase(newIndex)) return;

    elasticClient.getClient()
            .admin()
            .indices()
            .prepareAliases()
            .removeAlias(oldIndex, aliasName)
            .addAlias(newIndex, aliasName)
            .execute()
            .actionGet();
    elasticClient.getClient()
            .admin()
            .indices()
            .delete(new DeleteIndexRequest(oldIndex))
            .actionGet();
  }

  public <T> List<T> search(SearchRequest<T> searchRequest) throws Exception {
    val query = searchRequest.getQuery();
    QueryBuilder queryBuilder = queryManager.getQueryBuilder(query);

    SearchRequestBuilder searchRequestBuilder = elasticClient.getClient()
        .prepareSearch(searchRequest.getIndex())
        .setTypes(searchRequest.getType())
        .setQuery(queryBuilder);

    if (!query.getSorters().isEmpty()) {
      query.getSorters().forEach(sorter -> searchRequestBuilder.addSort(
          SortBuilders.fieldSort(sorter.getFieldName())
                  .order(ElasticUtils.getSortOrder(sorter.getSortOrder()))
      ));
    }

    SearchResponse searchResponse = searchRequestBuilder
        .setFrom(query.getPageWindow().getPageNumber() * query.getPageWindow().getPageSize())
        .setSize(query.getPageWindow().getPageSize())
        .execute()
        .actionGet();

    return ElasticUtils.getResponse(searchResponse, searchRequest.getKlass());
  }

  public <T> List<T> searchByIds(IdSearchRequest<T> idSearchRequest) {
    MultiGetResponse multiGetItemResponses = elasticClient.getClient().prepareMultiGet().add(
            idSearchRequest.getIndex(),
            idSearchRequest.getType(),
            idSearchRequest.getIds()
    ).get();

    return ElasticUtils.getResponse(multiGetItemResponses, idSearchRequest.getKlass());
  }

  @Override
  public void close() {
      elasticClient.stop();
  }
}
