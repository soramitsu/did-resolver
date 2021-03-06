package jp.co.soramitsu.sora.didresolver.services;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import jp.co.soramitsu.sora.didresolver.exceptions.DDOUnparseableException;
import jp.co.soramitsu.sora.sdk.did.model.dto.DDO;

public interface StorageService {

  void createOrUpdate(String did, Object ddo);

  Optional<JsonNode> findDDObyDID(String did) throws DDOUnparseableException;

  /**
   * Delete DDO by DID
   *
   * @param did - valid DID
   */
  void delete(String did);
}
