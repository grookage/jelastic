/*
 * Copyright 2021 Koushik R <rkoushik.14@gmail.com>.
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
package com.grookage.jelastic.core.elastic;

import com.grookage.jelastic.core.models.query.sorter.*;
import com.grookage.jelastic.core.utils.ElasticUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;

@SuppressWarnings({"rawtypes", "ToArrayCallWithZeroLengthArrayArgument"})
public class ElasticSortBuilder implements SorterVisitor<SortBuilder> {

    @Override
    public SortBuilder visit(FieldSorter fieldSorter) {
        return SortBuilders.fieldSort(fieldSorter.getFieldName())
                .order(ElasticUtils.getSortOrder(fieldSorter.getSortOrder()));
    }

    @Override
    public SortBuilder visit(GeoDistanceSorter geoDistanceSorter) {
        return SortBuilders.geoDistanceSort(geoDistanceSorter.getFieldName(),
                geoDistanceSorter.getGeoPoints().toArray(new GeoPoint[geoDistanceSorter.getGeoPoints().size()]))
                .order(ElasticUtils.getSortOrder(geoDistanceSorter.getSortOrder()));
    }

    @Override
    public SortBuilder visit(ScriptSorter scriptSorter) {
        return SortBuilders.scriptSort(scriptSorter.getScript(), scriptSorter.getScriptSortType())
                .order(ElasticUtils.getSortOrder(scriptSorter.getSortOrder()));
    }

    @Override
    public SortBuilder visit(ScoreSorter scoreSorter) {
        return SortBuilders.scoreSort().order(ElasticUtils.getSortOrder(scoreSorter.getSortOrder()));
    }
}
