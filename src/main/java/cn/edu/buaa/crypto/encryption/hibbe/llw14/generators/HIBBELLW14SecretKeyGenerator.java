package cn.edu.buaa.crypto.encryption.hibbe.llw14.generators;

import cn.edu.buaa.crypto.algebra.generators.AsymmetricKeySerParametersGenerator;
import cn.edu.buaa.crypto.algebra.serparams.AsymmetricKeySerParameter;
import cn.edu.buaa.crypto.encryption.hibbe.llw14.genparams.HIBBELLW14DelegateGenerationParameter;
import cn.edu.buaa.crypto.encryption.hibbe.llw14.genparams.HIBBELLW14SecretKeyGenerationParameter;
import cn.edu.buaa.crypto.utils.PairingUtils;
import cn.edu.buaa.crypto.encryption.hibbe.llw14.HIBBELLW14Engine;
import cn.edu.buaa.crypto.encryption.hibbe.llw14.serparams.*;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.bouncycastle.crypto.KeyGenerationParameters;

/**
 * Created by Weiran Liu on 2016/5/16.
 *
 * Liu-Liu-Wu composite-order HIBBE secret key generator.
 */
public class HIBBELLW14SecretKeyGenerator implements AsymmetricKeySerParametersGenerator {
    private KeyGenerationParameters params;

    public void init(KeyGenerationParameters keyGenerationParameters) {
        this.params = keyGenerationParameters;
    }

    public AsymmetricKeySerParameter generateKey() {
        if (params instanceof HIBBELLW14SecretKeyGenerationParameter) {
            HIBBELLW14SecretKeyGenerationParameter parameters = (HIBBELLW14SecretKeyGenerationParameter)params;

            HIBBELLW14PublicKeySerParameter publicKeyParameters = parameters.getPublicKeyParameters();
            HIBBELLW14MasterSecretKeySerParameter masterSecretKeyParameters = parameters.getMasterSecretKeyParameters();

            if (parameters.getIds().length != publicKeyParameters.getMaxUser()) {
                throw new IllegalArgumentException("Invalid identity vector length");
            }
            Pairing pairing = PairingFactory.getPairing(publicKeyParameters.getParameters());
            Element[] elementIds = PairingUtils.MapStringArrayToGroup(pairing, parameters.getIds(), PairingUtils.PairingGroupType.Zr);
            Element r = pairing.getZr().newRandomElement().getImmutable();
            Element a0_r = pairing.getZr().newRandomElement().getImmutable();
            Element a1_r = pairing.getZr().newRandomElement().getImmutable();
            Element[] bs_r = new Element[publicKeyParameters.getMaxUser()];

            Element a1 = publicKeyParameters.getG().powZn(r).mul(publicKeyParameters.getX3().powZn(a1_r)).getImmutable();
            Element a0 = publicKeyParameters.getH().getImmutable();
            Element[] bs = new Element[publicKeyParameters.getMaxUser()];

            for (int i=0; i<publicKeyParameters.getMaxUser(); i++){
                if (parameters.getIdAt(i) != null) {
                    //Compute a0
                    a0 = a0.mul(publicKeyParameters.getUsAt(i).powZn(elementIds[i])).getImmutable();
                    //Set h[i] to be one
                    bs[i] = pairing.getG1().newOneElement().getImmutable();
                } else {
                    //Set h[i] to be h_i^r
                    bs_r[i] = pairing.getZr().newRandomElement().getImmutable();
                    bs[i] = publicKeyParameters.getUsAt(i).powZn(r).mul(publicKeyParameters.getX3().powZn(bs_r[i])).getImmutable();
                }
            }
            //raise a0 to the power of r and then multiple it by gAlpha
            a0 = a0.powZn(r).mul(masterSecretKeyParameters.getGAlpha()).mul(publicKeyParameters.getX3().powZn(a0_r)).getImmutable();

            return new HIBBELLW14SecretKeySerParameter(publicKeyParameters.getParameters(),
                    parameters.getIds(), elementIds, a0, a1, bs);
        } else if (params instanceof HIBBELLW14DelegateGenerationParameter)  {
            HIBBELLW14DelegateGenerationParameter parameters = (HIBBELLW14DelegateGenerationParameter)params;

            HIBBELLW14PublicKeySerParameter publicKeyParameters = parameters.getPublicKeyParameters();
            HIBBELLW14SecretKeySerParameter secretKeyParameters = parameters.getSecretKeyParameters();
            if (secretKeyParameters.getIds().length != publicKeyParameters.getMaxUser()
                    || secretKeyParameters.getIds()[parameters.getIndex()] != null) {
                throw new IllegalArgumentException("Invalid identity vector length");
            }

            Pairing pairing = PairingFactory.getPairing(publicKeyParameters.getParameters());
            String[] ids = new String[publicKeyParameters.getMaxUser()];
            Element[] elementIds = new Element[publicKeyParameters.getMaxUser()];
            Element elementDelegateId = PairingUtils.MapStringToGroup(pairing, parameters.getDelegateId(), PairingUtils.PairingGroupType.Zr).getImmutable();

            Element a0_r = pairing.getZr().newRandomElement().getImmutable();
            Element a1_r = pairing.getZr().newRandomElement().getImmutable();
            Element[] bs_r = new Element[publicKeyParameters.getMaxUser()];
            Element t = pairing.getZr().newRandomElement().getImmutable();
            Element a0 = publicKeyParameters.getH().getImmutable();
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
                    bs_r[i] = pairing.getZr().newRandomElement().getImmutable();
                    bs[i] = secretKeyParameters.getBsAt(i)
                            .mul(publicKeyParameters.getUsAt(i).powZn(t)).mul(publicKeyParameters.getX3().powZn(bs_r[i])).getImmutable();
                }
            }
            //Compute the rest of a0
            a0 = a0.powZn(t).mul(secretKeyParameters.getA0())
                    .mul(secretKeyParameters.getBsAt(parameters.getIndex()).powZn(elementIds[parameters.getIndex()]))
                    .mul(publicKeyParameters.getX3().powZn(a0_r)).getImmutable();
            //Compute the result of a1
            a1 = a1.mul(secretKeyParameters.getA1()).mul(publicKeyParameters.getX3().powZn(a1_r)).getImmutable();

            return new HIBBELLW14SecretKeySerParameter(publicKeyParameters.getParameters(),
                    ids, elementIds, a0, a1, bs);
        } else {
            throw new IllegalArgumentException
                    ("Invalid KeyGenerationParameters for " + HIBBELLW14Engine.SCHEME_NAME
                            + " Secret Key Generatation, find "
                            + params.getClass().getName() + ", require "
                            + HIBBELLW14SecretKeyGenerationParameter.class.getName() + " or "
                            + HIBBELLW14DelegateGenerationParameter.class.getName());
        }
    }
}
