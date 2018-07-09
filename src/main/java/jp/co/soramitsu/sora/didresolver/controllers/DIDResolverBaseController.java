package jp.co.soramitsu.sora.didresolver.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import jp.co.soramitsu.sora.crypto.Consts;
import jp.co.soramitsu.sora.didresolver.dto.DDO;
import jp.co.soramitsu.sora.didresolver.dto.Proof;
import jp.co.soramitsu.sora.didresolver.dto.PublicKey;
import jp.co.soramitsu.sora.didresolver.exceptions.BadProofException;
import jp.co.soramitsu.sora.didresolver.exceptions.DIDDuplicateException;
import jp.co.soramitsu.sora.didresolver.exceptions.InvalidProofException;
import jp.co.soramitsu.sora.didresolver.exceptions.UnparseableException;
import jp.co.soramitsu.sora.didresolver.services.CryptoService;
import jp.co.soramitsu.sora.didresolver.services.StorageService;
import jp.co.soramitsu.sora.didresolver.services.ValidateService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(DIDResolverBaseController.PATH)
@Api(value = DIDResolverBaseController.PATH, description = "CRUD operations on DID documents")
@Slf4j
public class DIDResolverBaseController {

  static final String PATH = "/did";
  static final String ID_PARAM = "/{did}";

  protected StorageService storageService;

  private CryptoService cryptoService;

  private ValidateService validateService;

  protected DIDResolverBaseController(StorageService storageService, CryptoService cryptoService,
      ValidateService validateService) {
    this.storageService = storageService;
    this.cryptoService = cryptoService;
    this.validateService = validateService;
  }

  @PostMapping(consumes = {MediaType.APPLICATION_JSON_UTF8_VALUE})
  @ApiOperation("This operation is used to register new DID-DDO pair in Identity System")
  public void createDDO(
      @ApiParam(value = "url encoded DID", required = true) @Validated @RequestBody DDO ddo)
      throws UnparseableException {
    log.info("start execution of method createDDO for DID - {}", ddo.getId());
    verifyDDOProof(ddo);
    val optionalDDO = storageService.read(ddo.getId());
    if (optionalDDO.isPresent()) {
      throw new DIDDuplicateException(ddo.getId());
    }
    log.info("write to storage DDO with DID - {}", ddo.getId());
    storageService.createOrUpdate(ddo.getId(), ddo);
  }

  protected void verifyDDOProof(DDO ddo) throws UnparseableException {
    Proof proof = ddo.getProof().get(0);
    if (!validateService.isProofCreatorInAuth(proof.getCreator(), ddo.getAuthentication())
        || !validateService.isProofInPublicKeys(proof.getCreator(),
        getPublicKeysForCheck(proof.getCreator(), ddo.getId(), ddo.getPublicKey()))) {
      throw new InvalidProofException(ddo.getId());
    }
    Optional<PublicKey> publicKey = cryptoService.getPublicKeyByProof(proof, ddo.getPublicKey());
    if (publicKey.isPresent() && !cryptoService
        .verifyDDOProof(ddo, publicKey.get().getPublicKeyValue())) {
      log.warn("failure verify proof for DDO with DID {}", ddo.getId());
      throw new BadProofException(ddo.getId());
    }
    log.debug("success verify proof for DDO with DID {}", ddo.getId());
  }

  /**
   * Get array of public keys for check proof correctness
   *
   * @param creator value of field creator of proof section
   * @param did valid DID of DDO
   * @param publicKey array of public keys of the current DDO
   * @return array of public keys of the current DDO in case when creator DID equals did, otherwise
   * array of public keys of DDO with did from creator
   */
  private List<PublicKey> getPublicKeysForCheck(@NotBlank String creator, @NotBlank String did,
      @NotNull @Valid List<PublicKey> publicKey) throws UnparseableException {
    String proofCreatorDID = creator.substring(0, creator.indexOf(Consts.DID_URI_DETERMINATOR));
    if (!did.equals(proofCreatorDID)) {
      val proofCreatorDDO = storageService.read(proofCreatorDID);
      if (proofCreatorDDO.isPresent()) {
        return proofCreatorDDO.get().getPublicKey();
      }
    }
    return publicKey;
  }
}
