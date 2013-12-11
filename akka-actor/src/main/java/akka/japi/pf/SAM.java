/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

// TODO:ban #3770 doc missing

public final class SAM {
  private SAM() {
  }

  public static interface Apply<P, R> {
    public abstract R apply(P p);
  }

  public static interface Apply2<P, Q, R> {
    public abstract R apply(P p, Q q);
  }

  public static interface EmptyUnitApply {
    public abstract void apply();
  }

  public static interface Predicate<P> {
    public abstract boolean defined(Object o);
  }

  public static interface TypedPredicate<P> {
    public abstract boolean defined(P p);
  }

  public static interface UnitApply<P> {
    public abstract void apply(P p);
  }

  public static interface UnitApply2<P, Q> {
    public abstract void apply(P p, Q q);
  }

  public static interface UnitApply3<P, Q, R> {
    public abstract void apply(P p, Q q, R r);
  }
}
