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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.seleritycorp.common.base.config.ApplicationConfig;
import com.seleritycorp.common.base.config.Config;
import com.seleritycorp.common.base.coreservices.CallErrorException;
import com.seleritycorp.common.base.coreservices.RefDataClient;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;

/**
 * Fetcher for RDS data.
 */
public class RdsDataFetcher {
  private final RefDataClient refDataClient;
  private final List<String> enumTypes;

  /**
   * Creates a fetcher of RDS data.
   * 
   * @param appConfig The application config to use.
   * @param refDataClient The CoreService client for reference data.
   */
  @Inject
  public RdsDataFetcher(@ApplicationConfig Config appConfig, RefDataClient refDataClient) {
    this.refDataClient = refDataClient;
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
   * @throws IOException for network or other IO issues occur.
   * @throws CallErrorException for server and semantics errors.
   */
  public JsonArray fetch() throws IOException, CallErrorException {
    JsonArray ret = new JsonArray();
    for (String enumType : enumTypes) {
      JsonElement identifiers = refDataClient.getIdentifiersForEnumType(enumType);

      JsonObject wrappedIdentifiers = new JsonObject();
      wrappedIdentifiers.add(enumType, identifiers);

      ret.add(wrappedIdentifiers);
    }
    return ret;
  }
}
