package cn.edu.buaa.crypto.encryption.hibbe.llw16b.generators;

import cn.edu.buaa.crypto.algebra.generators.AsymmetricKeySerParametersGenerator;
import cn.edu.buaa.crypto.algebra.serparams.AsymmetricKeySerParameter;
import cn.edu.buaa.crypto.encryption.hibbe.llw16b.genparams.HIBBELLW16bDelegateGenerationParameter;
import cn.edu.buaa.crypto.encryption.hibbe.llw16b.genparams.HIBBELLW16bSecretKeyGenerationParameter;
import cn.edu.buaa.crypto.encryption.hibbe.llw16b.serparams.HIBBELLW16bMasterSecretKeySerParameter;
import cn.edu.buaa.crypto.encryption.hibbe.llw16b.serparams.HIBBELLW16bPublicKeySerParameter;
import cn.edu.buaa.crypto.encryption.hibbe.llw16b.serparams.HIBBELLW16bSecretKeySerParameter;
import cn.edu.buaa.crypto.utils.PairingUtils;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.bouncycastle.crypto.KeyGenerationParameters;

/**
 * Created by Weiran Liu on 2016/11/10.
 *
 * Liu-Liu-Wu prime-order CCA2-secure HIBBE secret key generator.
 */
public class HIBBELLW16bSecretKeyGenerator  implements AsymmetricKeySerParametersGenerator {
    private KeyGenerationParameters params;

    public void init(KeyGenerationParameters keyGenerationParameters) {
        this.params = keyGenerationParameters;
    }

    public AsymmetricKeySerParameter generateKey() {
        if (params instanceof HIBBELLW16bSecretKeyGenerationParameter) {
            HIBBELLW16bSecretKeyGenerationParameter parameters = (HIBBELLW16bSecretKeyGenerationParameter)params;

            HIBBELLW16bPublicKeySerParameter publicKeyParameters = parameters.getPublicKeyParameters();
            HIBBELLW16bMasterSecretKeySerParameter masterSecretKeyParameters = parameters.getMasterSecretKeyParameters();
            if (parameters.getIds().length != publicKeyParameters.getMaxUser()) {
                throw new IllegalArgumentException("Invalid identity vector length");
            }

            Pairing pairing = PairingFactory.getPairing(publicKeyParameters.getParameters());
            Element[] elementIds = PairingUtils.MapStringArrayToGroup(pairing, parameters.getIds(), PairingUtils.PairingGroupType.Zr);
            Element r = pairing.getZr().newRandomElement().getImmutable();

            Element a1 = publicKeyParameters.getG().powZn(r).getImmutable();
            Element a0 = publicKeyParameters.getG3().getImmutable();
            Element[] bs = new Element[publicKeyParameters.getMaxUser()];
            Element bv = publicKeyParameters.getUv().getImmutable();

            for (int i=0; i<publicKeyParameters.getMaxUser(); i++){
                if (parameters.getIdAt(i) != null) {
                    //Compute a0
                    a0 = a0.mul(publicKeyParameters.getUsAt(i).powZn(elementIds[i])).getImmutable();
                    //Set h[i] to be one
                    bs[i] = pairing.getG1().newOneElement().getImmutable();
                } else {
                    //Set h[i] to be h_i^r
                    bs[i] = publicKeyParameters.getUsAt(i).powZn(r).getImmutable();
                }
            }
            //raise a0 to the power of r and then multiple it by gAlpha
            a0 = a0.powZn(r).mul(masterSecretKeyParameters.getG2Alpha()).getImmutable();
            bv = bv.powZn(r).getImmutable();

            return new HIBBELLW16bSecretKeySerParameter(publicKeyParameters.getParameters(),
                    parameters.getIds(), elementIds, a0, a1, bs, bv);
        } else if (params instanceof HIBBELLW16bDelegateGenerationParameter)  {
            HIBBELLW16bDelegateGenerationParameter parameters = (HIBBELLW16bDelegateGenerationParameter)params;

            HIBBELLW16bPublicKeySerParameter publicKeyParameters = parameters.getPublicKeyParameters();
            HIBBELLW16bSecretKeySerParameter secretKeyParameters = parameters.getSecretKeyParameters();
            if (secretKeyParameters.getIds().length != publicKeyParameters.getMaxUser()
                    || secretKeyParameters.getIds()[parameters.getIndex()] != null) {
                throw new IllegalArgumentException("Invalid identity vector length");
            }

            Pairing pairing = PairingFactory.getPairing(publicKeyParameters.getParameters());
            String[] ids = new String[publicKeyParameters.getMaxUser()];
            Element[] elementIds = new Element[publicKeyParameters.getMaxUser()];
            Element elementDelegateId = PairingUtils.MapStringToGroup(pairing, parameters.getDelegateId(), PairingUtils.PairingGroupType.Zr).getImmutable();

            Element t = pairing.getZr().newRandomElement().getImmutable();
            Element a0 = publicKeyParameters.getG3().getImmutable();
            Element a1 = publicKeyParameters.getG().powZn(t).getImmutable();
            Element[] bs = new Element[publicKeyParameters.getMaxUser()];

            for (int i=0; i<publicKeyParameters.getMaxUser(); i++) {
                if (secretKeyParameters.getIdAt(i) != null) {
                    ids[i] = secretKeyParameters.getIdAt(i);
                    elementIds[i] = secretKeyParameters.getElementIdAt(i);
                    //Compute a0
                    a0 = a0.mul(publicKeyParameters.getUsAt(i).powZn(elementIds[i])).getImmutable();
                    //Set h[i] to be one
                    bs[i] = pairing.getG1().newOneElement().getImmutable();
                } else if (i == parameters.getIndex()) {
                    ids[i] = parameters.getDelegateId();
                    elementIds[i] = elementDelegateId;
                    //Compute a0
                    a0 = a0.mul(publicKeyParameters.getUsAt(i).powZn(elementIds[i])).getImmutable();
                    //Set h[i] to be one
                    bs[i] = pairing.getG1().newOneElement().getImmutable();
                } else {
                    bs[i] = secretKeyParameters.getBsAt(i)
                            .mul(publicKeyParameters.getUsAt(i).powZn(t)).getImmutable();
                }
            }
            //Compute the rest of a0
            a0 = a0.powZn(t).mul(secretKeyParameters.getA0())
                    .mul(secretKeyParameters.getBsAt(parameters.getIndex()).powZn(elementIds[parameters.getIndex()])).getImmutable();
            //Compute the result of a1
            a1 = a1.mul(secretKeyParameters.getA1()).getImmutable();
            Element bv = secretKeyParameters.getBv().mul(publicKeyParameters.getUv().powZn(t)).getImmutable();

            return new HIBBELLW16bSecretKeySerParameter(publicKeyParameters.getParameters(),
                    ids, elementIds, a0, a1, bs, bv);
        } else {
            throw new IllegalArgumentException
                    ("Invalid KeyGenerationParameters for Secret Key Generatation, find "
                            + params.getClass().getName() + ", require "
                            + HIBBELLW16bSecretKeyGenerationParameter.class.getName() + " or "
                            + HIBBELLW16bDelegateGenerationParameter.class.getName());
        }
    }
}

