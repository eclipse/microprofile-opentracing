/*
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
# OpenTracing

* Proposal: [MP-0007](0007-DistributedTracing.md)
* Authors: [Akihiko Kuroda](https://github.com/akihikokuroda), [Steve Fontes](https://github.com/Steve-Fontes)
* Status: **Awaiting review**
* Decision Notes: [Discussion thread topic covering the  Rationale](https://groups.google.com/forum/#!topic/microprofile/YxKba36lye4)

## Introduction

Distributed tracing allows you to trace the flow of a request across service boundaries.
This is particularly important in a microservices environment where a request typically flows through multiple services.
To accomplish distributed tracing, each service must be instrumented to log messages with a correlation id that may have been propagated from an upstream service.
A common companion to distributed trace logging is a service where the distributed trace records can be stored. ([Examples](http://opentracing.io/documentation/pages/supported-tracers.html)).
The storage service for distributed trace records can provide features to view the cross service trace records associated with particular request flows.

It will be useful for services written in the microprofile.io framework to be able to integrate well with a distributed trace system that is part of the larger microservices environment.

This proposal defines an API and microprofile.io behaviors that allow services to easily participate in an environment where distributed tracing is enabled.

Mailinglist thread: [Discussion thread topic for that proposal](https://groups.google.com/forum/#!topic/microprofile/YxKba36lye4)

## Motivation

In order for a distributed tracing system to be effective and usable, two things are required
1. The different services in the environment must agree on the mechanism for transferring correlation ids across services.
2. The different services in the environment should produce their trace records in format that is consumable by the storage service for distributed trace records.

Without the first, some services will not be included in the trace records associated with a request.
Without the second, custom code would need to be written to present the information about a full request flow.

There are existing distributed tracing systems that provide a server for distributed trace record storage and viewing, and application libraries for instrumenting microservices.
The problem is that the different distributed tracing systems use implementation specific mechanisms for propagating correlation IDs and for formatting trace records,
so once a microservice chooses a distributed tracing implementation library to use for its instrumentation, all other microservices in the environment are locked into the same choice.

The [opentracing.io project's](http://opentracing.io/) purpose is to provide a standard API for instrumenting microservices for distributed tracing.
If every microservice is instrumented for distributed tracing using the opentracing.io API, then (as long as an implementation library exists for the microservice's language),
the microservice can be configured at deploy time to use a common system implementation to perform the log record formatting and cross service correlation id propagation.
The common implementation ensures that correlation ids are propagated in a way that is understandable to all services,
and log records are formatted in a way that is understandable to the server for distributed trace record storage.

In order to make microprofile.io distributed tracing friendly, it will be useful to allow distributed tracing to be enabled on any microprofile.io application,
without having to explicitly add distributed tracing code to the application.

In order to make microprofile.io as flexible as possible for adding distributed trace log records, microprofile.io should expose whatever objects are necessary for an application to use the opentracing.io API.

This proposal specifically addresses the problem of making it easy to instrument services with distributed tracing function, given an existing distributed tracing system in the environment.

This proposal specifically does not address the problem of defining, implementing, or configuring the underlying distributed tracing system. The proposal assumes an environment where all services use a common opentracing.io implementation (all zipkin compatible, all jaeger compatible, ...). At some point it would be beneficial to define another specification that allows inter-operation between different opentracing.io implementations.

## Proposed solution

The [opentracing.io](http://opentracing.io) API provides a mechanism to include distributed tracing instrumentation across services written in different languages.

The following are the five requirements proposed for providing distributed tracing instrumentation in microprofile.io:

### Requirement 1. Support configuration of an opentracing.io compliant Tracer

Specification does not need to contain how this would be implemented.

### Requirement 2. Allow developer to easily add tracing to an application

This support is implemented with an @Trace annotation, an @NoTrace annotation, and an @TraceDecorate annotation.

#### @Trace(name=&lt;Tracepoint name&gt;, relationship=[ChildOf|FollowsFrom|New])
The @Trace annotation, applies to a block of code. The annotation starts a Span at the beginning of the block, and finishes the Span at the end of the block. When applied to Class, the @Trace annotation is applied to all methods in the Class. The @Trace annotation has two optional arguments.
* name=&lt;Tracepoint name&gt;. Defaults to ClassName.MethodName.
* relationship=[ChildOf|FollowsFrom|New]. Default is ChildOf if a Span is active, else New.

#### @NoTrace
The @NoTrace annotation can only be applied to methods. The @NoTrace annotation overrides an @Trace annotation that was applied at the Class level. The @NoTrace annotation has no arguments

#### @TraceDecorate(tags=&lt;Map of tags&gt;,logs=&lt;Map of logs&gt;,baggage=&lt;Map of baggage&gt;)
The @TraceDecorate annotation adds information to the active Span. The @TraceDecorate can only be used when there is an active Span. The @TraceDecorate annotation has 3 optional arguments.
* tags=&lt;Map of tags&gt;. Default is NULL. Records the tags into the Span.
* logs=&lt;Map of logs&gt;. Default is NULL. Records the logs into the Span.
* baggage=&lt;Map of baggage&gt;. Default is NULL. Records the baggages into the Span.

### Requirement 3. Provide direct programmatic access to opentracing.io API
The @Tracer annotation provides access to the configured Tracer object.

Access to the configured Tracer gives full access to opentracing.io functions.
By https://github.com/opentracing/opentracing-java/pull/115, the opentracing.io specification for Java includes access to the active Span.

Providing the Tracer object enables support for the more complex tracing requirements, such as when a Span is started in one method, and finished in another.

### Requirement 4. Automatically handle a Span extracted from an incoming request

Any method annotated @GET, @PUT, @POST, @DELETE, or @PATCH will have @Trace annotation implicitly added for microprofile.io applications.

In this case an attempt is made to use the configured Tracer to extract a Span from the arriving request headers. If a Span is extracted, it is used as the parent Span for the new Span that is created for the method. This allows a microprofile.io application to easily participate in distributed Span correlation.

### Requirement 5. Automatically inject a Span into outgoing requests

A request for a javax.ws.rs.client.Client will deliver a Client that is extended (decorated, has feature registered, ...) in some way so that when an outbound request is made with that Client, a new Span will be created that is inserted in the outbound request for propagation downstream. The new Span will be a child of the current Span if a current Span exists. The new Span will be finished when the outbound request is completed. This extends the capability for a microprofile.io application to participate in distributed Span correlation.

## Detailed design
Example @Trace applied to a method:

```
@PATH("ServiceA")
public Class MyService {
	...
	@GET
	@PATH("Endpoint1")
	@Trace
	public String ServiceEndpoint1() {
		...
	}
 ...
}
```
The example starts a Span named MyService.ServiceEndpoint1 when the method ServiceEndpoint1 is invoked. The Span is finished when the method ends. If a Span is active when the method is invoked, the started Span will be a child of the active Span, otherwise a new Span is started.

## Impact on existing code
@Trace annotations are added to existing code.

## Alternatives considered
Current mechanisms require a decision at development time about the distributed trace system that will be used.
This feature allows the decision to be made at the operational environment level.
