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


const { ZBClient } = require('zeebe-node');

// Change this if you changed the task name in the BPMN file
const PROC_NAME = 'DoMathTask';
// change this to false for no output
const DEBUG = true;

if (DEBUG) {
  console.log("Starting Camunda Cloud Zeebe ScriptWorker")
  console.log("===================================")
}
; (async () => {
  const zbc = new ZBClient();

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
