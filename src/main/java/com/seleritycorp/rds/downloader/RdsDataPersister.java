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

import com.google.gson.JsonObject;

import com.seleritycorp.common.base.config.ApplicationConfig;
import com.seleritycorp.common.base.config.ApplicationPaths;
import com.seleritycorp.common.base.config.Config;
import com.seleritycorp.common.base.config.ConfigUtils;
import com.seleritycorp.common.base.logging.Log;
import com.seleritycorp.common.base.logging.LogFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.inject.Inject;

/**
 * Persists RDS data.
 *
 * <p>To avoid having other processes read half-written files, data first get written to a
 * temporary file. Once this temporary file has been fully written, the file is moved to the final
 * place in one atomic operation.
 */
public class RdsDataPersister {
  private static final Log log = LogFactory.getLog(RdsDataPersister.class);

  private final Path target;
  private final Path targetParent;
  private final Path tmpTarget;
  private final Path tmpTargetParent;
  private Writer tmpTargetWriter;

  /**
   * Creates a persister for RDS data.
   *
   * @param appConfig The application config to use.
   * @param appPaths The base paths to write data to.
   */
  @Inject
  public RdsDataPersister(@ApplicationConfig Config appConfig, ApplicationPaths appPaths) {
    Config config = ConfigUtils.subconfig(appConfig, "RdsDataDownloader.persister");

    this.target = appPaths.getDataPath().resolve(config.get("target", "rds/rds-data.json"));
    this.targetParent = target.getParent();

    if (config.get("tmpTarget") == null) {
      if (this.targetParent == null) {
        this.tmpTarget = Paths.get(this.target.getFileName() + ".tmp");
      } else {
        this.tmpTarget = this.targetParent.resolve(this.target.getFileName() + ".tmp");
      }
    } else {
      this.tmpTarget = appPaths.getDataPath().resolve(config.get("tmpTarget"));
    }
    this.tmpTargetParent = this.tmpTarget.getParent();
  }

  /**
   * Retrieves the writer object to the temp file. Every time it's called, it
   * returns the new writer i.e it wipes out the temp file and create new one
   *
   * @return Writer object
   * @throws IOException while creating the writer object
   */
  public Writer getCleanWriter() throws IOException {
    if (tmpTargetWriter != null) {
      tmpTargetWriter.close();
    }
    if (!Files.isDirectory(tmpTargetParent)) {
      try {
        Files.createDirectories(tmpTargetParent);
      } catch (IOException e) {
        throw new IOException("Failed to create temporary target directory " + tmpTargetParent, e);
      }
    }
    tmpTargetWriter = new BufferedWriter(
            new OutputStreamWriter(
                    Files.newOutputStream(tmpTarget),
                    StandardCharsets.UTF_8),
            16 * 1024 * 1024);
    return tmpTargetWriter;
  }

  /**
   * Persist data fetched from RDS.
   *
   * @throws IOException for errors while persisting.
   */
  public void persist() throws Exception {
    log.info("Persisting RDS data to " + tmpTarget);
    if (this.tmpTargetWriter == null) {
      throw new Exception(
              "Writer is not initialized - Fetch the data into the writer before calling persist!");
    }
    this.tmpTargetWriter.flush();
    this.tmpTargetWriter.close();
    if (Files.isRegularFile(tmpTarget) && Files.size(tmpTarget) == 0) {
      throw new Exception("Downloaded RDS data is empty!");
    }
    // At this point, the Rds data has been persistent.
    // Now moving it to the target path, atomically.
    log.info("Moving RDS data to " + target);

    if (!Files.isDirectory(targetParent)) {
      try {
        Files.createDirectories(targetParent);
      } catch (IOException e) {
        throw new IOException("Failed to create target directory " + targetParent, e);
      }
    }

    try {
      Files.move(tmpTarget, target, StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new IOException(
              "Failed to move temporary target " + tmpTarget + " to effective target " + target);
    }
  }
}
