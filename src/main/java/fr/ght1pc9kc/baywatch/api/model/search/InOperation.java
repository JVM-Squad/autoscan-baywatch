package fr.ght1pc9kc.baywatch.api.model.search;

import lombok.EqualsAndHashCode;

import java.util.Collection;

@lombok.Value
@EqualsAndHashCode(callSuper = true)
public class InOperation<T> extends BiOperand<Collection<T>> {
    public InOperation(CriterionProperty criteria, CriterionValue<Collection<T>> value) {
        super(criteria, value);
    }

    @Override
    public <R> R visit(Visitor<R> visitor) {
        return visitor.visitIn(this);
    }
}
