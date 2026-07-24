package org.folio.innreach.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.PATRON;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortBy;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortOrder;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters;
import org.folio.innreach.domain.entity.TransactionPatronHold;

@ExtendWith(MockitoExtension.class)
class InnReachTransactionSpecificationTest {

  @Mock
  private CriteriaBuilder cb;
  @Mock
  private CriteriaQuery<Object> cq;
  @Mock
  private Root<InnReachTransaction> root;
  @Mock
  private Join<Object, Object> hold;
  @Mock
  private Join<Object, TransactionPatronHold> patronHoldJoin;
  @Mock
  private Fetch<Object, Object> holdFetch;
  @Mock
  private Path<Object> simplePath;
  @Mock
  private Path<Object> typePath;
  @Mock
  private Path<Object> statePath;
  @Mock
  private Path<Object> centralServerCodePath;
  @Mock
  private Path<Object> agencyCodePath;
  @Mock
  private Path<Object> barcodePath;
  @Mock
  private Expression<String> stringExpr;
  @Mock
  private Predicate conjunction;
  @Mock
  private Predicate predicate;
  @Mock
  private Predicate predicate2;
  @Mock
  private Order order;

  private final InnReachTransactionFilterParameters parameters = new InnReachTransactionFilterParameters();

  @Test
  void isOfType_emptyTypes_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);

    var result = InnReachTransactionSpecification.isOfType(cb, root, parameters);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  void isOfType_withTypes_returnsInPredicate() {
    var types = List.of(InnReachTransaction.TransactionType.PATRON);
    parameters.setTypes(types);
    when(root.get("type")).thenReturn(typePath);
    when(typePath.in(types)).thenReturn(predicate);

    var result = InnReachTransactionSpecification.isOfType(cb, root, parameters);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  void isOfState_emptyStates_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);

    var result = InnReachTransactionSpecification.isOfState(cb, root, parameters);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  void isOfState_withStates_returnsInPredicate() {
    var states = List.of(InnReachTransaction.TransactionState.PATRON_HOLD);
    parameters.setStates(states);
    when(root.get("state")).thenReturn(statePath);
    when(statePath.in(states)).thenReturn(predicate);

    var result = InnReachTransactionSpecification.isOfState(cb, root, parameters);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  void centralCodeIn_emptyCodes_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);

    var result = InnReachTransactionSpecification.centralCodeIn(cb, root, parameters);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  void centralCodeIn_withCodes_returnsInPredicate() {
    var codes = List.of("d2ir");
    parameters.setCentralServerCodes(codes);
    when(root.get("centralServerCode")).thenReturn(centralServerCodePath);
    when(centralServerCodePath.in(codes)).thenReturn(predicate);

    var result = InnReachTransactionSpecification.centralCodeIn(cb, root, parameters);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  void patronAgencyIn_emptyCodes_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);

    var result = InnReachTransactionSpecification.patronAgencyIn(cb, hold, parameters);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  void patronAgencyIn_withCodes_returnsInPredicate() {
    var codes = List.of("qwe12");
    parameters.setPatronAgencyCodes(codes);
    when(hold.get("patronAgencyCode")).thenReturn(agencyCodePath);
    when(agencyCodePath.in(codes)).thenReturn(predicate);

    var result = InnReachTransactionSpecification.patronAgencyIn(cb, hold, parameters);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  void itemAgencyIn_emptyCodes_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);

    var result = InnReachTransactionSpecification.itemAgencyIn(cb, hold, parameters);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  void itemAgencyIn_withCodes_returnsInPredicate() {
    var codes = List.of("asd78");
    parameters.setItemAgencyCodes(codes);
    when(hold.get("itemAgencyCode")).thenReturn(agencyCodePath);
    when(agencyCodePath.in(codes)).thenReturn(predicate);

    var result = InnReachTransactionSpecification.itemAgencyIn(cb, hold, parameters);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  void patronTypeIn_emptyTypes_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);

    var result = InnReachTransactionSpecification.patronTypeIn(cb, hold, parameters);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  void patronTypeIn_withTypes_returnsInPredicate() {
    var types = List.of(1);
    parameters.setPatronTypes(types);
    when(hold.get("centralPatronType")).thenReturn(simplePath);
    when(simplePath.in(types)).thenReturn(predicate);

    var result = InnReachTransactionSpecification.patronTypeIn(cb, hold, parameters);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  void patronNameIn_emptyNames_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);

    var result = InnReachTransactionSpecification.patronNameIn(cb, hold, parameters);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  void patronNameIn_withNames_returnsInPredicate() {
    var names = List.of("patronName1");
    parameters.setPatronNames(names);
    when(hold.get("patronName")).thenReturn(simplePath);
    when(simplePath.in(names)).thenReturn(predicate);

    var result = InnReachTransactionSpecification.patronNameIn(cb, hold, parameters);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  void centralItemTypeIn_emptyTypes_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);

    var result = InnReachTransactionSpecification.centralItemTypeIn(cb, hold, parameters);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  void centralItemTypeIn_withTypes_returnsInPredicate() {
    var types = List.of(1);
    parameters.setCentralItemTypes(types);
    when(hold.get("centralItemType")).thenReturn(simplePath);
    when(simplePath.in(types)).thenReturn(predicate);

    var result = InnReachTransactionSpecification.centralItemTypeIn(cb, hold, parameters);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  void itemBarcodeIn_emptyBarcodes_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);

    var result = InnReachTransactionSpecification.itemBarcodeIn(cb, root, hold, parameters);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  @SuppressWarnings("unchecked")
  void itemBarcodeIn_patronType_usesShippedItemBarcode() {
    var barcodes = List.of("ABC-abc-1234");
    parameters.setItemBarcodes(barcodes);
    when(root.get("type")).thenReturn(typePath);
    when(cb.treat(hold, TransactionPatronHold.class)).thenReturn(patronHoldJoin);
    when(cb.equal(typePath, PATRON)).thenReturn(predicate);
    when(cb.notEqual(typePath, PATRON)).thenReturn(predicate2);
    when(patronHoldJoin.get("shippedItemBarcode")).thenReturn(barcodePath);
    when(hold.get("folioItemBarcode")).thenReturn(barcodePath);
    when(barcodePath.in(barcodes)).thenReturn(predicate);
    when(cb.and(predicate, predicate)).thenReturn(predicate);
    when(cb.and(predicate2, predicate)).thenReturn(predicate);
    when(cb.or(predicate, predicate)).thenReturn(predicate);

    var result = InnReachTransactionSpecification.itemBarcodeIn(cb, root, hold, parameters);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings("unchecked")
  void itemBarcodeIn_nonPatronType_usesFolioItemBarcode() {
    var barcodes = List.of("DEF-def-5678");
    parameters.setItemBarcodes(barcodes);
    when(root.get("type")).thenReturn(typePath);
    when(cb.treat(hold, TransactionPatronHold.class)).thenReturn(patronHoldJoin);
    when(cb.equal(typePath, PATRON)).thenReturn(predicate);
    when(cb.notEqual(typePath, PATRON)).thenReturn(predicate2);
    when(patronHoldJoin.get("shippedItemBarcode")).thenReturn(barcodePath);
    when(hold.get("folioItemBarcode")).thenReturn(barcodePath);
    when(barcodePath.in(barcodes)).thenReturn(predicate);
    when(cb.and(predicate, predicate)).thenReturn(predicate);
    when(cb.and(predicate2, predicate)).thenReturn(predicate);
    when(cb.or(predicate, predicate)).thenReturn(predicate);

    var result = InnReachTransactionSpecification.itemBarcodeIn(cb, root, hold, parameters);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  void keywordLookup_blankKeyword_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);

    var spec = InnReachTransactionSpecification.keywordLookup(null);
    var result = spec.toPredicate(root, cq, cb);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  @SuppressWarnings("unchecked")
  void keywordLookup_withKeyword_buildsOrPredicates() {
    var keyword = "test";
    when(root.join("hold")).thenReturn((Join) hold);
    when(hold.get("itemId")).thenReturn(simplePath);
    when(cb.equal(simplePath, keyword)).thenReturn(predicate);
    when(hold.get("patronId")).thenReturn(simplePath);
    when(cb.equal(simplePath, keyword)).thenReturn(predicate);
    when(root.get("trackingId")).thenReturn(simplePath);
    when(cb.equal(simplePath, keyword)).thenReturn(predicate);
    when(hold.get("folioPatronBarcode")).thenReturn(simplePath);
    when(cb.equal(simplePath, keyword)).thenReturn(predicate);
    when(hold.get("folioItemBarcode")).thenReturn(simplePath);
    when(cb.equal(simplePath, keyword)).thenReturn(predicate);
    when(hold.get("patronName")).thenReturn(simplePath);
    when(cb.lower((Expression) simplePath)).thenReturn(stringExpr);
    when(cb.like(stringExpr, "%test%")).thenReturn(predicate);
    when(hold.get("author")).thenReturn(simplePath);
    when(cb.like(stringExpr, "%test%")).thenReturn(predicate);
    when(hold.get("title")).thenReturn(simplePath);
    when(cb.like(stringExpr, "%test%")).thenReturn(predicate);
    when(cb.or(predicate, predicate, predicate, predicate, predicate, predicate, predicate, predicate))
      .thenReturn(predicate);

    var spec = InnReachTransactionSpecification.keywordLookup(keyword);
    var result = spec.toPredicate(root, cq, cb);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  void fetchHoldAndPickupLocation_countQuery_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);
    when(cq.getResultType()).thenAnswer(invocation -> Long.class);

    var spec = InnReachTransactionSpecification.fetchHoldAndPickupLocation();
    var result = spec.toPredicate(root, cq, cb);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  @SuppressWarnings("unchecked")
  void fetchHoldAndPickupLocation_nonCountQuery_fetchesHoldAndPickup() {
    when(cb.conjunction()).thenReturn(conjunction);
    when(cq.getResultType()).thenReturn(Object.class);
    when(root.fetch("hold")).thenReturn(holdFetch);
    when(holdFetch.fetch("pickupLocation")).thenReturn(null);

    var spec = InnReachTransactionSpecification.fetchHoldAndPickupLocation();
    spec.toPredicate(root, cq, cb);

    verify(root).fetch("hold");
    verify(holdFetch).fetch("pickupLocation");
  }

  @Test
  void sortBy_nullSortBy_returnsConjunction() {
    when(cb.conjunction()).thenReturn(conjunction);

    var spec = InnReachTransactionSpecification.sortBy(null, SortOrder.ASC);
    var result = spec.toPredicate(root, cq, cb);

    assertThat(result).isEqualTo(conjunction);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sortBy_transactionTypeAsc_appliesAscendingOrder() {
    when(root.get("type")).thenReturn(typePath);
    when(cb.asc((Expression) typePath)).thenReturn(order);

    var spec = InnReachTransactionSpecification.sortBy(SortBy.TRANSACTION_TYPE, SortOrder.ASC);
    spec.toPredicate(root, cq, cb);

    verify(cb).asc((Expression) typePath);
    verify(cq).orderBy(order);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sortBy_dateModifiedDesc_appliesDescendingOrder() {
    when(root.get("updatedDate")).thenReturn(simplePath);
    when(cb.desc((Expression) simplePath)).thenReturn(order);

    var spec = InnReachTransactionSpecification.sortBy(SortBy.DATE_MODIFIED, SortOrder.DESC);
    spec.toPredicate(root, cq, cb);

    verify(cb).desc((Expression) simplePath);
    verify(cq).orderBy(order);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sortBy_itemAgency_appliesOrderOnHoldField() {
    when(root.get("hold")).thenReturn((Path) hold);
    when(hold.get("itemAgencyCode")).thenReturn(agencyCodePath);
    when(cb.asc((Expression) agencyCodePath)).thenReturn(order);

    var spec = InnReachTransactionSpecification.sortBy(SortBy.ITEM_AGENCY, SortOrder.ASC);
    spec.toPredicate(root, cq, cb);

    verify(cq).orderBy(order);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sortBy_patronName_appliesOrderOnHoldField() {
    when(root.get("hold")).thenReturn((Path) hold);
    when(hold.get("patronName")).thenReturn(simplePath);
    when(cb.asc((Expression) simplePath)).thenReturn(order);

    var spec = InnReachTransactionSpecification.sortBy(SortBy.PATRON_NAME, SortOrder.ASC);
    spec.toPredicate(root, cq, cb);

    verify(cq).orderBy(order);
  }
}
