/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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

package org.eclipse.microprofile.opentracing.tck.annotationimpls;

import java.lang.reflect.Method;

import javax.enterprise.context.Dependent;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.opentracing.NoTrace;
import org.eclipse.microprofile.opentracing.Trace;

/**
 * Implements the interceptor for the @Trace annotation.
 *
 * @author Steve Fontes
 *
 */

@Interceptor
@Dependent
@Trace
public class TraceInterceptor {
    /**
     * Called for methods annotated with @Trace.
     *
     * @param ctx - Access to invocation attributes
     * @return value of method wrapped
     * @throws Exception -
     */
    @AroundInvoke
    public Object traceMethodEntry(final InvocationContext ctx)
            throws Exception {
        Object returnValue;
        Boolean doTrace = false;
        String name;
        String relationship = "child_of";
        try {
            try {
                Method method = ctx.getMethod();
                doTrace = !method.isAnnotationPresent(NoTrace.class);
                if (doTrace) {
                    Trace trace = (Trace) method.getAnnotation(Trace.class);
                    if (trace == null) {
                        name = method.getDeclaringClass().getName()
                                + "."
                                + method.getName();
                    }
                    else {
                        if (trace.name().equals("")) {
                            name = method.getDeclaringClass().getName()
                                    + "."
                                    + method.getName();
                        }
                        else {
                            name = trace.name();
                        }
                        relationship = trace.relationship();
                    }
                    System.out.println("Create new Span ["
                        + name
                        + "] with relationship ["
                        + relationship
                        + "]");
                }
            }
            finally {
                returnValue = ctx.proceed();
            }
        }
        finally {
            if (doTrace) {
                System.out.println("Finish created Span");
            }
        }
        return returnValue;
    }
}
