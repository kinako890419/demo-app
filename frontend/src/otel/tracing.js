import { WebTracerProvider, StackContextManager, BatchSpanProcessor } from '@opentelemetry/sdk-trace-web';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { resourceFromAttributes } from '@opentelemetry/resources';
import { W3CTraceContextPropagator } from '@opentelemetry/core';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { FetchInstrumentation } from '@opentelemetry/instrumentation-fetch';
import { trace } from '@opentelemetry/api';

const exporter = new OTLPTraceExporter({
  url: 'http://localhost:4318/v1/traces',
});

// Configure the trace exporter
const provider = new WebTracerProvider({
  resource: resourceFromAttributes({
    'service.name': 'react-frontend',
    'service.version': '1.0.0',
    'deployment.environment': 'development',
  }),
  spanProcessors: [
    new BatchSpanProcessor(exporter, {
      maxExportBatchSize: 10,
      scheduledDelayMillis: 500,
    }),
  ],
});

provider.register({
  contextManager: new StackContextManager(),
  propagator: new W3CTraceContextPropagator(),
});

// Register auto-instrumentations
// This is what connects the frontend trace to the backend trace (distributed tracing)
registerInstrumentations({
  instrumentations: [
    new FetchInstrumentation({
      propagateTraceHeaderCorsUrls: [/localhost:8181/],
      clearTimingResources: true,
    }),
  ],
});

export const tracer = trace.getTracer('react-frontend');