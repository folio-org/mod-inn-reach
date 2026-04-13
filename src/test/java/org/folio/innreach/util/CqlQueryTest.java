package org.folio.innreach.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CqlQueryTest {

  @Test
  void exactMatch_producesCorrectQuery() {
    var query = CqlQuery.exactMatch("title", "hello");

    assertEquals("title==\"hello\"", query.getQuery());
  }

  @Test
  void exactMatch_encodesSpecialCharacters() {
    var query = CqlQuery.exactMatch("title", "hello*world");

    assertEquals("title==\"hello\\*world\"", query.getQuery());
  }

  @Test
  void exactMatchById_producesQueryOnIdField() {
    var query = CqlQuery.exactMatchById("abc-123");

    assertEquals("id==\"abc-123\"", query.getQuery());
  }

  @Test
  void exactMatchByName_producesQueryOnNameField() {
    var query = CqlQuery.exactMatchByName("Test Name");

    assertEquals("name==\"Test Name\"", query.getQuery());
  }

  @Test
  void exactMatchByCode_producesQueryOnCodeField() {
    var query = CqlQuery.exactMatchByCode("CODE1");

    assertEquals("code==\"CODE1\"", query.getQuery());
  }

  @Test
  void exactMatchByNameAndCode_combinesNameAndCodeWithAnd() {
    var query = CqlQuery.exactMatchByNameAndCode("Test", "CODE1");

    assertEquals("name==\"Test\" and code==\"CODE1\"", query.getQuery());
  }

  @Test
  void and_wrapsQueriesInParentheses() {
    var left = CqlQuery.exactMatch("field1", "val1");
    var right = CqlQuery.exactMatch("field2", "val2");

    var combined = left.and(right);

    assertEquals("(field1==\"val1\") and (field2==\"val2\")", combined.getQuery());
  }

  @Test
  void and_withSimplifiedJoin_omitsParentheses() {
    var left = CqlQuery.exactMatch("field1", "val1");
    var right = CqlQuery.exactMatch("field2", "val2");

    var combined = left.and(right, true);

    assertEquals("field1==\"val1\" and field2==\"val2\"", combined.getQuery());
  }

  @Test
  void or_wrapsQueriesInParentheses() {
    var left = CqlQuery.exactMatch("field1", "val1");
    var right = CqlQuery.exactMatch("field2", "val2");

    var combined = left.or(right);

    assertEquals("(field1==\"val1\") or (field2==\"val2\")", combined.getQuery());
  }

  @Test
  void or_withSimplifiedJoin_omitsParentheses() {
    var left = CqlQuery.exactMatch("field1", "val1");
    var right = CqlQuery.exactMatch("field2", "val2");

    var combined = left.or(right, true);

    assertEquals("field1==\"val1\" or field2==\"val2\"", combined.getQuery());
  }

  @Test
  void chainingAndThenOr_producesNestedQuery() {
    var q1 = CqlQuery.exactMatch("a", "1");
    var q2 = CqlQuery.exactMatch("b", "2");
    var q3 = CqlQuery.exactMatch("c", "3");

    var combined = q1.and(q2).or(q3);

    assertEquals("((a==\"1\") and (b==\"2\")) or (c==\"3\")", combined.getQuery());
  }

  @Test
  void exactMatchAny_joinsMultipleValuesWithOr() {
    var query = CqlQuery.exactMatchAny("status", List.of("active", "pending"));

    assertEquals("status==(\"active\" or \"pending\")", query.getQuery());
  }

  @Test
  void exactMatchAny_singleValue_producesQueryWithOneValue() {
    var query = CqlQuery.exactMatchAny("status", List.of("active"));

    assertEquals("status==(\"active\")", query.getQuery());
  }

  @Test
  void exactMatchAny_emptyList_producesEmptyValues() {
    var query = CqlQuery.exactMatchAny("status", List.of());

    assertEquals("status==()", query.getQuery());
  }

  @Test
  void exactMatchAny_nullList_producesEmptyValues() {
    var query = CqlQuery.exactMatchAny("status", null);

    assertEquals("status==()", query.getQuery());
  }

  @Test
  void exactMatchAny_filtersOutBlankValues() {
    var query = CqlQuery.exactMatchAny("status", List.of("active", "", "  ", "pending"));

    assertEquals("status==(\"active\" or \"pending\")", query.getQuery());
  }

  @Test
  void exactMatch_escapesQuotesInValue() {
    var query = CqlQuery.exactMatch("title", "say \"hello\"");

    assertEquals("title==\"say \\\"hello\\\"\"", query.getQuery());
  }

  @Test
  void exactMatch_escapesBackslashInValue() {
    var query = CqlQuery.exactMatch("path", "a\\b");

    assertEquals("path==\"a\\\\b\"", query.getQuery());
  }

  @Test
  void exactMatch_escapesCaretInValue() {
    var query = CqlQuery.exactMatch("field", "a^b");

    assertEquals("field==\"a\\^b\"", query.getQuery());
  }

  @Test
  void exactMatch_escapesQuestionMarkInValue() {
    var query = CqlQuery.exactMatch("field", "is it?");

    assertEquals("field==\"is it\\?\"", query.getQuery());
  }

  @Test
  void chainingOrThenAnd_producesNestedQuery() {
    var q1 = CqlQuery.exactMatch("a", "1");
    var q2 = CqlQuery.exactMatch("b", "2");
    var q3 = CqlQuery.exactMatch("c", "3");

    var combined = q1.or(q2).and(q3);

    assertEquals("((a==\"1\") or (b==\"2\")) and (c==\"3\")", combined.getQuery());
  }

  @Test
  void exactMatchAny_encodesSpecialCharactersInValues() {
    var query = CqlQuery.exactMatchAny("title", List.of("hello*", "world?"));

    assertEquals("title==(\"hello\\*\" or \"world\\?\")", query.getQuery());
  }

  @Test
  void exactMatchByNameAndCode_encodesSpecialCharacters() {
    var query = CqlQuery.exactMatchByNameAndCode("Test*Name", "CODE?1");

    assertEquals("name==\"Test\\*Name\" and code==\"CODE\\?1\"", query.getQuery());
  }
}


