/*
 * Copyright (C) 2016-2017 Selerity, Inc. (support@seleritycorp.com)
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.seleritycorp.common.base.coreservices.CallErrorException;
import com.seleritycorp.common.base.coreservices.RefDataClient;
import com.seleritycorp.common.base.meta.MetaDataFormatter;
import com.seleritycorp.common.base.test.SettableConfig;

public class RdsDataFetcherTest extends EasyMockSupport {
  SettableConfig config;
  RefDataClient refDataClient;
  MetaDataFormatter metaDataFormatter;

  @Before
  public void setUp() throws IOException {
    config = new SettableConfig();
    refDataClient = createMock(RefDataClient.class);
    metaDataFormatter = createMock(MetaDataFormatter.class);
    expect(metaDataFormatter.getUserAgent()).andReturn("quux").once();
  }

  @Test
  public void testFetchNull() throws IOException, CallErrorException {
    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher(null);
    JsonObject fetchedData = fetcher.fetch();

    verifyAll();

    assertThat(fetchedData).isNotNull();

    verifyMeta(fetchedData);

    JsonObject data = getData(fetchedData);
    assertThat(data.entrySet()).isEmpty();
  }

  @Test
  public void testFetchEmpty() throws IOException, CallErrorException {
    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher("");
    JsonObject fetchedData = fetcher.fetch();

    verifyAll();

    assertThat(fetchedData).isNotNull();

    verifyMeta(fetchedData);

    JsonObject data = getData(fetchedData);
    assertThat(data.entrySet()).isEmpty();
  }

  @Test
  public void testFetchBlank() throws IOException, CallErrorException {
    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher("  ");
    JsonObject fetchedData = fetcher.fetch();

    verifyAll();

    assertThat(fetchedData).isNotNull();

    verifyMeta(fetchedData);

    JsonObject data = getData(fetchedData);
    assertThat(data.entrySet()).isEmpty();
  }

  @Test
  public void testFetchSingle() throws IOException, CallErrorException {
    JsonElement elementFoo = new JsonPrimitive(42);
    expect(refDataClient.getIdentifiersForEnumType("foo")).andReturn(elementFoo);

    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher("foo");
    JsonObject fetchedData = fetcher.fetch();

    verifyAll();

    assertThat(fetchedData).isNotNull();

    verifyMeta(fetchedData);

    JsonObject data = getData(fetchedData);

    assertThat(data.get("foo")).isEqualTo(elementFoo);
    assertThat(data.entrySet()).hasSize(1);
  }

  @Test
  public void testFetchMultiple() throws IOException, CallErrorException {
    JsonElement elementFoo = new JsonPrimitive(42);
    expect(refDataClient.getIdentifiersForEnumType("foo")).andReturn(elementFoo);
    JsonElement elementBar = new JsonPrimitive("baz");
    expect(refDataClient.getIdentifiersForEnumType("bar")).andReturn(elementBar);

    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher("foo,bar");
    JsonObject fetchedData = fetcher.fetch();

    verifyAll();

    assertThat(fetchedData).isNotNull();

    verifyMeta(fetchedData);

    JsonObject data = getData(fetchedData);

    assertThat(data.get("foo")).isEqualTo(elementFoo);
    assertThat(data.get("bar")).isEqualTo(elementBar);
    assertThat(data.entrySet()).hasSize(2);
  }

  @Test
  public void testFetchMix() throws IOException, CallErrorException {
    JsonElement elementFoo = new JsonPrimitive(42);
    expect(refDataClient.getIdentifiersForEnumType("foo")).andReturn(elementFoo);
    JsonElement elementBar = new JsonPrimitive("baz");
    expect(refDataClient.getIdentifiersForEnumType("bar")).andReturn(elementBar);

    replayAll();

    RdsDataFetcher fetcher = createRdsDataFetcher(",   foo  ,, bar,,,");
    JsonObject fetchedData = fetcher.fetch();

    verifyAll();

    assertThat(fetchedData).isNotNull();

    verifyMeta(fetchedData);

    JsonObject data = getData(fetchedData);

    assertThat(data.get("foo")).isEqualTo(elementFoo);
    assertThat(data.get("bar")).isEqualTo(elementBar);
    assertThat(data.entrySet()).hasSize(2);
  }

  private RdsDataFetcher createRdsDataFetcher(String enumTypes) {
    config.set("RdsDataDownloader.fetcher.enumTypes", enumTypes);
    return new RdsDataFetcher(config, refDataClient, metaDataFormatter);
  }

  private void verifyMeta(JsonElement fetchedData) {
    assertThat(fetchedData.isJsonObject()).isTrue();

    assertThat(fetchedData.getAsJsonObject().get("meta").isJsonObject()).isTrue();
    JsonObject meta = fetchedData.getAsJsonObject().get("meta").getAsJsonObject();

    assertThat(meta.get("format").isJsonPrimitive());
    assertThat(meta.get("format").getAsString()).isEqualTo("RdsData");

    assertThat(meta.get("version").isJsonPrimitive());
    assertThat(meta.get("version").getAsInt()).isEqualTo(2);

    assertThat(meta.get("agent").isJsonPrimitive());
    assertThat(meta.get("agent").getAsString()).isEqualTo("quux");
  }

  private JsonObject getData(JsonElement fetchedData) {
    assertThat(fetchedData.isJsonObject()).isTrue();
    assertThat(fetchedData.getAsJsonObject().get("data").isJsonObject()).isTrue();
    return fetchedData.getAsJsonObject().get("data").getAsJsonObject();
  }
}
