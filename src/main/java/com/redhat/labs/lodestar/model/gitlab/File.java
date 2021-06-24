package com.redhat.labs.lodestar.model.gitlab;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.json.bind.annotation.JsonbProperty;
import javax.ws.rs.WebApplicationException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class File {

	@JsonbProperty("file_path")
	private String filePath;
	@JsonbProperty("branch")
	private String branch;
	@Builder.Default
	@JsonbProperty("encoding")
	private String encoding = "base64";
	@JsonbProperty("author_email")
	private String authorEmail;
	@JsonbProperty("author_name")
	private String authorName;
	@JsonbProperty("content")
	private String content;
	@JsonbProperty("commit_message")
	private String commitMessage;

	public void encodeFileAttributes() {

		// encode file path
		if (null != filePath) {
			String encodedFilePath;
			try {
				encodedFilePath = urlEncode(this.filePath);
			} catch (UnsupportedEncodingException e) {
				throw new WebApplicationException("failed to encode url" + filePath, 500);
			}
			this.filePath = encodedFilePath;
		}

		// encode contents
		if (null != content) {
			byte[] encodedContents = base64Encode(this.content.getBytes());
			this.content = new String(encodedContents, StandardCharsets.UTF_8);
		}

	}

	public void decodeFileAttributes() {

		// decode file path
		if (null != filePath) {
			String decodedFilePath;
			try {
				decodedFilePath = urlDecode(this.filePath);
			} catch (UnsupportedEncodingException e) {
				throw new WebApplicationException("failed to decode url" + filePath, 500);
			}
			this.filePath = decodedFilePath;
		}

		// decode contents
		if (null != content) {
			byte[] decodedContents = base64Decode(this.content);
			this.content = new String(decodedContents, StandardCharsets.UTF_8);

		}

	}

	byte[] base64Encode(byte[] src) {
		return Base64.getEncoder().encode(src);
	}

	byte[] base64Decode(String src) {
		return Base64.getDecoder().decode(src);
	}

	String urlEncode(String src) throws UnsupportedEncodingException {
		return URLEncoder.encode(src, StandardCharsets.UTF_8.toString());
	}

	String urlDecode(String src) throws UnsupportedEncodingException {
		return URLDecoder.decode(src, StandardCharsets.UTF_8.toString());
	}

}
