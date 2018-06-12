package jp.co.soramitsu.sora.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import jp.co.soramitsu.sora.crypto.algorithms.RawSignatureStrategy;
import jp.co.soramitsu.sora.crypto.algorithms.RawSignatureStrategy.SignatureSuiteException;
import jp.co.soramitsu.sora.crypto.algorithms.SignatureSuiteRegistry;
import jp.co.soramitsu.sora.crypto.algorithms.SignatureSuiteRegistry.NoSuchStrategy;
import jp.co.soramitsu.sora.crypto.hash.RawDigestStrategy;
import jp.co.soramitsu.sora.util.bencoder.BencodeMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Crypto {

  @Setter
  @Getter
  private RawDigestStrategy digestStrategy;

  @Getter
  @Setter
  private ObjectMapper mapper;

  public Crypto(@NotNull RawDigestStrategy digestStrategy) {
    this(digestStrategy, new BencodeMapper());
  }

  public Crypto(@NotNull RawDigestStrategy digestStrategy, @NotNull ObjectMapper mapper) {
    this.digestStrategy = digestStrategy;
    this.mapper = mapper;
    log.debug("create new Crypto object with digest strategy - " + digestStrategy);
  }

  private byte[] createVerifyHash(VerifiableJson document, ProofProxy proof)
      throws CreateVerifyHashException {
    log.info("verify hash for document");
    // sanitize inputs
    sanitizeDocument(document);
    sanitizeProof(proof);
    log.debug("encode input into a string");
    // encode input into a string
    try {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      mapper.writeValue(stream, document);
      mapper.writeValue(stream, proof);
      log.debug("calculate digest(document + proof)");
      // calculate digest(document + proof)
      return digestStrategy.digest(stream.toByteArray());
    } catch (IOException e) {
      throw new CreateVerifyHashException(e);
    }

  }

  private void sanitizeDocument(VerifiableJson document) {
    log.debug("sanitize document for verify hash");
    document.setProof(null);
  }

  private void sanitizeProof(ProofProxy proof) {
    log.debug("sanitize document for verify hash");
    // if "created" is empty, then set current time as "created"
    if (proof.getCreated() == null) {
      log.debug("set current time as \"created\"");
      proof.setCreated(Instant.now());
    }

    proof.setSignatureValue(null);
  }

  public void sign(VerifiableJson document, KeyPair keypair, ProofProxy proof)
      throws CreateVerifyHashException, SignatureSuiteException, NoSuchStrategy {
    log.info("sign document");
    RawSignatureStrategy signer = SignatureSuiteRegistry.INSTANCE.get(proof.getType());
    log.debug("recieved signature strategy for sign document - " + signer);
    // backup proofs
    List<ProofProxy> proofs = document.getProof();

    // hash and sign input
    byte[] hash = createVerifyHash(document, proof);
    byte[] signature = signer.rawSign(hash, keypair);
    log.debug("include signature into a proof, add it to saved proofs");
    // include signature into a proof, add it to saved proofs
    proof.setSignatureValue(signature);
    if (proofs == null) {
      proofs = new ArrayList<>();
    }
    proofs.add(proof);
    log.debug("add all proofs to the document");
    // add all proofs to the document
    document.setProof(proofs);
  }

  /**
   * Verify specific proof
   *
   * @return true if proof is valid, false otherwise
   */
  public boolean verify(VerifiableJson document, PublicKey publicKey, ProofProxy proof)
      throws CreateVerifyHashException, SignatureSuiteException, NoSuchStrategy {
    log.info("verify specific proof");
    RawSignatureStrategy verifier = SignatureSuiteRegistry.INSTANCE.get(proof.getType());
    log.debug("recieved verify strategy for document - " + verifier);
    byte[] hash = createVerifyHash(document, proof);
    byte[] signature = proof.getSignatureValue();

    return verifier.rawVerify(hash, signature, publicKey);
  }

  /**
   * Verify all proofs
   *
   * @return true if all proofs are valid, false otherwise
   */
  public boolean verifyAll(VerifiableJson document, PublicKey publicKey)
      throws CreateVerifyHashException, SignatureSuiteException, NoSuchStrategy {
    log.info("verify all proofs of document");
    final List<ProofProxy> proofs = document.getProof();
    if (proofs == null || proofs.isEmpty()) {
      log.error("document has no verifiable proofs");
      throw new SignatureSuiteException("document has no verifiable proofs");
    }

    for (ProofProxy proof : proofs) {
      if (!verify(document, publicKey, proof)) {
        return false;
      }
    }

    return true;
  }


  public static class CreateVerifyHashException extends IOException {

    CreateVerifyHashException(IOException e) {
      super(e);
    }
  }
}
