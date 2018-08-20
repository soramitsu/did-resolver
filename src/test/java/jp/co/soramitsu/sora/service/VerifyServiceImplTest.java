package jp.co.soramitsu.sora.service;

import static jp.co.soramitsu.sora.util.DataProvider.ID_BASE;
import static jp.co.soramitsu.sora.util.DataProvider.KEYS_COUNT;
import static jp.co.soramitsu.sora.util.DataProvider.KEY_VALUE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import jp.co.soramitsu.sora.didresolver.dto.DDO;
import jp.co.soramitsu.sora.didresolver.dto.Proof;
import jp.co.soramitsu.sora.didresolver.dto.PublicKey;
import jp.co.soramitsu.sora.didresolver.services.VerifyService;
import jp.co.soramitsu.sora.didresolver.services.impl.VerifyServiceImpl;
import jp.co.soramitsu.sora.util.DataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class VerifyServiceImplTest {

  private static final String DDO_JSON_NAME = "canonicalDDO.json";

  @Autowired
  private VerifyService verifyService;

  private DataProvider dataProvider = new DataProvider();

  @TestConfiguration
  static class CryptoServiceImplTestContextConfiguration {

    @Bean
    public VerifyService cryptoService() {
      return new VerifyServiceImpl();
    }
  }

  @Test
  public void testSuccessGetPublicKeyByProof() {
    Optional<PublicKey> publicKey = verifyService
        .getPublicKeyByProof(dataProvider.getProofForTest(), dataProvider.getPublicKeysForTest());
    assertTrue(publicKey.isPresent());
  }

  @Test
  public void testFailedGetPublicKeyByProof() {
    Proof proof = dataProvider.getProofForTest();
    proof.setCreator(URI.create(ID_BASE + KEYS_COUNT + 2));
    List<PublicKey> publicKeys = dataProvider.getPublicKeysForTest();
    Optional<PublicKey> publicKey = verifyService
        .getPublicKeyByProof(proof, publicKeys);
    assertFalse(publicKey.isPresent());
  }

  @Test
  public void testSuccessVerifyDDOProof() throws IOException {
    DDO ddo = dataProvider.getDDOFromJson(DDO_JSON_NAME);
    assertFalse(verifyService.verifyDDOProof(ddo, KEY_VALUE));
    assertTrue(verifyService.verifyDDOProof(ddo, ddo.getPublicKey().get(0).getPublicKeyValue()));
  }

  @Test
  public void testFailedVerifyDDOProof() throws IOException {
    DDO ddo = dataProvider.getDDOFromJson(DDO_JSON_NAME);
    ddo.setCreated(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    assertFalse(verifyService.verifyDDOProof(ddo, KEY_VALUE));
  }
}
