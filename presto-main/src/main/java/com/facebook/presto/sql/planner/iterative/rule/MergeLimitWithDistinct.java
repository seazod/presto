/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package com.facebook.presto.sql.planner.iterative.rule;

import com.facebook.presto.sql.planner.PlanNodeIdAllocator;
import com.facebook.presto.sql.planner.SymbolAllocator;
import com.facebook.presto.sql.planner.iterative.Lookup;
import com.facebook.presto.sql.planner.iterative.Rule;
import com.facebook.presto.sql.planner.plan.AggregationNode;
import com.facebook.presto.sql.planner.plan.DistinctLimitNode;
import com.facebook.presto.sql.planner.plan.LimitNode;
import com.facebook.presto.sql.planner.plan.PlanNode;

import java.util.Optional;

public class MergeLimitWithDistinct
    implements Rule
{
    @Override
    public Optional<PlanNode> apply(PlanNode node, Lookup lookup, PlanNodeIdAllocator idAllocator, SymbolAllocator symbolAllocator)
    {
        if (!(node instanceof LimitNode)) {
            return Optional.empty();
        }

        LimitNode parent = (LimitNode) node;

        PlanNode input = lookup.resolve(parent.getSource());
        if (!(input instanceof AggregationNode)) {
            return Optional.empty();
        }

        AggregationNode child = (AggregationNode) input;

        if (isDistinct(child)) {
            return Optional.empty();
        }

        return Optional.of(
                new DistinctLimitNode(
                        parent.getId(),
                        child.getSource(),
                        parent.getCount(),
                        false,
                        child.getHashSymbol()));
    }

    /**
     * Whether this node corresponds to a DISTINCT operation in SQL
     */
    private boolean isDistinct(AggregationNode node)
    {
        return !node.getAggregations().isEmpty() ||
                node.getOutputSymbols().size() != node.getGroupingKeys().size() ||
                !node.getOutputSymbols().containsAll(node.getGroupingKeys());
    }
}
