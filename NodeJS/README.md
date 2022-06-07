# The NodeJS version of the worker

This is just like the other workers, except it's written in NodeJS.

## Installation

    $ git clone
    $ cd NodeJS
    $ npm install

## Running

    $ cd NodeJS
    $ node test-script

## Output

    % node test-script
    Starting Camunda Cloud Zeebe ScriptWorker
    ===================================
    14:00:56.071 | zeebe |  INFO: Authenticating client with Camunda Cloud...
    Handling job:  2251799814893972
    Incoming variables:  { count: 0, add: 0 }
    Job Complete:  { count: 0, add: 0 }
    Handling job:  2251799814893982
    Incoming variables:  { add: 4, count: 0 }
    Job Complete:  { add: 4, count: 4 }
    Handling job:  2251799814893989
    Incoming variables:  { add: 5, count: 4 }
    Job Complete:  { add: 5, count: 9 }

## Details

The Node.js client is not an officially supported client library, but since NodeJS is so popular, we are including an example.

The NodeJS example is even shorter than the others.

```js
const ZB = require('zeebe-node');

// Change this if you changed the task name in the BPMN file
const PROC_NAME = 'DoMathTask';
// change this to false for no output
const DEBUG = true;

if (DEBUG) {
  console.log("Starting Camunda Cloud Zeebe ScriptWorker")
  console.log("===================================")
}
  ; (async () => {
    const zbc = new ZB.ZBClient()
    zbc.createWorker(PROC_NAME, (job) => {
      if (DEBUG) {
        console.log("Handling job: ", job.key)
      }
      if (job.variables.count === undefined) {
        job.variables.count = 0
      }
      if (job.variables.add === undefined) {
        job.variables.add = 0
      }
      if (DEBUG) {
        console.log("Incoming variables: ", job.variables)
      }
      job.variables.count = job.variables.count + job.variables.add
      if (DEBUG) {
        console.log("Job Complete: ", job.variables)
      }
      job.complete(job.variables)
    })
  })()
```

Again, we define the task name to listen for, and then start a worker to listen for those tasks. We then get the process variables `count` and `add` (if they exist or we create them if they don't), add them together, and return the result.  Once again, to run this worker:

```shell
% export ZEEBE_ADDRESS=YOUR_CLUSTER.bru-2.zeebe.camunda.io:443
% export ZEEBE_CLIENT_ID=YOUR_CLIENT_ID
% export ZEEBE_CLIENT_SECRET=YOUR_CLIENT_SECRET
% npm install
% node test-script.js
```

If you then start a process instance in your C8 cluster, the Node.js task worker will handle it in exactly the same way as the Go task worker did.

> **Note:** Make sure you stop any other task worker before starting this task worker as you don't want the 2 task workers fighting over jobs.
