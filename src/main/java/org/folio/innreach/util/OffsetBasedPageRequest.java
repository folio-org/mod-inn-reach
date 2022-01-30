package org.folio.innreach.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetBasedPageRequest implements Pageable {

  private int limit;
  private int offset;
  private final Sort sort;

  public OffsetBasedPageRequest(int offset, int limit, Sort sort) {
    this.limit = limit;
    this.offset = offset;
    this.sort = sort;
  }

  public OffsetBasedPageRequest(int offset, int limit) {
    this(offset, limit, Sort.unsorted());
  }

  @Override
  public int getPageNumber() {
    return offset / limit;
  }

  @Override
  public int getPageSize() {
    return limit;
  }

  @Override
  public long getOffset() {
    return offset;
  }

  @Override
  public Sort getSort() {
    return sort;
  }

  @Override
  public Pageable next() {
    return new OffsetBasedPageRequest((int)getOffset() + getPageSize(), getPageSize(), getSort());
  }

  public OffsetBasedPageRequest previous() {
    return hasPrevious() ? new OffsetBasedPageRequest((int)getOffset() - getPageSize(), getPageSize(), getSort()) : this;
  }

  @Override
  public Pageable previousOrFirst() {
    return hasPrevious() ? previous() : first();
  }

  @Override
  public Pageable first() {
    return new OffsetBasedPageRequest(0, getPageSize(), getSort());
  }

  @Override
  public Pageable withPage(int i) {
    return null;
  }

  @Override
  public boolean hasPrevious() {
    return offset > limit;
  }
}
