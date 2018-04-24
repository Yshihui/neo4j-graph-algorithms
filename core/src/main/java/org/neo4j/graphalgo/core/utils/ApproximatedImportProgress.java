package org.neo4j.graphalgo.core.utils;

import org.neo4j.graphalgo.core.utils.paged.AllocationTracker;
import org.neo4j.graphalgo.core.utils.paged.BitUtil;

import java.util.concurrent.atomic.AtomicLong;

public final class ApproximatedImportProgress implements ImportProgress {

    private final ProgressLogger progressLogger;
    private final AllocationTracker tracker;
    private final long nodeCount;
    private final long approxOperations;
    private final long progressMask;
    private final int relationProgressShift;

    private final AtomicLong nodeProgress;

    public ApproximatedImportProgress(
            ProgressLogger progressLogger,
            AllocationTracker tracker,
            long nodeCount,
            long maxRelCount,
            boolean loadIncoming,
            boolean loadOutgoing) {
        this.progressLogger = progressLogger;
        this.tracker = tracker;
        this.nodeCount = nodeCount;
        long relOperations = (loadIncoming ? maxRelCount : 0) + (loadOutgoing ? maxRelCount : 0);
        long relFactor = nodeCount > 0 ? BitUtil.nearbyPowerOfTwo(relOperations / nodeCount) : 0;
        relationProgressShift = Long.numberOfTrailingZeros(relFactor);
        approxOperations = nodeCount + (nodeCount << relationProgressShift);
        progressMask = (BitUtil.nearbyPowerOfTwo(nodeCount) >>> 6) - 1;
        nodeProgress = new AtomicLong();
    }

    @Override
    public void nodeProgress() {
        long nodes = nodeProgress.incrementAndGet();
        if ((nodes & progressMask) == 0) {
            progressLogger.logProgress(
                    nodes,
                    approxOperations,
                    tracker);
        }
    }

    @Override
    public void relProgress() {
        long nodes = nodeProgress.incrementAndGet();
        if ((nodes & progressMask) == 0) {
            progressLogger.logProgress(
                    (nodes << relationProgressShift) + nodeCount,
                    approxOperations,
                    tracker);
        }
    }

    @Override
    public void resetForRelationships() {
        nodeProgress.set(0);
    }
}
