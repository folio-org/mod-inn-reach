package org.folio.innreach.domain.service.impl;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.CharUtils;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InstanceTransformationService;
import org.folio.innreach.domain.service.MARCRecordTransformationService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.dto.Instance;

@RequiredArgsConstructor
@Log4j2
@Service
public class InstanceTransformationServiceImpl implements InstanceTransformationService {

  private static final String MARC_BIB_FORMAT = "ISO2709";

  private final MARCRecordTransformationService marcService;
  private final ContributionValidationService validationService;

  @Override
  public BibInfo getBibInfo(UUID centralServerId, Instance instance) {
    var bibId = instance.getHrid();

    var suppressionStatus = validationService.getSuppressionStatus(centralServerId, instance.getStatisticalCodeIds());
    var marc = marcService.transformRecord(centralServerId, instance);

    var bibInfo = new BibInfo();
    bibInfo.setBibId(bibId);

    bibInfo.setSuppress(CharUtils.toString(suppressionStatus));
    bibInfo.setMarc21BibFormat(MARC_BIB_FORMAT);
    bibInfo.setMarc21BibData(marc.getBase64rawContent());
    bibInfo.setTitleHoldCount(0);
    bibInfo.setItemCount(0);
    return bibInfo;
  }

}
