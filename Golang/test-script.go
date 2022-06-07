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

package main

import (
	"context"
	"fmt"
	"os"

	log "github.com/sirupsen/logrus"

	"github.com/camunda-cloud/zeebe/clients/go/pkg/entities"
	"github.com/camunda-cloud/zeebe/clients/go/pkg/worker"
	"github.com/camunda-cloud/zeebe/clients/go/pkg/zbc"
)

// Set this to the name of your task
const PROC_NAME = "DoMathTask"

// Set this to `false` to stop output to the terminal
const DEBUG = true

// App dummy Structure
type App struct {
}

// JobVars the variables we get from the Camunda Platform 8 process
type JobVars struct {
	Add   int `json:"add"`
	Count int `json:"count"`
}

var readyClose = make(chan struct{})

func main() {
	a := App{}
	dPrintln("Starting Camunda Cloud Zeebe ScriptWorker")
	dPrintln("===================================")
	err := a.Initialize()
	if err != nil {
		dPrintln("Error:", err)
		log.Fatal(err)
	}
}

// Initialize the Camunda Platform 8 client
func (a *App) Initialize() error {
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
	// Start the job worker to handle jobs
	// This will wait for tasks to be available
	jobWorker := client.NewJobWorker().JobType(PROC_NAME).Handler(a.handleC8Job).Open()

	<-readyClose
	jobWorker.Close()
	jobWorker.AwaitClose()
	return nil
}

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

// If we fail to handle the job for some reason.
func failJob(client worker.JobClient, job entities.Job) {
	dPrintln("Failed to complete job")
	job.GetKey()
	ctx := context.Background()
	_, err := client.NewFailJobCommand().JobKey(job.GetKey()).Retries(job.Retries - 1).Send(ctx)
	if err != nil {
		panic(err)
	}
}

// Printf for debugging
func dPrintf(format string, a ...interface{}) {
	if DEBUG {
		fmt.Printf(format, a...)
	}
}

// Println for debugging
func dPrintln(a ...interface{}) {
	if DEBUG {
		fmt.Println(a...)
	}
}
