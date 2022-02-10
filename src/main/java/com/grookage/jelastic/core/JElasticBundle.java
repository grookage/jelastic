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
package com.grookage.jelastic.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.grookage.jelastic.core.config.JElasticConfiguration;
import com.grookage.jelastic.core.elastic.ElasticClient;
import com.grookage.jelastic.core.health.EsClientHealth;
import com.grookage.jelastic.core.managers.QueryManager;
import com.grookage.jelastic.core.repository.ElasticRepository;
import com.grookage.jelastic.core.utils.MapperUtils;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;

/**
 * @author koushik
 */
@NoArgsConstructor
public abstract class JElasticBundle<T extends Configuration> implements ConfiguredBundle<T> {

    @Getter
    private ElasticClient client;
    @Getter
    private ElasticRepository repository;

    public abstract JElasticConfiguration getElasticConfiguration(T configuration);

    /**
     * Sets the objectMapper properties and initializes elasticClient, along with its health check
     *
     * @param configuration {@link T}               The typed config against which the said TrouperBundle is initialized
     * @param environment   {@link Environment}     The dropwizard environment object.
     */
    @Override
    public void run(T configuration, Environment environment) throws Exception {
        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        environment.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        environment.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        environment.getObjectMapper().configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        environment.getObjectMapper().configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);

        MapperUtils.init(environment.getObjectMapper());

        final var jElasticConfiguration = getElasticConfiguration(configuration);
        client = new ElasticClient(getElasticConfiguration(configuration));
        repository = new ElasticRepository(client, new QueryManager());

        environment.healthChecks().register("jelastic-health-check", new EsClientHealth(client, jElasticConfiguration));
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }
}
