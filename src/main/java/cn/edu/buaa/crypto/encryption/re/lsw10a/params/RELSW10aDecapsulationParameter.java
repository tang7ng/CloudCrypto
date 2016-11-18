package cn.edu.buaa.crypto.encryption.re.lsw10a.params;

import cn.edu.buaa.crypto.encryption.re.lsw10a.RELSW10aEngine;
import cn.edu.buaa.crypto.encryption.re.lsw10a.serparams.RELSW10aCipherSerParameter;
import cn.edu.buaa.crypto.encryption.re.lsw10a.serparams.RELSW10aPublicKeySerParameter;
import cn.edu.buaa.crypto.encryption.re.lsw10a.serparams.RELSW10aSecretKeySerParameter;
import org.bouncycastle.crypto.CipherParameters;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Weiran Liu on 2016/4/4.
 *
 * Lewko-Waters revocation encryption decapsulation parameter.
 */
public class RELSW10aDecapsulationParameter implements CipherParameters {
    private RELSW10aPublicKeySerParameter publicKeyParameters;
    private RELSW10aSecretKeySerParameter secretKeyParameters;
    private String[] ids;
    private RELSW10aCipherSerParameter ciphertextParameters;

    public RELSW10aDecapsulationParameter(CipherParameters publicKeyParameters,
                                          CipherParameters secretKeyParameters,
                                          String[] ids,
                                          CipherParameters ciphertextParameters) {
        this.publicKeyParameters = (RELSW10aPublicKeySerParameter)publicKeyParameters;
        this.secretKeyParameters = (RELSW10aSecretKeySerParameter)secretKeyParameters;
        //remove repeated ids
        Set<String> idSet = new HashSet<String>();
        Collections.addAll(idSet, ids);
        this.ids = idSet.toArray(new String[1]);

        this.ciphertextParameters = (RELSW10aCipherSerParameter)ciphertextParameters;
        if (this.ciphertextParameters.getLength() != this.ids.length) {
            throw new IllegalArgumentException
                    ("Length of " + RELSW10aEngine.SCHEME_NAME
                            + " Ciphertext and Identity Vector Mismatch, Ciphertext Length = "
                            + this.ciphertextParameters.getLength() + ", Identity Vector Length = "
                            + ids.length);
        }
    }

    public RELSW10aPublicKeySerParameter getPublicKeyParameters() { return this.publicKeyParameters; }

    public RELSW10aSecretKeySerParameter getSecretKeyParameters() { return this.secretKeyParameters; }

    public RELSW10aCipherSerParameter getCiphertextParameters() { return this.ciphertextParameters; }

    public int getLength() { return this.ids.length; }

    public String[] getIds() { return this.ids; }

    public String getIdsAt(int index) { return this.ids[index]; }
}
