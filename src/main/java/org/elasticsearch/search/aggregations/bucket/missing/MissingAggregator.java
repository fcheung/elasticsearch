/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.search.aggregations.bucket.missing;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.util.Bits;
import org.elasticsearch.search.aggregations.Aggregator;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.bucket.SingleBucketAggregator;
import org.elasticsearch.search.aggregations.support.AggregationContext;
import org.elasticsearch.search.aggregations.support.ValuesSource;
import org.elasticsearch.search.aggregations.support.ValuesSourceAggregatorFactory;
import org.elasticsearch.search.aggregations.support.ValuesSourceConfig;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class MissingAggregator extends SingleBucketAggregator {

    private final ValuesSource valuesSource;
    private Bits docsWithValue;

    public MissingAggregator(String name, AggregatorFactories factories, ValuesSource valuesSource,
                             AggregationContext aggregationContext, Aggregator parent, Map<String, Object> metaData) throws IOException {
        super(name, factories, aggregationContext, parent, metaData);
        this.valuesSource = valuesSource;
    }

    @Override
    public void setNextReader(LeafReaderContext reader) {
        if (valuesSource != null) {
            docsWithValue = valuesSource.docsWithValue(reader.reader().maxDoc());
        } else {
            docsWithValue = new Bits.MatchNoBits(reader.reader().maxDoc());
        }
    }

    @Override
    public void collect(int doc, long owningBucketOrdinal) throws IOException {
        if (docsWithValue != null && !docsWithValue.get(doc)) {
            collectBucket(doc, owningBucketOrdinal);
        }
    }

    @Override
    public InternalAggregation buildAggregation(long owningBucketOrdinal) throws IOException {
        return new InternalMissing(name, bucketDocCount(owningBucketOrdinal), bucketAggregations(owningBucketOrdinal), metaData());
    }

    @Override
    public InternalAggregation buildEmptyAggregation() {
        return new InternalMissing(name, 0, buildEmptySubAggregations(), metaData());
    }

    public static class Factory extends ValuesSourceAggregatorFactory<ValuesSource>  {

        public Factory(String name, ValuesSourceConfig valueSourceConfig) {
            super(name, InternalMissing.TYPE.name(), valueSourceConfig);
        }

        @Override
        protected MissingAggregator createUnmapped(AggregationContext aggregationContext, Aggregator parent, Map<String, Object> metaData) throws IOException {
            return new MissingAggregator(name, factories, null, aggregationContext, parent, metaData);
        }

        @Override
        protected MissingAggregator doCreateInternal(ValuesSource valuesSource, AggregationContext aggregationContext, Aggregator parent, boolean collectsFromSingleBucket, Map<String, Object> metaData) throws IOException {
            return new MissingAggregator(name, factories, valuesSource, aggregationContext, parent, metaData);
        }
    }

}


