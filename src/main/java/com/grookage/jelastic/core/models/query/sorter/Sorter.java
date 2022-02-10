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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ComparisonChain;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,  property = "sorterType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FieldSorter.class, name = SorterType.FIELD),
        @JsonSubTypes.Type(value = GeoDistanceSorter.class, name = SorterType.GEO_DISTANCE),
        @JsonSubTypes.Type(value = ScriptSorter.class, name = SorterType.SCRIPT),
        @JsonSubTypes.Type(value = ScoreSorter.class, name = SorterType.SCORE)

})
@Data
@NoArgsConstructor
public abstract class Sorter implements Comparable<Sorter>{

    @Min(1)
    public int priority;

    private SortOrder sortOrder;

    private String sorterType;

    @Override
    public int compareTo(Sorter o) {
        return ComparisonChain.start()
                .compare(priority, o.getPriority())
                .result();
    }

    public Sorter(String sorterType) {
      this.sorterType = sorterType;
    }

    public Sorter(int priority, SortOrder sortOrder, String sorterType){
        this.priority = priority;
        this.sortOrder = sortOrder;
        this.sorterType = sorterType;
    }

    public abstract <V> V accept(SorterVisitor<V> visitor);
}
