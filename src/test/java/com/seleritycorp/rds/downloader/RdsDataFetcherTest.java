/*
 * Copyright (C) 2016 Selerity, Inc. (support@seleritycorp.com)
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

package com.seleritycorp.rds.downloader;

import static org.assertj.core.api.Assertions.assertThat;

import static org.easymock.EasyMock.expect;

import java.io.IOException;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.seleritycorp.common.base.coreservices.CallErrorException;
import com.seleritycorp.common.base.coreservices.RefDataClient;
import com.seleritycorp.common.base.test.SettableConfig;

public class RdsDataFetcherTest extends EasyMockSupport {
  SettableConfig config;
  RefDataClient refDataClient;

  @Before
  public void setUp() throws IOException {
    config = new SettableConfig();
    refDataClient = createMock(RefDataClient.class);
  }

  @Test
  public void testFetchNull() throws IOException, CallErrorException {
    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher(null);
    JsonArray data = fetcher.fetch();

    verifyAll();

    assertThat(data).isNotNull();
    assertThat(data).isEmpty();
  }

  @Test
  public void testFetchEmpty() throws IOException, CallErrorException {
    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher("");
    JsonArray data = fetcher.fetch();

    verifyAll();

    assertThat(data).isNotNull();
    assertThat(data).isEmpty();
  }

  @Test
  public void testFetchBlank() throws IOException, CallErrorException {
    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher("  ");
    JsonArray data = fetcher.fetch();

    verifyAll();

    assertThat(data).isNotNull();
    assertThat(data).isEmpty();
  }

  @Test
  public void testFetchSingle() throws IOException, CallErrorException {
    expect(refDataClient.getIdentifiersForEnumType("foo")).andReturn(new JsonPrimitive(42));

    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher("foo");
    JsonArray data = fetcher.fetch();

    verifyAll();

    assertThat(data).isNotNull();

    JsonObject expectedFoo = new JsonObject();
    expectedFoo.addProperty("foo", 42);

    assertThat(data).contains(expectedFoo);
    assertThat(data).hasSize(1);
  }

  @Test
  public void testFetchMultiple() throws IOException, CallErrorException {
    expect(refDataClient.getIdentifiersForEnumType("foo")).andReturn(new JsonPrimitive(42));
    expect(refDataClient.getIdentifiersForEnumType("bar")).andReturn(new JsonPrimitive("baz"));

    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher("foo,bar");
    JsonArray data = fetcher.fetch();

    verifyAll();

    JsonObject expectedFoo = new JsonObject();
    expectedFoo.addProperty("foo", 42);

    JsonObject expectedBar = new JsonObject();
    expectedBar.addProperty("bar", "baz");

    assertThat(data).contains(expectedFoo);
    assertThat(data).contains(expectedBar);
    assertThat(data).hasSize(2);
  }

  @Test
  public void testFetchMix() throws IOException, CallErrorException {
    expect(refDataClient.getIdentifiersForEnumType("foo")).andReturn(new JsonPrimitive(42));
    expect(refDataClient.getIdentifiersForEnumType("bar")).andReturn(new JsonPrimitive("baz"));

    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher(",   foo  ,, bar,,,");
    JsonArray data = fetcher.fetch();

    verifyAll();

    JsonObject expectedFoo = new JsonObject();
    expectedFoo.addProperty("foo", 42);

    JsonObject expectedBar = new JsonObject();
    expectedBar.addProperty("bar", "baz");

    assertThat(data).contains(expectedFoo);
    assertThat(data).contains(expectedBar);
    assertThat(data).hasSize(2);
  }

  private RdsDataFetcher createRdsDataFetcher(String enumTypes) {
    config.set("RdsDataDownloader.fetcher.enumTypes", enumTypes);
    return new RdsDataFetcher(config, refDataClient);
  }
}
