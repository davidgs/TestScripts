//
//    Copyright (c) 2018 camunda services GmbH (info@camunda.com)
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.

package io.camunda.testscript;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;

import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestScript {

  private static final String PROC_NAME = "DoMathTask";
  static final Boolean DEBUG = true;
  private static final Logger LOG = LogManager.getLogger(TestScript.class);

  public static void main(String[] args) {
    LOG.info("Starting Camunda Cloud Zeebe ScriptWorker");
    LOG.info("===================================");
    try (ZeebeClient client = ZeebeClientFactory.getZeebeClient()) {
      client.newTopologyRequest().send().join();
      LOG.info("Client Connected");
      try (final JobWorker workerRegistration = client
          .newWorker()
          .jobType(PROC_NAME)
          .handler(new MathHandler())
          .open()) {
        LOG.info("Job worker opened and receiving jobs for {}.", PROC_NAME);
        // run until System.in receives exit command
        waitUntilSystemInput("exit");
      }
    } catch (Exception e) {
      LOG.error("Failed to create client: {}", e.getMessage());
      e.printStackTrace();
    }
    waitUntilSystemInput("exit");
  }

  private static void waitUntilSystemInput(final String exitCode) {
    try (final Scanner scanner = new Scanner(System.in)) {
      while (scanner.hasNextLine()) {
        final String nextLine = scanner.nextLine();
        if (nextLine.contains(exitCode)) {
          return;
        }
      }
    }
  }
}

class SimpleMath {
  private int count;
  private int add;

  public int getCount() {
    return count;
  }

  public void setCount(final int count) {
    this.count = count;
  }

  public int getAdd() {
    return add;
  }

  public void setAdd(final int add) {
    this.add = add;
  }

  public int getResult() {
    return count + add;
  }
}

class MathHandler implements JobHandler {
  private static final Logger LOG = LogManager.getLogger(MathHandler.class);

  @Override
  public void handle(final JobClient client, final ActivatedJob job) {
    // read the variables of the job
    final SimpleMath newsum = new SimpleMath();
    try {
      newsum.setCount((int) job.getVariablesAsMap().get("count"));
    } catch (java.lang.NullPointerException e) {
      LOG.warn("Failed to get variable count, initializing to 0");
      newsum.setCount(0);
    }
    try {
      newsum.setAdd((int) job.getVariablesAsMap().get("add"));
    } catch (java.lang.NullPointerException e) {
      LOG.warn("Failed to get variable add, initializing to 0");
      newsum.setAdd(0);
    }
    LOG.info("Starting Job {} with count {} and add {}: ", job.getKey(), newsum.getCount(), newsum.getAdd());
    newsum.setCount(newsum.getResult());
    // update the variables and complete the job

    client.newCompleteCommand(job.getKey()).variables(newsum).send();
    LOG.info("Completed Job {} with count {} and add {}: ", job.getKey(), newsum.getCount(), newsum.getAdd());
  }

}