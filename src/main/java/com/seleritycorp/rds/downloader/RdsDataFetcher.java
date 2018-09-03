/*
 * Copyright (C) 2016-2018 Selerity, Inc. (support@seleritycorp.com)
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import com.seleritycorp.common.base.config.ApplicationConfig;
import com.seleritycorp.common.base.config.Config;
import com.seleritycorp.common.base.coreservices.CallErrorException;
import com.seleritycorp.common.base.coreservices.RefDataClient;
import com.seleritycorp.common.base.http.client.HttpException;
import com.seleritycorp.common.base.meta.MetaDataFormatter;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;

/**
 * Fetcher for RDS data.
 */
public class RdsDataFetcher {
  private final RefDataClient refDataClient;
  private final List<String> enumTypes;
  private final String agent;

  /**
   * Creates a fetcher of RDS data.
   *
   * @param appConfig The application config to use.
   * @param refDataClient The CoreService client for reference data.
   * @param metaDataFormatter formats the agent for meta data.
   */
  @Inject
  public RdsDataFetcher(@ApplicationConfig Config appConfig, RefDataClient refDataClient,
      MetaDataFormatter metaDataFormatter) {
    this.refDataClient = refDataClient;
    this.agent = metaDataFormatter.getUserAgent();
    String enumTypesString = appConfig.get("RdsDataDownloader.fetcher.enumTypes", "");

    this.enumTypes = new LinkedList<>();
    for (String uncleanedEnumType : enumTypesString.split(",")) {
      String cleanEnumType = uncleanedEnumType.trim();
      if (!cleanEnumType.isEmpty()) {
        this.enumTypes.add(cleanEnumType);
      }
    }
  }

  /**
   * Fetches RDS data.
   *
   * <p>If fetching fails, no re-tries are done.
   *
   * @param  writer Writer object to write the RDS data
   * @throws HttpException for network or other IO issues occur.
   * @throws CallErrorException for server and semantics errors.
   */
  public void fetch(Writer writer) throws CallErrorException, HttpException {
    JsonWriter jsonWriter = new JsonWriter(writer);
    try {
      JsonObject meta = new JsonObject();
      meta.addProperty("format", "RdsData");
      meta.addProperty("version", 2);
      meta.addProperty("agent", agent);
      jsonWriter.beginObject();
      jsonWriter.name("meta").beginObject();
      jsonWriter.name("format").value("RdsData");
      jsonWriter.name("version").value(2);
      jsonWriter.name("agent").value(agent);
      jsonWriter.endObject();
      jsonWriter.name("data").beginObject();
      for (String enumType : enumTypes) {
        jsonWriter.name(enumType);
        refDataClient.getIdentifiersForEnumType(enumType, jsonWriter);
      }
      jsonWriter.endObject();
      jsonWriter.endObject();
    } catch (IOException e) {
      throw new HttpException("Failed while writing the response ", e);
    }
  }
}
