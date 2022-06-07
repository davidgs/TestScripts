# The Go version of the worker

This is just like the other workers, except it's written in Go.

## Installation

    $ git clone
    $ cd Golang
    $ go mod tidy

## Running

    $ cd Golang
    $ go run test-script.go

## Output

    % go run test-script.go
    Starting Camunda Cloud Zeebe ScriptWorker
    ===================================
    handleC8Job
    {Add:0 Count:0}
    {Add:0 Count:0}
    Complete job 2251799814889100 of type DoMathTask
    Successfully completed job
    handleC8Job
    {Add:4 Count:0}
    {Add:4 Count:4}
    Complete job 2251799814889110 of type DoMathTask
    Successfully completed job
    handleC8Job
    {Add:5 Count:4}
    {Add:5 Count:9}
    Complete job 2251799814889117 of type DoMathTask
    Successfully completed job

## Details


Go is one of the officially supported languages, so we are providing an example of the task worker in Go.

First, we need to import the required libraries:

```go
  "github.com/camunda-cloud/zeebe/clients/go/pkg/entities"
  "github.com/camunda-cloud/zeebe/clients/go/pkg/worker"
  "github.com/camunda-cloud/zeebe/clients/go/pkg/zbc"
```

Next, we will define the task that we will execute:

```go
// Set this to the name of your task
const PROC_NAME = "DoMathTask"

// Set this to `false` to stop output to the terminal
const DEBUG = true
```

You can turn off all output by setting `DEBUG` to `false` here as well.

Next, we create a `client` that will connect to our C8 cluster:

```go
zeebeAddress := os.Getenv("ZEEBE_ADDRESS")
	if zeebeAddress == "" {
		panic(fmt.Errorf("ZEEBE_ADDRESS is not set"))
	}
	client, err := zbc.NewClient(&zbc.ClientConfig{
		GatewayAddress: zeebeAddress,
	})
	if err != nil {
		log.Fatal("Failed to create client")
		panic(err)
	}
  ```

  To run this task worker, you will have to set the environment variables for your C8 cluster in order for the client to be able to connect, then build and execute the worker.

  ```shell
  % export ZEEBE_ADDRESS=YOUR_CLUSTER.bru-2.zeebe.camunda.io:443
  % export ZEEBE_CLIENT_ID=YOUR_CLIENT_ID
  % export ZEEBE_CLIENT_SECRET=YOUR_CLIENT_SECRET
  % go mod init do-math-task
  % go mod tidy
  % ./do-math-task
  ```

  The task worker will pick those up and use them to create the client instance and connect it to your cluster.

  Finally, a `JobWorker` is started that will listen for, and execute, the `DoMathTask` tasks as they come in. In the configuration provided the `do-math`task` worker will run continuously, waiting for new `DoMathTask` jobs to arrive and handling them as they do.

  ```go
  // Start the job worker to handle jobs
	// This will wait for tasks to be available
	jobWorker := client.NewJobWorker().JobType(PROC_NAME).Handler(a.handleC8Job).Open()

	<-readyClose
	jobWorker.Close()
	jobWorker.AwaitClose()
	return nil
  ```

  We have provided a handle to a function `handleC8Job` that will be called each time a new task is created.

  ```go
// Here's where we handle incoming script task jobs.
func (a *App) handleC8Job(client worker.JobClient, job entities.Job) {
	dPrintln("handleC8Job")
	jobKey := job.GetKey()
	_, err := job.GetCustomHeadersAsMap()
	if err != nil {
		// failed to handle job as we require the custom job headers
		failJob(client, job)
		return
	}
	jobVars := JobVars{}
	err = job.GetVariablesAs(&jobVars)
	if err != nil {
		failJob(client, job)
		return
	}
	dPrintf("%+v\n", jobVars)

	// This is a simple script. We add the two values and return the result.
	jobVars.Count = jobVars.Count + jobVars.Add
	dPrintf("%+v\n", jobVars)
	request, err := client.NewCompleteJobCommand().JobKey(jobKey).VariablesFromObject(jobVars)
	if err != nil {
		// failed to set the updated variables
		failJob(client, job)
		return
	}
	dPrintln("Complete job", jobKey, "of type", job.Type)
	ctx := context.Background()
	_, err = request.Send(ctx)
	if err != nil {
		panic(err)
	}
	dPrintln("Successfully completed job")
}
```

At the bottom of the file are some helper functions for debug printing, and for handling the failure to execute the job, but we won't go through them here.

The really important parts of the `handleC8Job` function are the gathering of the process variables `count` and `add` and then the return of the newly updated variables after we have performed the addition.

If we run this process with no changes, we should end up with a `count` of 9 at the end. (0 + 0 + 4 + 5 = 9)

One thing to note here is that, if you add a start variable json when you start your process instance, that will be executed just as you'd expect. So if we include:

```json
{
  "count": 2,
  "add": 2
}
```

When we start the process, we should end up with 13 at the end (2 + 2 + 4 + 5 = 13).

> **Note:** Make sure you stop any other task worker before starting this task worker as you don't want the 2 task workers fighting over jobs.