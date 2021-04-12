package org.folio.innreach;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ModInnReachApplicationTests {
<<<<<<< HEAD
  @Autowired
  private ClientKeyRepo clientKeyRepo;
//  @Test
	void contextLoads() {
    clientKeyRepo.save(new ClientKey(""+(new Date().getTime()),""+""+(new Date().getTime())));
	}

=======
>>>>>>> experimental
}
