package org.neo4j.graphalgo.core.huge;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.graphalgo.api.HugeGraph;
import org.neo4j.graphalgo.api.HugeRelationshipIntersect;
import org.neo4j.graphalgo.core.GraphLoader;
import org.neo4j.internal.kernel.api.TokenWrite;
import org.neo4j.internal.kernel.api.Write;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.Statement;
import org.neo4j.internal.kernel.api.exceptions.KernelException;
import org.neo4j.test.rule.ImpermanentDatabaseRule;

import java.util.Arrays;
import java.util.PrimitiveIterator;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public final class HugeIntersectionTest {

    private static final int DEGREE = 25;
    private static HugeRelationshipIntersect INTERSECT;
    private static long START1;
    private static long START2;
    private static long[] TARGETS;

    @ClassRule
    public static final ImpermanentDatabaseRule DB = new ImpermanentDatabaseRule();

    @BeforeClass
    public static void setup() {
        long[] neoStarts = new long[2];
        long[] neoTargets = DB.executeAndCommit(db -> {
            try (KernelTransaction st = DB.transaction()) {
                TokenWrite token = st.tokenWrite();
                int type = token.relationshipTypeGetOrCreateForName("TYPE");
                Write write = st.dataWrite();
                final Random random = new Random(0L);
                final long start1 = write.nodeCreate();
                final long start2 = write.nodeCreate();
                final long start3 = write.nodeCreate();
                neoStarts[0] = start1;
                neoStarts[1] = start2;
                write.relationshipCreate(start1, type, start2);
                final long[] targets = new long[DEGREE];
                int some = 0;
                for (int i = 0; i < DEGREE; i++) {
                    long target = write.nodeCreate();
                    write.relationshipCreate(start1, type, target);
                    write.relationshipCreate(start3, type, target);
                    if (random.nextBoolean()) {
                        write.relationshipCreate(start2, type, target);
                        targets[some++] = target;
                    }
                }
                st.success();
                return Arrays.copyOf(targets, some);
            } catch (KernelException e) {
                throw new RuntimeException(e);
            }
        });

        final HugeGraph graph = (HugeGraph) new GraphLoader(DB).asUndirected(true).load(HugeGraphFactory.class);
        INTERSECT = graph.intersectionCopy();
        START1 = graph.toHugeMappedNodeId(neoStarts[0]);
        START2 = graph.toHugeMappedNodeId(neoStarts[1]);
        TARGETS = Arrays.stream(neoTargets).map(graph::toHugeMappedNodeId).toArray();
        Arrays.sort(TARGETS);
    }

    @Test
    public void intersectWithTargets() {
        PrimitiveIterator.OfLong targets = Arrays.stream(TARGETS).iterator();
        INTERSECT.intersectAll(START1, (a, b, c) -> {
            assertEquals(START1, a);
            assertEquals(START2, b);
            assertEquals(targets.nextLong(), c);
        });
    }
}
