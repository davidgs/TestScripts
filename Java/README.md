# The Java version of the worker

This is just like the other workers, except it's written in Java.

## Installation

    $ git clone
    $ cd Java
    $ mvn clean install

## Running

    $ cd Java
    $ mvn exec:java

## Output

    % mvn exec:java
    [INFO] Scanning for projects...
    [INFO]
    [INFO] -------< io.camunda.testscript:camunda-platform-testscript-java >-------
    [INFO] Building camunda-platform-testscript-java 1.0-SNAPSHOT
    [INFO] --------------------------------[ jar ]---------------------------------
    [INFO]
    [INFO] --- exec-maven-plugin:3.0.0:java (default-cli) @ camunda-platform-testscript-java ---
    Starting Camunda Cloud Zeebe ScriptWorker
    ===================================
    13:52:20.721 [io.camunda.testscript.TestScript.main()] INFO  io.camunda.testscript.TestScript - Client Connected
    13:52:20.747 [io.camunda.testscript.TestScript.main()] INFO  io.camunda.testscript.TestScript - Job worker opened and receiving jobs for DoMathTask.
    13:52:26.998 [pool-3-thread-1] WARN  io.camunda.testscript.MathHandler - Failed to get variable count, initializing to 0
    13:52:26.998 [pool-3-thread-1] WARN  io.camunda.testscript.MathHandler - Failed to get variable add, initializing to 0
    13:52:26.998 [pool-3-thread-1] INFO  io.camunda.testscript.MathHandler - Starting Job 2251799814893769 with count 0 and add 0:
    13:52:27.030 [pool-3-thread-1] INFO  io.camunda.testscript.MathHandler - Completed Job 2251799814893769 with count 0 and add 0:
    13:52:27.181 [pool-3-thread-1] INFO  io.camunda.testscript.MathHandler - Starting Job 2251799814893781 with count 0 and add 4:
    13:52:27.182 [pool-3-thread-1] INFO  io.camunda.testscript.MathHandler - Completed Job 2251799814893781 with count 4 and add 4:
    13:52:27.333 [pool-3-thread-1] INFO  io.camunda.testscript.MathHandler - Starting Job 2251799814893790 with count 4 and add 5:
    13:52:27.334 [pool-3-thread-1] INFO  io.camunda.testscript.MathHandler - Completed Job 2251799814893790 with count 9 and add 5:

## Details

The Java client is an officially supported client library.

The Java example uses Maven for configuration and building.

```java
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
```

Again, we define the task name to listen for, and then start a worker to listen for those tasks. When a task is created, we use the `MathHandler` to complete the task.

```java
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
```

You'll notice that in Java, we would normally throw an exception if we attempt to get a variable that is not already defined, so we catch that exception, and create the variable instead of failing.

Once again, to run this worker:

```shell
% export ZEEBE_ADDRESS=YOUR_CLUSTER.bru-2.zeebe.camunda.io:443
% export ZEEBE_CLIENT_ID=YOUR_CLIENT_ID
% export ZEEBE_CLIENT_SECRET=YOUR_CLIENT_SECRET
% mvn clean install
% mvn exec:java
```

If you then start a process instance in your C8 cluster, the Java task worker will handle it in exactly the same way as the other task workers did.

> **Note:** Make sure you stop any other task worker before starting this task worker as you don't want the 2 task workers fighting over jobs.
