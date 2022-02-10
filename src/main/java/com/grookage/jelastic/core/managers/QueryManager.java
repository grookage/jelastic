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
package com.grookage.jelastic.core.managers;

import com.grookage.jelastic.core.elastic.ElasticQueryBuilder;
import com.grookage.jelastic.core.elastic.ElasticSortBuilder;
import com.grookage.jelastic.core.exception.InvalidQueryException;
import com.grookage.jelastic.core.models.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

/**
 * Created by koushikr on 19/09/17.
 */
@SuppressWarnings("rawtypes")
@Slf4j
@Singleton
public class QueryManager {

    private final ElasticQueryBuilder elasticQueryBuilder;
    private final ElasticSortBuilder elasticSortBuilder;

    public QueryManager() {
        this.elasticQueryBuilder = new ElasticQueryBuilder();
        this.elasticSortBuilder = new ElasticSortBuilder();
    }

    public QueryBuilder getQueryBuilder(Query query) {
        final var boolQueryBuilder = boolQuery();
        try {
            query.getFilters().forEach(k -> boolQueryBuilder.must(k.accept(elasticQueryBuilder)));
        } catch (Exception e) {
            throw new InvalidQueryException("Query incorrect: " + e.getMessage(), e);
        }
        return boolQueryBuilder;
    }

    public List<SortBuilder> getSortBuilders(Query query) {
        return  query.getSorters().stream().map(sorter ->
                sorter.accept(elasticSortBuilder)).collect(Collectors.toList());

    }


}
