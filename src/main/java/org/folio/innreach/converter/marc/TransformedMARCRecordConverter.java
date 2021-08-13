package org.folio.innreach.converter.marc;

import static org.apache.commons.lang3.StringUtils.SPACE;

import static org.folio.innreach.converter.marc.Constants.BLANK_REPLACEMENT;

import java.util.regex.Pattern;

import org.marc4j.marc.Leader;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;
import org.marc4j.marc.impl.SubfieldImpl;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.dto.folio.sourcerecord.RecordFieldDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.SourceRecordDTO;
import org.folio.innreach.dto.TransformedMARCRecordDTO;

@Component
public class TransformedMARCRecordConverter {

  private static final int ADDRESS_LENGTH = 12;
  private static final int TAG_LENGTH = 4;
  private static final int TERMINATOR_LENGTH = 1;
  private static final int LEADER_LENGTH = 24;

  private static final Pattern CONTROL_FIELD_PATTERN = Pattern.compile("^(00)[1-9]$");

  private static final MarcFactory MARC_FACTORY = MarcFactory.newInstance();

  public TransformedMARCRecordDTO toTransformedRecord(SourceRecordDTO sourceRecord) {
    return new TransformedMARCRecordDTO()
      .id(sourceRecord.getId())
      .content(toMARCRecord(sourceRecord).toString());
  }

  private Record toMARCRecord(SourceRecordDTO sourceRecord) {
    var parsedRecord = sourceRecord.getParsedRecord();

    var record = MARC_FACTORY.newRecord();
    var leaderString = sourceRecord.getParsedRecord().getLeader();

    parsedRecord.getFields()
      .stream()
      .map(this::toVariableField)
      .forEach(record::addVariableField);

    Leader leader = MARC_FACTORY.newLeader(restoreBlanks(leaderString));
    leader.setRecordLength(calculateRecordLength(record));
    record.setLeader(leader);

    return record;
  }

  private VariableField toVariableField(RecordFieldDTO recordField) {
    if (isControlField(recordField)) {
      return MARC_FACTORY.newControlField(recordField.getCode(), recordField.getValue());
    }
    var dataField = MARC_FACTORY.newDataField(recordField.getCode(), recordField.getInd1(), recordField.getInd2());

    recordField.getSubFields().forEach(subField ->
      dataField.addSubfield(new SubfieldImpl(subField.getCode(), subField.getValue()))
    );

    return dataField;
  }

  private boolean isControlField(RecordFieldDTO recordField) {
    return CONTROL_FIELD_PATTERN.matcher(recordField.getCode()).matches();
  }

  private int calculateRecordLength(Record record) {
    int addressesLength = record.getVariableFields().size() * ADDRESS_LENGTH;
    int controlFieldsLength = record.getControlFields()
      .stream()
      .mapToInt(controlField -> controlField.getData().length() + TERMINATOR_LENGTH)
      .sum();
    int dataFieldsLength = record.getDataFields()
      .stream()
      .mapToInt(dataField -> dataField.toString().length() - TAG_LENGTH + TERMINATOR_LENGTH)
      .sum();
    return LEADER_LENGTH + addressesLength + controlFieldsLength + dataFieldsLength + TERMINATOR_LENGTH;
  }

  private String restoreBlanks(String sourceString) {
    return sourceString.replace(BLANK_REPLACEMENT, SPACE);
  }

}
