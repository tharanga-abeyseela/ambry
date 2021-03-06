/**
 * Copyright 2016 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.github.ambry.router;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.ambry.clustermap.ClusterMap;
import com.github.ambry.clustermap.DataNodeId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * {@link NonBlockingRouter}-specific metrics tracking.
 * <p/>
 * Exports metrics that are triggered by the {@link NonBlockingRouter} to the provided {@link MetricRegistry}
 */
public class NonBlockingRouterMetrics {
  private final MetricRegistry metricRegistry;
  // @todo: Ensure all metrics here get updated appropriately.
  // @todo: chunk filling rate metrics.
  // @todo: More metrics for the RequestResponse handling (poll, handleResponse etc.)

  // Operation rate.
  public final Meter putBlobOperationRate;
  public final Meter getBlobInfoOperationRate;
  public final Meter getBlobOperationRate;
  public final Meter deleteBlobOperationRate;
  public final Meter operationQueuingRate;
  public final Meter operationDequeuingRate;

  // Latency.
  public final Histogram putBlobOperationLatencyMs;
  public final Histogram putChunkOperationLatencyMs;
  public final Histogram getBlobInfoOperationLatencyMs;
  public final Histogram getBlobOperationLatencyMs;
  public final Histogram getBlobOperationTotalTimeMs;
  public final Histogram deleteBlobOperationLatencyMs;
  public final Histogram routerRequestLatencyMs;

  // Operation error count.
  public final Counter putBlobErrorCount;
  public final Counter getBlobInfoErrorCount;
  public final Counter getBlobErrorCount;
  public final Counter deleteBlobErrorCount;
  public final Counter operationAbortCount;

  // Count for various errors.
  public final Counter ambryUnavailableErrorCount;
  public final Counter invalidBlobIdErrorCount;
  public final Counter invalidPutArgumentErrorCount;
  public final Counter operationTimedOutErrorCount;
  public final Counter routerClosedErrorCount;
  public final Counter unexpectedInternalErrorCount;
  public final Counter blobTooLargeErrorCount;
  public final Counter badInputChannelErrorCount;
  public final Counter insufficientCapacityErrorCount;
  public final Counter blobDeletedErrorCount;
  public final Counter blobDoesNotExistErrorCount;
  public final Counter blobExpiredErrorCount;
  public final Counter unknownReplicaResponseError;
  public final Counter unknownErrorCountForOperation;

  // Performance metrics for operation managers.
  public final Histogram putManagerPollTimeMs;
  public final Histogram getManagerPollTimeMs;
  public final Histogram deleteManagerPollTimeMs;
  public final Histogram putManagerHandleResponseTimeMs;
  public final Histogram getManagerHandleResponseTimeMs;
  public final Histogram deleteManagerHandleResponseTimeMs;

  // Misc metrics.
  public final Meter operationErrorRate;
  public final Counter slippedPutSuccessCount;
  public final Counter ignoredResponseCount;
  public Gauge<Long> chunkFillerThreadRunning;
  public Gauge<Long> requestResponseHandlerThreadRunning;
  public Gauge<Integer> activeOperations;

  // Map that stores dataNode-level metrics.
  private final Map<DataNodeId, NodeLevelMetrics> dataNodeToMetrics;

  public NonBlockingRouterMetrics(ClusterMap clusterMap) {
    metricRegistry = clusterMap.getMetricRegistry();

    // Operation Rate.
    putBlobOperationRate = metricRegistry.meter(MetricRegistry.name(PutOperation.class, "PutBlobOperationRate"));
    getBlobInfoOperationRate =
        metricRegistry.meter(MetricRegistry.name(GetBlobInfoOperation.class, "GetBlobInfoOperationRate"));
    getBlobOperationRate = metricRegistry.meter(MetricRegistry.name(GetBlobOperation.class, "GetBlobOperationRate"));
    deleteBlobOperationRate =
        metricRegistry.meter(MetricRegistry.name(DeleteOperation.class, "DeleteBlobOperationRate"));
    operationQueuingRate = metricRegistry.meter(MetricRegistry.name(NonBlockingRouter.class, "OperationQueuingRate"));
    operationDequeuingRate =
        metricRegistry.meter(MetricRegistry.name(NonBlockingRouter.class, "OperationDequeuingRate"));

    // Latency.
    putBlobOperationLatencyMs =
        metricRegistry.histogram(MetricRegistry.name(PutOperation.class, "PutBlobOperationLatencyMs"));
    putChunkOperationLatencyMs =
        metricRegistry.histogram(MetricRegistry.name(PutOperation.class, "PutChunkOperationLatencyMs"));
    getBlobInfoOperationLatencyMs =
        metricRegistry.histogram(MetricRegistry.name(GetBlobInfoOperation.class, "GetBlobInfoOperationLatencyMs"));
    getBlobOperationLatencyMs =
        metricRegistry.histogram(MetricRegistry.name(GetBlobOperation.class, "GetBlobOperationLatencyMs"));
    getBlobOperationTotalTimeMs =
        metricRegistry.histogram(MetricRegistry.name(GetBlobOperation.class, "GetBlobOperationTotalTimeMs"));
    deleteBlobOperationLatencyMs =
        metricRegistry.histogram(MetricRegistry.name(DeleteOperation.class, "DeleteBlobOperationLatencyMs"));
    routerRequestLatencyMs =
        metricRegistry.histogram(MetricRegistry.name(NonBlockingRouter.class, "RouterRequestLatencyMs"));

    // Operation error count.
    putBlobErrorCount = metricRegistry.counter(MetricRegistry.name(PutOperation.class, "PutBlobErrorCount"));
    getBlobInfoErrorCount =
        metricRegistry.counter(MetricRegistry.name(GetBlobInfoOperation.class, "GetBlobInfoErrorCount"));
    getBlobErrorCount = metricRegistry.counter(MetricRegistry.name(GetBlobOperation.class, "GetBlobErrorCount"));
    deleteBlobErrorCount = metricRegistry.counter(MetricRegistry.name(DeleteOperation.class, "DeleteBlobErrorCount"));
    operationAbortCount = metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "OperationAbortCount"));

    // Counters for various errors.
    ambryUnavailableErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "AmbryUnavailableErrorCount"));
    invalidBlobIdErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "InvalidBlobIdErrorCount"));
    invalidPutArgumentErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "InvalidPutArgumentErrorCount"));
    operationTimedOutErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "OperationTimedOutErrorCount"));
    routerClosedErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "RouterClosedErrorCount"));
    unexpectedInternalErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "UnexpectedInternalErrorCount"));
    blobTooLargeErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "BlobTooLargeErrorCount"));
    badInputChannelErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "BadInputChannelErrorCount"));
    insufficientCapacityErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "InsufficientCapacityErrorCount"));
    blobDeletedErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "BlobDeletedErrorCount"));
    blobDoesNotExistErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "BlobDoesNotExistErrorCount"));
    blobExpiredErrorCount =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "BlobExpiredErrorCount"));
    unknownReplicaResponseError =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "UnknownReplicaResponseError"));
    unknownErrorCountForOperation =
        metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "UnknownErrorCountForOperation"));

    // Performance metrics for operation managers.
    putManagerPollTimeMs = metricRegistry.histogram(MetricRegistry.name(PutManager.class, "PutManagerPollTimeMs"));
    getManagerPollTimeMs = metricRegistry.histogram(MetricRegistry.name(GetManager.class, "GetManagerPollTimeMs"));
    deleteManagerPollTimeMs =
        metricRegistry.histogram(MetricRegistry.name(DeleteManager.class, "DeleteManagerPollTimeMs"));
    putManagerHandleResponseTimeMs =
        metricRegistry.histogram(MetricRegistry.name(PutManager.class, "PutManagerHandleResponseTimeMs"));
    getManagerHandleResponseTimeMs =
        metricRegistry.histogram(MetricRegistry.name(GetManager.class, "GetManagerHandleResponseTimeMs"));
    deleteManagerHandleResponseTimeMs =
        metricRegistry.histogram(MetricRegistry.name(DeleteManager.class, "DeleteManagerHandleResponseTimeMs"));

    // Misc metrics.
    operationErrorRate = metricRegistry.meter(MetricRegistry.name(NonBlockingRouter.class, "OperationErrorRate"));
    slippedPutSuccessCount = metricRegistry.counter(MetricRegistry.name(PutOperation.class, "SlippedPutSuccessCount"));
    ignoredResponseCount = metricRegistry.counter(MetricRegistry.name(NonBlockingRouter.class, "IgnoredRequestCount"));

    // Track metrics at the DataNode level.
    dataNodeToMetrics = new HashMap<>();
    for (DataNodeId dataNodeId : clusterMap.getDataNodeIds()) {
      String dataNodeName = dataNodeId.getDatacenterName() + "." + dataNodeId.getHostname() + "." + Integer
          .toString(dataNodeId.getPort());
      dataNodeToMetrics.put(dataNodeId, new NodeLevelMetrics(metricRegistry, dataNodeName));
    }
  }

  /**
   * Initializes a {@link Gauge} metric for the status of {@code RequestResponseHandlerThread} of an
   * {@link com.github.ambry.router.NonBlockingRouter.OperationController}, to indicate if it is running
   * or not.
   * @param requestResponseHandlerThread The {@code RequestResponseHandlerThread} of which the status is
   *                                     to be monitored.
   */
  public void initializeOperationControllerMetrics(final Thread requestResponseHandlerThread) {
    requestResponseHandlerThreadRunning = new Gauge<Long>() {
      @Override
      public Long getValue() {
        return requestResponseHandlerThread.isAlive() ? 1L : 0L;
      }
    };
    metricRegistry
        .register(MetricRegistry.name(NonBlockingRouter.class, requestResponseHandlerThread.getName() + "Running"),
            requestResponseHandlerThreadRunning);
  }

  /**
   * Initializes a {@link Gauge} metric for the status of {@code ChunkFillerThread} of a {@link PutManager}, to
   * indicate if it is running or not.
   * @param chunkFillerThread The {@code ChunkFillerThread} of which the status is to be monitored.
   */
  public void initializePutManagerMetrics(final Thread chunkFillerThread) {
    chunkFillerThreadRunning = new Gauge<Long>() {
      @Override
      public Long getValue() {
        return chunkFillerThread.isAlive() ? 1L : 0L;
      }
    };
    metricRegistry.register(MetricRegistry.name(PutManager.class, chunkFillerThread.getName() + "Running"),
        chunkFillerThreadRunning);
  }

  /**
   * Initializes a {@link Gauge} metric to monitor the number of running
   * {@link com.github.ambry.router.NonBlockingRouter.OperationController} of a {@link NonBlockingRouter}.
   * @param currentOperationsCount The counter of {@link com.github.ambry.router.NonBlockingRouter.OperationController}.
   */
  public void initializeNumActiveOperationsMetrics(final AtomicInteger currentOperationsCount) {
    activeOperations = new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return currentOperationsCount.get();
      }
    };
    metricRegistry.register(MetricRegistry.name(NonBlockingRouter.class, "NumActiveOperations"), activeOperations);
  }

  /**
   * Count errors based on error type.
   * <p/>
   * This method should be called when an {@code Operation} is completed or aborted.
   * @param exception The exception to be counted.
   */
  void countError(Exception exception) {
    operationErrorRate.mark();
    if (exception instanceof RouterException) {
      switch (((RouterException) exception).getErrorCode()) {
        case AmbryUnavailable:
          ambryUnavailableErrorCount.inc();
          break;
        case InvalidBlobId:
          invalidBlobIdErrorCount.inc();
          break;
        case InvalidPutArgument:
          invalidPutArgumentErrorCount.inc();
          break;
        case OperationTimedOut:
          operationTimedOutErrorCount.inc();
          break;
        case RouterClosed:
          routerClosedErrorCount.inc();
          break;
        case UnexpectedInternalError:
          unexpectedInternalErrorCount.inc();
          break;
        case BlobTooLarge:
          blobTooLargeErrorCount.inc();
          break;
        case BadInputChannel:
          badInputChannelErrorCount.inc();
          break;
        case InsufficientCapacity:
          insufficientCapacityErrorCount.inc();
          break;
        case BlobDeleted:
          blobDeletedErrorCount.inc();
          break;
        case BlobDoesNotExist:
          blobDoesNotExistErrorCount.inc();
          break;
        case BlobExpired:
          blobExpiredErrorCount.inc();
          break;
        default:
          unknownErrorCountForOperation.inc();
          break;
      }
    } else {
      unknownErrorCountForOperation.inc();
    }
  }

  /**
   * Get {@link NodeLevelMetrics} for a given {@link DataNodeId}. The construction of {@code dataNodeToMetrics}
   * and any {@link DataNodeId} as a key passed to this method are all based on the {@link ClusterMap}, and the
   * key should always exist. If the {@link DataNodeId} as the key does not exist, this will be a programming
   * error.
   * @param dataNodeId The {@link DataNodeId} to be indexed.
   * @return The {@link NodeLevelMetrics}.
   */
  NodeLevelMetrics getDataNodeBasedMetrics(DataNodeId dataNodeId) {
    return dataNodeToMetrics.get(dataNodeId);
  }

  /**
   * A metrics class that tracks at the {@link DataNodeId} level. These metrics are collected based on the operation
   * requests sent to individual {@link DataNodeId}. An operation request is part of an operation, and conveys an actual
   * request to a {@link com.github.ambry.clustermap.ReplicaId} in a {@link DataNodeId}. An operation request can be
   * either for a metadata blob, or for a datachunk.
   */
  public class NodeLevelMetrics {

    // Request rate. For each operation type, this metrics tracks the request rate from the NonBlockingRouter to the
    // remote data node.
    public final Meter putRequestRate;
    public final Meter getBlobInfoRequestRate;
    public final Meter getRequestRate;
    public final Meter deleteRequestRate;

    // Request latency. For each operation type, this metrics tracks the round-trip time between the NonBlockingRouter
    // and the remote data node.
    public final Histogram putRequestLatencyMs;
    public final Histogram getBlobInfoRequestLatencyMs;
    public final Histogram getRequestLatencyMs;
    public final Histogram deleteRequestLatencyMs;

    // Request error count. For each operation type, this metrics tracks the total error count seen by the
    // NonBlockingRouter for the remote data node.
    public final Counter putRequestErrorCount;
    public final Counter getBlobInfoRequestErrorCount;
    public final Counter getRequestErrorCount;
    public final Counter deleteRequestErrorCount;

    NodeLevelMetrics(MetricRegistry registry, String dataNodeName) {
      // Request rate.
      putRequestRate = registry.meter(MetricRegistry.name(PutOperation.class, dataNodeName, "PutRequestRate"));
      getBlobInfoRequestRate =
          registry.meter(MetricRegistry.name(GetBlobInfoOperation.class, dataNodeName, "GetBlobInfoRequestRate"));
      getRequestRate = registry.meter(MetricRegistry.name(GetBlobOperation.class, dataNodeName, "GetRequestRate"));
      deleteRequestRate = registry.meter(MetricRegistry.name(DeleteOperation.class, dataNodeName, "DeleteRequestRate"));

      // Request latency.
      putRequestLatencyMs =
          registry.histogram(MetricRegistry.name(PutOperation.class, dataNodeName, "PutRequestLatencyMs"));
      getBlobInfoRequestLatencyMs = registry
          .histogram(MetricRegistry.name(GetBlobInfoOperation.class, dataNodeName, "GetBlobInfoRequestLatencyMs"));
      getRequestLatencyMs =
          registry.histogram(MetricRegistry.name(GetBlobOperation.class, dataNodeName, "GetRequestLatencyMs"));
      deleteRequestLatencyMs =
          registry.histogram(MetricRegistry.name(DeleteOperation.class, dataNodeName, "DeleteRequestLatencyMs"));

      // Request error count.
      putRequestErrorCount =
          registry.counter(MetricRegistry.name(PutOperation.class, dataNodeName, "PutRequestErrorCount"));
      getBlobInfoRequestErrorCount = registry
          .counter(MetricRegistry.name(GetBlobInfoOperation.class, dataNodeName, "GetBlobInfoRequestErrorCount"));
      getRequestErrorCount =
          registry.counter(MetricRegistry.name(GetBlobOperation.class, dataNodeName, "GetRequestErrorCount"));
      deleteRequestErrorCount =
          registry.counter(MetricRegistry.name(DeleteOperation.class, dataNodeName, "DeleteRequestErrorCount"));
    }
  }
}
