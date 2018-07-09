package jp.co.soramitsu.sora.didresolver.services;

import java.util.List;
import jp.co.soramitsu.sora.didresolver.dto.Authentication;
import jp.co.soramitsu.sora.didresolver.dto.PublicKey;

public interface ValidateService {

  /**
   * Check that proof is in public keys
   *
   * @param proofCreator field creator of proof element
   * @param publicKeys array of public keys from DDO
   * @return true if public keys contains key with id equals value of creator otherwise false
   */
  boolean isProofInPublicKeys(String proofCreator, List<PublicKey> publicKeys);

  /**
   * Check that Proof field creator is in authentication section
   *
   * @param creator value of field creator of proof section
   * @param authentication array of values of authentication section
   */
  boolean isProofCreatorInAuth(String creator, List<Authentication> authentication);
}
