package org.engen.gradle.plugin.security

import groovy.transform.CompileStatic

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.AlgorithmParameters
import java.security.SecureRandom
import java.security.spec.KeySpec

@CompileStatic
class CryptoUtil {

    SecretKey createKey(String password) {
        /* Derive the key, given password and salt. */
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        SecureRandom salt = new SecureRandom()
        byte[] bytes = new byte[8]
        salt.nextBytes(bytes)
        KeySpec spec = new PBEKeySpec(password.chars, salt.generateSeed(8), 65536, 128)
        SecretKey tmp = factory.generateSecret(spec)
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES")
        return secret
    }

    CriptHolder encript(String password) {
        /* Encrypt the message. */
        SecretKey key = createKey(password)
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        AlgorithmParameters params = cipher.getParameters()
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV()
        byte[] ciphertext = cipher.doFinal(password.getBytes("UTF-8"))
        CriptHolder holder = new CriptHolder()
        holder.iv = iv
        holder.ciphertext = ciphertext
        holder.key = key.getEncoded()
        return holder
    }

    String decript(CriptHolder holder) {
        /* Decrypt the message, given derived key and initialization vector. */
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        SecretKey secret = new SecretKeySpec(holder.key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(holder.iv))
        return new String(cipher.doFinal(holder.ciphertext), "UTF-8")
    }

    static void main(String[] args) {
        /*CryptoUtil util = new CryptoUtil()
        CriptHolder holder = util.encript("minha_senha")
        holder.save("tmp/61603945504-secure")
        println "EncodedKey: " + holder.key
        println "IV: " + holder.iv
        println "CipherText: " + holder.ciphertext
        CriptHolder loaded = CriptHolder.load("tmp/61603945504-secure")
        println "Senha: " + util.decript(loaded)
        */
        savePassword("user1", "senha1")
        String pwd = loadPassword("user1")
        println "Password recovered: $pwd"
    }

    static void savePassword(String username, String password) {
        CryptoUtil util = new CryptoUtil()
        CriptHolder holder = util.encript(password)
        holder.save(CriptHolder.extensionUserFileName(username))
    }

    static String loadPassword(String username) {
        def util = new CryptoUtil()
        CriptHolder holder = CriptHolder.load(CriptHolder.extensionUserFileName(username))
        String pwd = util.decript(holder)
        return pwd
    }

}

class CriptHolder {
    byte[] iv
    byte[] ciphertext
    byte[] key

    void save(String fileName) {
        def ivBase64 = new String(Base64.encoder.encode(iv), "UTF-8")
        def ciphertextBase64 = new String(Base64.encoder.encode(ciphertext), "UTF-8")
        def keyBase64 = new String(Base64.encoder.encode(key), "UTF-8")
        String vars = "$ivBase64,$ciphertextBase64,$keyBase64"
        new File(fileName).write(vars, "UTF-8")
    }

    static CriptHolder load(String fileName) {
        if (!new File(fileName).exists()) {
            throw new IllegalArgumentException("The $fileName does not exist! Please, use secure password task for the server!")
        }
        def vars = new File(fileName).text.split(",")
        println "Vars: $vars"
        CriptHolder holder = new CriptHolder()
        holder.iv = Base64.decoder.decode(vars[0])
        holder.ciphertext = Base64.decoder.decode(vars[1])
        holder.key = Base64.decoder.decode(vars[2])
        return holder
    }

    static String extensionUserFileName(String username) {
        return "$username-secure"
    }
}