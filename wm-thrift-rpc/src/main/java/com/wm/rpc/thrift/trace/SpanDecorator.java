/*
 * Copyright 2017-2020 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.wm.rpc.thrift.trace;


import io.opentracing.Span;
import io.opentracing.tag.Tags;
import org.apache.thrift.protocol.TMessage;

import java.util.HashMap;
import java.util.Map;

public class SpanDecorator {

  static final String MESSAGE_TYPE = "message.type";
  static final String COMPONENT_NAME = "java-thrift";
  static final String MESSAGE_NAME = "message.name";
  static final String MESSAGE_SEQID = "message.seqid";
  static final String EVENT = "event";
  static final String ERROR_OBJECT = "error.object";

  public static void decorate(Span span, TMessage message) {
    span.setTag(Tags.COMPONENT.getKey(), COMPONENT_NAME);
    span.setTag(MESSAGE_NAME, message.name);
    span.setTag(MESSAGE_TYPE, message.type);
    span.setTag(MESSAGE_SEQID, message.seqid);
  }

  public static void onError(Throwable throwable, Span span) {
    if (span == null) {
      return;
    }
    span.setTag(Tags.ERROR.getKey(), Boolean.TRUE);
    span.log(errorLogs(throwable));
  }

  private static Map<String, Object> errorLogs(Throwable throwable) {
    Map<String, Object> errorLogs = new HashMap<>(3);
    errorLogs.put(EVENT, Tags.ERROR.getKey());
    if (throwable != null) {
      errorLogs.put(ERROR_OBJECT, throwable);
    }
    return errorLogs;
  }
}
