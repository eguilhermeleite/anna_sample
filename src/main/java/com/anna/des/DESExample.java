package com.anna.des;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/DESExample")
public class DESExample extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public DESExample() {
		super();
	}

	@PostMapping
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			// Chave de desencriptação do websevice cadastrado no AnnA
			String decKey = "NV2M5TnBxtHznZiBF85yNEP1FbnPPqvD"; // Deve ser alterada conforme o cadastro do Web Service

			String encKey = "lgmsTAiDqINHDQgu58gM2d3AKpPwV/tM"; // Deve ser alterada conforme o cadastro do Web Service

			// Ler a vari�vel de POST "ANNAEXEC" que contém o "IV Recebido" sem encriptação
			String ivRecebido = request.getParameter("ANNAEXEC");

			// Todas as demais varáveis recebidas no POST, com exceção da variável ANNAEXEC
			// que contém o "IV Recebido" serão enviadas com seu conteédo encriptado
			// Leitura da variável 'AnotherVar' enviada no POST encriptada
			String anotherVar = request.getParameter("AnotherVar");

			// O iv e as chaves são enviadas com encode em Base64, então é necessário
			// realizar o decode para obter os bytes
			byte[] ivDecoded = Base64.getDecoder().decode(ivRecebido);
			byte[] decKeyDecoded = Base64.getDecoder().decode(decKey);
			byte[] encKeyDecoded = Base64.getDecoder().decode(encKey);

			// Após o decode é feito a associação aos tipos requeridos pelo algoritmo
			// (IvParameterSpec para o IV, SecretKey para as chaves)
			IvParameterSpec iv = new IvParameterSpec(ivDecoded);
			SecretKey dKey = new SecretKeySpec(decKeyDecoded, "DESede");
			SecretKey eKey = new SecretKeySpec(encKeyDecoded, "DESede");

			// Desencriptação do conteúdo de 'AnotherVar'
			String decrypted = decrypt(anotherVar, dKey, iv);

			// ... Código do Web Service.
			// Neste exemplo retornaremos um container tipo MESSAGE com o valor
			// desencriptado da variável enviada no POST 'AnotherVar' configurada no fluxo
			// Para conhecer outros tipos de containers consulte a documentação
			String finalResponse = "[{";
			finalResponse += "\"PropName\":\"Container001\",";
			finalResponse += "\"PropValue\":";
			finalResponse += "[";
			finalResponse += "{\"";
			finalResponse += "PropName\":\"Type\",";
			finalResponse += "\"PropValue\":\"MESSAGE\"";
			finalResponse += "},";

			// anotherVar
			finalResponse += "{";
			finalResponse += "\"PropName\":\"Phrase\",";
			finalResponse += "\"PropValue\":\"" + decrypted + "\"";
			finalResponse += "},";

			finalResponse += "]";
			finalResponse += "}]";
			
			
			// ... Fim do código do Web Service.
			// Encriptação do JSON de resposta ao AnnA
			// Gerando um novo IV
			byte[] randomBytes = new byte[8];
			new Random().nextBytes(randomBytes);
			final IvParameterSpec novoIv = new IvParameterSpec(randomBytes);
			String novoIvEncoded = new String(Base64.getEncoder().encode(randomBytes));

			// Encriptação do "IV Novo" com a chave de desencriptação e o "IV Recebido"
			finalResponse = encrypt(finalResponse, eKey, novoIv);
			String novoIvEncriptado = encrypt(novoIvEncoded, dKey, iv);

			// Concatenação do JSON de resposta encriptado, o "IV Recebido" e o "IV Novo
			// Encriptado"
			finalResponse = finalResponse + ivRecebido + novoIvEncriptado;

			// Envio das informações ao AnnA
			PrintWriter out = response.getWriter();
			out.print("O valor de anotherVar é: " + finalResponse);

		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| InvalidAlgorithmParameterException | UnsupportedEncodingException | IllegalBlockSizeException
				| BadPaddingException ex) {
			Logger.getLogger(DESExample.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	  // Funções de encrypt e decrypt
    public String encrypt(String message, SecretKey key, IvParameterSpec iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {    
        final Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);        
        byte[] plainTextBytes = message.getBytes("utf-8");
        byte[] buf = cipher.doFinal(plainTextBytes);
        byte[] base64Bytes = Base64.getEncoder().encode(buf);
        String base64EncryptedString = new String(base64Bytes);        
        return base64EncryptedString;       
    }
     public String decrypt(String encMessage, SecretKey key, IvParameterSpec iv) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        byte[] message = Base64.getDecoder().decode(encMessage.getBytes("utf-8"));        
        final Cipher decipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        decipher.init(Cipher.DECRYPT_MODE, key, iv);        
        byte[] plainText = decipher.doFinal(message);        
        return new String(plainText, "UTF-8");
    }

}
