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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.seleritycorp.common.base.config.ApplicationConfig;
import com.seleritycorp.common.base.config.Config;
import com.seleritycorp.common.base.coreservices.CallErrorException;
import com.seleritycorp.common.base.coreservices.RefDataClient;
import com.seleritycorp.common.base.http.client.HttpException;
import com.seleritycorp.common.base.meta.MetaDataFormatter;

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
   * @return the fetched data
   * @throws HttpException for network or other IO issues occur.
   * @throws CallErrorException for server and semantics errors.
   */
  public JsonObject fetch() throws CallErrorException, HttpException {
    JsonObject meta = new JsonObject();
    meta.addProperty("format", "RdsData");
    meta.addProperty("version", 2);
    meta.addProperty("agent", agent);

    JsonObject ret = new JsonObject();
    ret.add("meta", meta);

    JsonObject data = new JsonObject();
    ret.add("data", data);
    for (String enumType : enumTypes) {
      JsonElement identifiers = refDataClient.getIdentifiersForEnumType(enumType);
      data.add(enumType, identifiers);
    }
    return ret;
  }
}
