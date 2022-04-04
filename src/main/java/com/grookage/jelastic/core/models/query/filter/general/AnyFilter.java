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
package com.grookage.jelastic.core.models.query.filter.general;

import com.grookage.jelastic.core.models.query.filter.Filter;
import com.grookage.jelastic.core.models.query.filter.FilterType;
import com.grookage.jelastic.core.models.query.filter.FilterVisitor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by koushikr
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuppressWarnings("unused")
public class AnyFilter extends Filter {

    public AnyFilter() {
        super(FilterType.ANY);
    }

    public AnyFilter(String fieldName){
        super(FilterType.ANY, fieldName);
    }

    @Override
    public <V> V accept(FilterVisitor<V> visitor) {
        return visitor.visit(this);
    }
}
