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
package com.grookage.jelastic.core.models.query.sorter;

import lombok.*;
import org.elasticsearch.common.geo.GeoPoint;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;

@SuppressWarnings("unused")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GeoDistanceSorter extends Sorter {

    @NotNull
    @NotEmpty
    private String fieldName;

    @NotNull
    @NotEmpty
    @Singular
    private Set<GeoPoint> geoPoints;

    public GeoDistanceSorter(){super(SorterType.GEO_DISTANCE);}

    @Builder
    public GeoDistanceSorter(int priority, SortOrder sortOrder, String fieldName, @Singular Set<GeoPoint> geoPoints){
        super(priority, sortOrder, SorterType.GEO_DISTANCE);
        this.geoPoints = geoPoints;
        this.fieldName = fieldName;

    }

    @Override
    public <V> V accept(SorterVisitor<V> visitor) {
        return visitor.visit(this);
    }

}
