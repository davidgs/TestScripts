# The C# version of the worker

This is just like the other workers, except it's written in C#.

## Installation

    $ git clone
    $ cd C-Sharp
    $ dotnet build

## Running

    $ cd C-Sharp
    $ dotnet run

## Output

    % dotnet run
    2022-06-02 13:55:55.1651 | DEBUG | Zeebe.Client.Impl.Worker.JobWorker | Job worker (hostname) for job type DoMathTask has been opened.
    2022-06-02 13:55:55.5789 | TRACE | Zeebe.Client.Impl.Builder.CamundaCloudTokenProvider | Read cached access token from /Users/davidgs/.zeebe/credentials
    2022-06-02 13:55:55.7803 | TRACE | Zeebe.Client.Impl.Builder.CamundaCloudTokenProvider | Found access token in credentials file.
    2022-06-02 13:56:08.5443 | DEBUG | Zeebe.Client.Impl.Worker.JobWorker | Job worker (hostname) activated 1 of 120 successfully.
    2022-06-02 13:56:08.5457 | TRACE | Zeebe.Client.Impl.Builder.CamundaCloudTokenProvider | Use in memory access token.
    2022-06-02 13:56:08.5544 | DEBUG | Client.Examples.CloudWorkerExample | Handling job: 4503599628577512
    2022-06-02 13:56:08.5652 | DEBUG | Client.Examples.CloudWorkerExample | Incoming variables: "{}"
    2022-06-02 13:56:08.6040 | DEBUG | Client.Examples.CloudWorkerExample | Job complete with: "{"count":0,"add":0}"
    2022-06-02 13:56:08.6084 | TRACE | Zeebe.Client.Impl.Builder.CamundaCloudTokenProvider | Use in memory access token.
    2022-06-02 13:56:08.8948 | DEBUG | Zeebe.Client.Impl.Worker.JobWorker | Job worker (hostname) activated 1 of 119 successfully.
    2022-06-02 13:56:08.8948 | DEBUG | Client.Examples.CloudWorkerExample | Handling job: 4503599628577533
    2022-06-02 13:56:08.8948 | TRACE | Zeebe.Client.Impl.Builder.CamundaCloudTokenProvider | Use in memory access token.
    2022-06-02 13:56:08.8948 | DEBUG | Client.Examples.CloudWorkerExample | Incoming variables: "{"add":5,"count":4}"
    2022-06-02 13:56:08.9033 | DEBUG | Client.Examples.CloudWorkerExample | Job complete with: "{"count":9,"add":5}"
    2022-06-02 13:56:08.9033 | TRACE | Zeebe.Client.Impl.Builder.CamundaCloudTokenProvider | Use in memory access token.

## Details

The C# client is not an officially supported client library, but we are including an example.

```c#
{
  internal class CloudWorkerExample
  {
    private static readonly string JobType = "DoMathTask";
    private static readonly string WorkerName = Environment.MachineName;

    private static readonly Logger Logger = LogManager.GetCurrentClassLogger();

    public static async Task Main(string[] args)
    {
      // create zeebe client
      var client = CamundaCloudClientBuilder.Builder()
      .FromEnv()
      .UseLoggerFactory(new NLogLoggerFactory())
      .Build();
      // open job worker
      using (var signal = new EventWaitHandle(false, EventResetMode.AutoReset))
      {
        client.NewWorker()
            .JobType(JobType)
            .Handler(HandleJob)
            .MaxJobsActive(120)
            .Name(WorkerName)
            .AutoCompletion()
            .PollInterval(TimeSpan.FromMilliseconds(100))
            .Timeout(TimeSpan.FromSeconds(10))
            .PollingTimeout(TimeSpan.FromSeconds(30))
            .HandlerThreads(8)
            .Open();
        Logger.Info("Starting Camunda Cloud Zeebe ScriptWorker");
        Logger.Info("===================================");
        // blocks main thread, so that worker can run
        signal.WaitOne();
      }
    }

    private static async Task HandleJob(IJobClient jobClient, IJob job)
    {
      Logger.Debug("Handling job: {Key}", job.Key);

      var jobVariables = job.Variables;
      Logger.Debug("Incoming variables: {Variables}", jobVariables);

      var calculation = JsonConvert.DeserializeObject<Calculation>(jobVariables);
      calculation!.AddToCount();
      var result = JsonConvert.SerializeObject(calculation);

      Logger.Debug("Job complete with: {Result}", result);
      await jobClient.NewCompleteJobCommand(job).Variables(result).Send();
    }
  }

  internal class Calculation
  {
    [JsonProperty("count")]
    public int Count { get; set; }
    [JsonProperty("add")]
    public int Add { get; set; }

    public void AddToCount()
    {
      Count += Add;
    }
  }
}
```

Again, we define the task name to listen for, and then start a worker to listen for those tasks. We then get the process variables `count` and `add` (if they exist or we create them if they don't), add them together, and return the result.  Once again, to run this worker:

```shell
% export ZEEBE_ADDRESS=YOUR_CLUSTER.bru-2.zeebe.camunda.io:443
% export ZEEBE_CLIENT_ID=YOUR_CLIENT_ID
% export ZEEBE_CLIENT_SECRET=YOUR_CLIENT_SECRET
% dotnet build
% dotnet run
```

If you then start a process instance in your C8 cluster, the .NET task worker will handle it in exactly the same way as the other task workers did.

> **Note:** Make sure you stop any other task worker before starting this task worker as you don't want the 2 task workers fighting over jobs.
