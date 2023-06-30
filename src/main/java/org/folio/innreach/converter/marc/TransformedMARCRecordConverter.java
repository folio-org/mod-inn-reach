package org.folio.innreach.converter.marc;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

import static org.folio.innreach.converter.marc.Constants.BLANK_REPLACEMENT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import lombok.extern.log4j.Log4j2;
import org.codehaus.plexus.util.Base64;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.Leader;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;
import org.marc4j.marc.impl.SubfieldImpl;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.dto.folio.sourcerecord.RecordFieldDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.SourceRecordDTO;
import org.folio.innreach.dto.TransformedMARCRecordDTO;

@Log4j2
@Component
public class TransformedMARCRecordConverter {

  private static final int ADDRESS_LENGTH = 12;
  private static final int TAG_LENGTH = 4;
  private static final int TERMINATOR_LENGTH = 1;
  private static final int LEADER_LENGTH = 24;

  private static final Pattern CONTROL_FIELD_PATTERN = Pattern.compile("^(00)[1-9]$");

  private static final MarcFactory MARC_FACTORY = MarcFactory.newInstance();

  public TransformedMARCRecordDTO toTransformedRecord(SourceRecordDTO sourceRecord) {
    var marcRecord = toMARCRecord(sourceRecord);
    var base64RawContent = toBase64RawContent(marcRecord);

    return new TransformedMARCRecordDTO()
      .id(sourceRecord.getId())
      .content(marcRecord.toString())
      .base64rawContent(base64RawContent);
  }

  private Record toMARCRecord(SourceRecordDTO sourceRecord) {
    var parsedRecord = sourceRecord.getParsedRecord();

    var newRecord = MARC_FACTORY.newRecord();
    var leaderString = sourceRecord.getParsedRecord().getLeader();

    parsedRecord.getFields()
      .stream()
      .map(this::toVariableField)
      .forEach(newRecord::addVariableField);

    Leader leader = MARC_FACTORY.newLeader(restoreBlanks(leaderString));
    leader.setRecordLength(calculateRecordLength(newRecord));
    newRecord.setLeader(leader);

    return newRecord;
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

  private int calculateRecordLength(Record newRecord) {
    int addressesLength = newRecord.getVariableFields().size() * ADDRESS_LENGTH;
    int controlFieldsLength = newRecord.getControlFields()
      .stream()
      .mapToInt(controlField -> controlField.getData().length() + TERMINATOR_LENGTH)
      .sum();
    int dataFieldsLength = newRecord.getDataFields()
      .stream()
      .mapToInt(dataField -> dataField.toString().length() - TAG_LENGTH + TERMINATOR_LENGTH)
      .sum();
    return LEADER_LENGTH + addressesLength + controlFieldsLength + dataFieldsLength + TERMINATOR_LENGTH;
  }

  private String restoreBlanks(String sourceString) {
    return sourceString.replace(BLANK_REPLACEMENT, SPACE);
  }

  private String toBase64RawContent(Record marcRecord) {
    MarcStreamWriter marcStreamWriter = null;

    try (var baos = new ByteArrayOutputStream()) {
      marcStreamWriter = new MarcStreamWriter(baos, StandardCharsets.UTF_8.toString());
      marcStreamWriter.write(marcRecord);
      return new String(Base64.encodeBase64(baos.toByteArray()));
    } catch (IOException e) {
      log.error("Can't transform MARC record content to Base64 encoded raw content", e);
    } finally {
      if (marcStreamWriter != null) {
        marcStreamWriter.close();
      }
    }
    return EMPTY;
  }

}
