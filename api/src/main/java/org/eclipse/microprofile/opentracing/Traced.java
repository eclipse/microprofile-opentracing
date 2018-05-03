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

package org.eclipse.microprofile.opentracing;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation allows fine-tuned control over which classes and methods
 * create OpenTracing spans. By default, all JAX-RS methods implicitly have
 * this annotation.
 *
 * This annotation applies to a class or a method. When applied to a class, this
 * annotation is applied to all methods of the class. If the annotation is
 * applied to a class and method then the annotation applied to the method takes
 * precedence. The annotation starts a Span at the beginning of the method, and
 * finishes the Span at the end of the method.
 *
 * This annotation also has {@code InterceptorBinding} for frameworks to
 * process all of each application's explicit {@code Traced} annotations.
 *
 * @author <a href="mailto:steve.m.fontes@gmail.com">Steve Fontes</a>
 */
@Documented
@InterceptorBinding
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface Traced {
    /**
     * Defaults to true. If <code>@Traced</code> is specified at the class
     * level, then <code>@Traced(false)</code> is used to annotate specific
     * methods to disable creation of a Span for those methods. By default all
     * JAX-RS endpoint methods are traced. To disable Span creation of a
     * specific JAX-RS endpoint, the @Traced(false) annotation can be used.
     *
     * When the <code>@Traced(false)</code> annotation is used for a JAX-RS
     * endpoint method, the upstream SpanContext will not be extracted. Any
     * Spans created, either automatically for outbound requests, or explicitly
     * using an injected Tracer, will not have an upstream parent Span in the
     * Span hierarchy.
     *
     * @return whether this method should be traced.
     */
    @Nonbinding
    boolean value() default true;

    /**
     * Default is "". If the <code>@Traced</code> annotation finds the
     * operationName as "", the default operation name is used. For a JAX-RS
     * endpoint method, it is
     * <code>${HTTP method}:${package name}.${class name}.${method name}</code>.
     * If the annotated method is not a JAX-RS endpoint, the default operation
     * name of the new Span for the method is:
     * <code>${package name}.${class name}.${method name}</code>. If
     * operationName is specified on a class, that operationName will be used
     * for all methods of the class unless a method explicitly overrides it with
     * its own operationName.
     *
     * @return the name to give the Span for this trace point.
     */
    @Nonbinding
    String operationName() default "";
}
