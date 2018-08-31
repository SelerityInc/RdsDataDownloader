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

import com.google.inject.Injector;

import com.seleritycorp.common.base.config.Config;
import com.seleritycorp.common.base.config.ConfigImpl;
import com.seleritycorp.common.base.config.CustomConfigModule;
import com.seleritycorp.common.base.inject.InjectorFactory;
import com.seleritycorp.common.base.logging.Log;
import com.seleritycorp.common.base.logging.LogFactory;
import com.seleritycorp.common.base.state.AppState;
import com.seleritycorp.common.base.state.StateManager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Starts a Rds data downloading process and runs it periodically.
 */
public class RdsDataDownloaderMain {
  private static final Log log = LogFactory.getLog(RdsDataDownloaderMain.class);

  /**
   * Sets up and starts fetching and persisting of RDS data.
   *
   */
  @SuppressFBWarnings(value = {"UW_UNCOND_WAIT", "WA_NOT_IN_LOOP"},
      justification = "Keeping the main thread alive (to avoid early exit) until user "
          + "interruptions occurs")
  public static void main(final String[] args) {

    Injector injector = init(args);
    StateManager sm = injector.getInstance(StateManager.class);

    RdsDataLifecycle lifecycle = injector.getInstance(RdsDataLifecycle.class);
    lifecycle.start();

    sm.setMainAppState(AppState.READY);

    Object lock = new Object();
    synchronized (lock) {
      try {
        lock.wait();
      } catch (InterruptedException e) {
        log.info("Main method interrupted", e);
      }
    }
  }

  static Injector init(String[] args) {

    Config appDefaults = new ConfigImpl() {
      {
        set("paths.data", "data");
        set("paths.dataState", "state");
      }
    };

    switch (args.length) {
      case 0:
        //no arguments then go with the default configuration strategy
        return InjectorFactory.getInjector();
      case 1:
        if (args[0].equals("--help")) {
          printUsageAndExit(System.out, 0);
        }
        break;
      case 2:
        Path confDir = Paths.get(args[1]);
        if (args[0].equals("--config") && confDir.toFile().isFile()) {
          InjectorFactory.register(
                  new CustomConfigModule(Paths.get(args[1]).toAbsolutePath(), appDefaults));
          return InjectorFactory.getInjector();
        }
        break;
      default:;
    }

    printUsageAndExit(System.err, 1);
    return null;
  }

  static void printUsageAndExit(PrintStream pw, int exitStatus) {
    String usage = ""
            + "Usage: java -jar RDSDataDownloader.jar [--help] [--config <file>]\n"
            + "\n"
            + "Options:\n"
            + "\n"
            + "--help          : print this help\n"
            + "--config <file> : read config from this file\n";

    pw.println(usage);
    System.exit(exitStatus);
  }
}
