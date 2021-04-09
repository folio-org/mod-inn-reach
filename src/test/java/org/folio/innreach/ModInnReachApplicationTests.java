package org.folio.innreach;

import org.folio.innreach.domain.entity.ClientKey;
import org.folio.innreach.repository.ClientKeyRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
class ModInnReachApplicationTests {
  @Autowired
  private ClientKeyRepo clientKeyRepo;
//  @Test
	void contextLoads() {
    clientKeyRepo.save(new ClientKey(""+(new Date().getTime()),""+""+(new Date().getTime())));
	}

}
