//
// Copyright (c) 2017 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
# MicroProfile OpenTracing

The MicroProfile OpenTracing specification defines behaviors and an API for accessing
an OpenTracing compliant Tracer object within your JAX-RS application.
The behaviors specify how incoming and outgoing requests will have OpenTracing
Spans automatically created. The API defines how to explicitly disable or enable
tracing for given endpoints.

* For the current specification see link:https://github.com/eclipse/microprofile-opentracing/blob/master/spec/src/main/asciidoc/microprofile-opentracing-spec.asciidoc[microprofile-opentracing-spec].

## How to contribute

Do you want to contribute to this project? Here is what you can do:

* Fork the repository, make changes, then do a pull request.
* https://github.com/eclipse/microprofile-opentracing/issues[Create or fix an issues].
* https://gitter.im/eclipse/microprofile-opentracing[Join us on Gitter to discuss this project].
* Join our https://calendar.google.com/calendar/embed?src=gbnbc373ga40n0tvbl88nkc3r4%40group.calendar.google.com[bi-weekly meeting] every second Wednesday at https://www.timeanddate.com/time/map/[16h00 GMT]. 
** https://docs.google.com/document/d/1TbeKThAd9Df6IS0S6lz_YsTmxpFqWRmCL2HjbrAyLPQ/edit#heading=h.t9s5gp6jb2g6[Minutes and Agenda].
** https://eclipse.zoom.us/j/949859967[Meeting room].
* Join the discussions on the https://groups.google.com/forum/#!forum/microprofile[MicroProfile Google Group]
* https://microprofile.io/blog/[Contribute a blog post].

## Build

```bash
mvn clean install
```
