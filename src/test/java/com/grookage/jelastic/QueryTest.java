package com.grookage.jelastic;

import com.grookage.jelastic.core.models.query.Query;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

public class QueryTest {

    @Test
    @SneakyThrows
    public void testQuery(){
        final var query = ResourceHelper.getResource(
                "fixtures/query.json",
                Query.class
        );
        Assert.assertNotNull(query);
        Assert.assertFalse(query.getFilters().isEmpty());
        Assert.assertFalse(query.getSorters().isEmpty());
    }
}
