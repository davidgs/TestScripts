# The NodeJS version of the worker

This is just like the other workers, except it's written in NodeJS.

## Installation

    $ git clone
    $ cd NodeJS
    $ npm install

## Running

    $ cd NodeJS
    $ npm install
    $ node test-script

## Output

    % node test-script
    Starting Camunda Cloud Zeebe ScriptWorker
    Handling jobs of type DoMathTask
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
const { ZBClient } = require('zeebe-node');

// Change this if you changed the task name in the BPMN file
const PROC_NAME = 'DoMathTask';
// change this to false for no output
const DEBUG = true;

if (DEBUG) {
  console.log("Starting Camunda Cloud Zeebe ScriptWorker")
  console.log(`Handling jobs of type ${PROC_NAME}`)
  console.log("===================================")
} 

const zbc = new ZBClient();

zbc.createWorker({
  taskType: PROC_NAME, 
  taskHandler: job => {
    if (DEBUG) {
      console.log("Handling job: ", job.key)
      console.log("Incoming variables: ", job.variables)
    }

    const count = job.variables.count ?? 0
    const add = job.variables ?? 0
    const newCount = count + add

    if (DEBUG) {
      console.log("Job Complete: ", {...job.variables, add, count: newCount})
    }

    job.complete({ 
      add,
      count: newCount
    })
  }
})
```

Again, we define the task name to listen for, and then start a worker to listen for those tasks. We then get the process variables `count` and `add` (if they exist or we default them to zero if they don't, using nullish coalescing), add them together, and return the result.  Once again, to run this worker:

```shell
% export ZEEBE_ADDRESS=YOUR_CLUSTER.bru-2.zeebe.camunda.io:443
% export ZEEBE_CLIENT_ID=YOUR_CLIENT_ID
% export ZEEBE_CLIENT_SECRET=YOUR_CLIENT_SECRET
% npm install
% node test-script.js
```

If you then start a process instance in your C8 cluster, the Node.js task worker will handle it in exactly the same way as the Go task worker did.

> **Note:** Make sure you stop any other task worker before starting this task worker as you don't want the 2 task workers fighting over jobs.
