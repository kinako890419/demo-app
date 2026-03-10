import { WebTracerProvider, StackContextManager, BatchSpanProcessor } from '@opentelemetry/sdk-trace-web';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { Resource, resourceFromAttributes } from '@opentelemetry/resources';
import { W3CTraceContextPropagator } from '@opentelemetry/core';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { XMLHttpRequestInstrumentation } from '@opentelemetry/instrumentation-xml-http-request';
import { trace, Tracer } from '@opentelemetry/api';
import {
    ATTR_SERVICE_NAME,
    ATTR_SERVICE_VERSION,
} from '@opentelemetry/semantic-conventions';

const exporter: OTLPTraceExporter = new OTLPTraceExporter({
  url: 'http://localhost:4318/v1/traces',
});

const resource: Resource = resourceFromAttributes({
    [ATTR_SERVICE_NAME]: 'react-frontend',
    [ATTR_SERVICE_VERSION]: '0.1.0',
});

const provider: WebTracerProvider = new WebTracerProvider({
  resource,
  spanProcessors: [
    new BatchSpanProcessor(exporter, {
      maxExportBatchSize: 10,
      scheduledDelayMillis: 500,
    }),
  ],
});

// Register trace context propagators
provider.register({
  contextManager: new StackContextManager(),
  propagator: new W3CTraceContextPropagator(),
});

// Register auto-instrumentations
registerInstrumentations({
  instrumentations: [
    new XMLHttpRequestInstrumentation({
      propagateTraceHeaderCorsUrls: [/localhost:8181/],
      clearTimingResources: true,
    }),
  ],
});

export const tracer: Tracer = trace.getTracer('react-frontend');